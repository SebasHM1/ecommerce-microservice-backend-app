# terraform/dev/main.tf

# Define el proveedor que usará Terraform. 
# La configuración se toma del contexto activo de KUBECONFIG.
provider "kubernetes" {}

# ==============================================================================
# GESTIÓN DEL NAMESPACE: Dinámico y Reutilizable
# ==============================================================================
# Ahora el nombre del namespace se toma de una variable.
# Esto permite que este mismo main.tf funcione para 'dev', 'stage' y 'prod'.
resource "kubernetes_namespace" "env_namespace" {
  metadata {
    name = var.k8s_namespace
  }
}

# ... (provider y namespace) ...

# ==============================================================================
# DESPLIEGUE DE LA BASE DE DATOS PARA ESTE ENTORNO
# ==============================================================================
resource "kubernetes_deployment" "mysql" {
  depends_on = [kubernetes_secret.mysql_secrets]
  metadata {
    name      = "mysql"
    namespace = var.k8s_namespace
    labels    = { app = "mysql" }
  }
  spec {
    replicas = 1
    selector { match_labels = { app = "mysql" } }
    template {
      metadata { labels = { app = "mysql" } }
      spec {
        container {
          name  = "mysql"
          image = "mysql:8.0"
          port { container_port = 3306 }
          
          # ==========================================================
          # SINTAXIS CORREGIDA
          # ==========================================================
          env {
            name  = "MYSQL_ROOT_PASSWORD"
            value_from {
              // CORRECCIÓN: 'secret_key_ref' está en su propia línea
              secret_key_ref {
                name = kubernetes_secret.mysql_secrets.metadata[0].name
                key  = "mysql-root-password"
              }
            }
          }
          env {
            name = "MYSQL_DATABASE"
            value_from {
               // CORRECCIÓN: 'secret_key_ref' está en su propia línea
               secret_key_ref {
                name = kubernetes_secret.mysql_secrets.metadata[0].name
                key = "mysql-database"
               }
            }
          }
        }
      }
    }
  }
}


# ==============================================================================
# NIVEL 0: Despliegue de Servicios sin Dependencias (Zipkin)
# ==============================================================================

resource "kubernetes_deployment" "zipkin" {
  depends_on = [kubernetes_service.mysql]
  
  metadata { /* ... */ }
  spec {
    replicas = 1
    selector { /* ... */ }
    template {
      metadata { /* ... */ }
      spec {
        container {
          name  = "zipkin"
          image = var.service_images["zipkin"]
          port { container_port = 9411 }

          env {
            name  = "STORAGE_TYPE"
            value = "mysql"
          }
          env {
            name  = "MYSQL_JDBC_URL"
            // Usando la base de datos principal para simplificar
            value = "jdbc:mysql://mysql:3306/${kubernetes_secret.mysql_secrets.data["mysql-database"]}?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true"
          }
          env {
            name  = "MYSQL_USER"
            value = "root"
          }
          env {
            name  = "MYSQL_PASS"
            value_from {
              // CORRECCIÓN: 'secret_key_ref' está en su propia línea
              secret_key_ref {
                name = kubernetes_secret.mysql_secrets.metadata[0].name
                key  = "mysql-root-password"
              }
            }
          }
        }
      }
    }
  }
}
resource "kubernetes_service" "zipkin" {
  depends_on = [kubernetes_deployment.zipkin]
  metadata {
    name      = "zipkin"
    namespace = var.k8s_namespace # Usar la variable de namespace
  }
  spec {
    selector = { app = "zipkin" }
    port {
      port        = 9411
      target_port = 9411
    }
    type = "ClusterIP"
  }
}

# ==============================================================================
# NIVEL 1: Despliegue del Servidor de Configuración (Cloud Config)
# ==============================================================================
module "cloud-config" {
  source = "../modules/microservice"
  depends_on = [kubernetes_deployment.zipkin]

  name           = "cloud-config"
  namespace      = var.k8s_namespace # Usar la variable de namespace
  # CAMBIO CLAVE: Usamos el mapa 'service_images' para obtener la URL completa de la imagen.
  image          = var.service_images["config"] 
  spring_profile = var.spring_profile
  container_port = 9296

  init_containers_config = [
    {
      name    = "wait-for-service-discovery"
      # URL dinámica usando la variable de namespace
      image   = "curlimages/curl:7.85.0"
      command = ["sh", "-c", "until curl -s -f http://service-discovery.${var.k8s_namespace}.svc.cluster.local:8761/actuator/health; do sleep 5; done"]
    }
  ]
  env_vars = {
    # URL dinámica usando la variable de namespace
    "SPRING_ZIPKIN_BASE_URL" = "http://zipkin.${var.k8s_namespace}.svc.cluster.local:9411/"
    "EUREKA_INSTANCE"        = "cloud-config"
  }
}

# ==============================================================================
# NIVEL 2: Despliegue del Descubrimiento de Servicios (Eureka)
# ==============================================================================
module "service-discovery" {
  source = "../modules/microservice"
  depends_on = [kubernetes_deployment.zipkin]

  name           = "service-discovery"
  namespace      = var.k8s_namespace # Usar la variable de namespace
  # CAMBIO CLAVE: Usamos el mapa 'service_images'
  image          = var.service_images["discovery"]
  spring_profile = var.spring_profile
  container_port = 8761
}

# ==============================================================================
# NIVEL 3: Despliegue Secuencial de Aplicaciones
# ==============================================================================

locals {
  # 'locals' ahora construye las URLs dinámicamente usando la variable del namespace.
  # Esto hace que todo el bloque sea reutilizable para cualquier entorno.
  common_app_env_vars = {
    "EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE" = "http://service-discovery.${var.k8s_namespace}.svc.cluster.local:8761/eureka"
    "SPRING_CONFIG_IMPORT"                  = "optional:configserver:http://cloud-config.${var.k8s_namespace}.svc.cluster.local:9296"
    "SPRING_ZIPKIN_BASE_URL"                = "http://zipkin.${var.k8s_namespace}.svc.cluster.local:9411/"
  }

  # 'locals' para los init containers para no repetir el mismo bloque 10 veces (DRY: Don't Repeat Yourself)
  init_containers_wait_for_infra = [
    {
      name    = "wait-for-cloud-config"
      image   = "curlimages/curl:7.85.0"
      command = ["sh", "-c", "echo Waiting for Cloud Config...; until curl -s -f http://cloud-config.${var.k8s_namespace}.svc.cluster.local:9296/actuator/health; do echo -n .; sleep 5; done; echo; echo Cloud Config is UP"]
    },
    {
      name    = "wait-for-service-discovery"
      image   = "curlimages/curl:7.85.0"
      command = ["sh", "-c", "echo Waiting for Service Discovery...; until curl -s -f http://service-discovery.${var.k8s_namespace}.svc.cluster.local:8761/actuator/health; do echo -n .; sleep 5; done; echo; echo Service Discovery is UP"]
    }
  ]
}

# 3.1: User Service
module "user-service" {
  source = "../modules/microservice"
  depends_on = [module.cloud-config]

  name           = "user-service"
  namespace      = var.k8s_namespace
  # CAMBIO CLAVE: Usamos el mapa 'service_images'
  image          = var.service_images["users"]
  spring_profile = var.spring_profile
  container_port = 8700
  health_check_path = "/user-service/actuator/health"
  init_containers_config = local.init_containers_wait_for_infra # MEJORA: Reutilizar el local
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "user-service" }
  )
}

# 3.2: Product Service
module "product-service" {
  source = "../modules/microservice"
  depends_on = [module.user-service]

  name           = "product-service"
  namespace      = var.k8s_namespace
  # CAMBIO CLAVE: Usamos el mapa 'service_images'
  image          = var.service_images["product"]
  spring_profile = var.spring_profile
  container_port = 8500
  health_check_path = "/product-service/actuator/health"
  init_containers_config = local.init_containers_wait_for_infra # MEJORA: Reutilizar el local
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "product-service" }
  )
}

# 3.3: Order Service
module "order-service" {
  source = "../modules/microservice"
  depends_on = [module.product-service]

  name           = "order-service"
  namespace      = var.k8s_namespace
  # CAMBIO CLAVE: Usamos el mapa 'service_images'
  image          = var.service_images["order"]
  spring_profile = var.spring_profile
  container_port   = 8300
  health_check_path = "/order-service/actuator/health"
  init_containers_config = local.init_containers_wait_for_infra # MEJORA: Reutilizar el local
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "order-service" }
  )
}

# 3.4: Payment Service
module "payment-service" {
  source = "../modules/microservice"
  depends_on = [module.order-service]

  name           = "payment-service"
  namespace      = var.k8s_namespace
  # CAMBIO CLAVE: Usamos el mapa 'service_images'
  image          = var.service_images["payment"]
  spring_profile = var.spring_profile
  container_port   = 8400
  health_check_path = "/payment-service/actuator/health"
  init_containers_config = local.init_containers_wait_for_infra # MEJORA: Reutilizar el local
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "payment-service" }
  )
}

# 3.5: Shipping Service
module "shipping-service" {
  source = "../modules/microservice"
  depends_on = [module.payment-service]

  name           = "shipping-service"
  namespace      = var.k8s_namespace
  # CAMBIO CLAVE: Usamos el mapa 'service_images'
  image          = var.service_images["shipping"]
  spring_profile = var.spring_profile
  container_port   = 8600
  health_check_type = "command"
  init_containers_config = local.init_containers_wait_for_infra # MEJORA: Reutilizar el local
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "shipping-service" }
  )
}

# 3.6: API Gateway
module "api-gateway" {
  source = "../modules/microservice"
  depends_on = [module.shipping-service]

  name           = "api-gateway"
  namespace      = var.k8s_namespace
  # CAMBIO CLAVE: Usamos el mapa 'service_images'
  image          = var.service_images["gateway"]
  spring_profile = var.spring_profile
  container_port = 8080
  health_check_type = "command"
  init_containers_config = local.init_containers_wait_for_infra # MEJORA: Reutilizar el local
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "api-gateway" }
  )
}