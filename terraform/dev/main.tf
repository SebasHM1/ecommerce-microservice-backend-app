# terraform/dev/main.tf

# Define el proveedor que usará Terraform. 
provider "kubernetes" {}

# ==============================================================================
# GESTIÓN DEL NAMESPACE: Simple y Directa
# ==============================================================================
# Terraform es ahora el dueño de este recurso. Lo creará si no existe en el estado.
resource "kubernetes_namespace" "dev" {
  metadata {
    name = "dev"
  }
}

# ==============================================================================
# NIVEL 0: Despliegue de Servicios sin Dependencias (Zipkin)
# ==============================================================================
resource "kubernetes_deployment" "zipkin" {
  depends_on = [kubernetes_namespace.dev]
  metadata {
    name      = "zipkin"
    namespace = "dev"
    labels    = { app = "zipkin" }
  }
  spec {
    replicas = 1
    selector { match_labels = { app = "zipkin" } }
    template {
      metadata { labels = { app = "zipkin" } }
      spec {
        container {
          name  = "zipkin"
          image = "openzipkin/zipkin:latest"
          port { container_port = 9411 }
        }
      }
    }
  }
}
resource "kubernetes_service" "zipkin" {
  depends_on = [kubernetes_deployment.zipkin]
  metadata {
    name      = "zipkin"
    namespace = "dev"
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
  
  # Asegura que el namespace y zipkin se creen primero
  depends_on = [
    kubernetes_deployment.zipkin
    ]

  name           = "cloud-config"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:config"
  spring_profile = var.spring_profile
  container_port = 9296 # Puerto específico de cloud-config

  init_containers_config = [
    {
      name    = "wait-for-service-discovery"
      image   = "curlimages/curl:7.85.0"
      command = ["sh", "-c", "until curl -s -f http://service-discovery.dev.svc.cluster.local:8761/actuator/health; do sleep 5; done"]
    }
  ]

  env_vars = {
    "SPRING_ZIPKIN_BASE_URL" = "http://zipkin.dev.svc.cluster.local:9411/"
    "EUREKA_INSTANCE"        = "cloud-config"
  }

}

# ==============================================================================
# NIVEL 3: Despliegue del Descubrimiento de Servicios (Eureka)
# ==============================================================================
module "service-discovery" {
  source = "../modules/microservice"

  # ¡DEPENDENCIA CLAVE! Se crea solo después de que cloud-config esté definido.

  name           = "service-discovery"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:discovery"
  spring_profile = var.spring_profile
  container_port = 8761 # Puerto específico de service-discovery
}

# ==============================================================================
# NIVEL 3: Despliegue Secuencial de Aplicaciones 1x1
# ==============================================================================

locals {
  common_app_env_vars = {
    "EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE" = "http://service-discovery.dev.svc.cluster.local:8761/eureka"
    "SPRING_CONFIG_IMPORT"                  = "optional:configserver:http://cloud-config.dev.svc.cluster.local:9296"
    "SPRING_ZIPKIN_BASE_URL"                = "http://zipkin.dev.svc.cluster.local:9411/"
  }
}

# 3.1: User Service (Entidad fundamental)
module "user-service" {
  source = "../modules/microservice"
  depends_on = [module.service-discovery] # Depende de la infra

  name           = "user-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:users"
  spring_profile = var.spring_profile
  container_port = 8700
  health_check_path = "/user-service/actuator/health"
  init_containers_config = [
    {
      name    = "wait-for-cloud-config"
      image   = "curlimages/curl:7.85.0"
      command = ["sh", "-c", "echo Waiting for Cloud Config...; until curl -s -f http://cloud-config.dev.svc.cluster.local:9296/actuator/health; do echo -n .; sleep 5; done; echo; echo Cloud Config is UP"]
    },
    {
      name    = "wait-for-service-discovery"
      image   = "curlimages/curl:7.85.0"
      command = ["sh", "-c", "echo Waiting for Service Discovery...; until curl -s -f http://service-discovery.dev.svc.cluster.local:8761/actuator/health; do echo -n .; sleep 5; done; echo; echo Service Discovery is UP"]
    }
  ]
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "user-service" }
  )
}

# 3.2: Product Service (Entidad de catálogo)
module "product-service" {
  source = "../modules/microservice"
  depends_on = [module.user-service] # Espera a que user-service esté definido

  name           = "product-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:product"
  spring_profile = var.spring_profile

  container_port = 8500
  health_check_path = "/product-service/actuator/health"
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "product-service" }
  )

}

# 3.3: Order Service (Lógica de negocio principal)
module "order-service" {
  source = "../modules/microservice"
  depends_on = [module.product-service] # Espera a product-service

  name           = "order-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:order"
  spring_profile = var.spring_profile
  container_port   = 8300
  health_check_path = "/order-service/actuator/health"
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "order-service" }
  )
}

# 3.4: Payment Service (Soporte a la orden)
module "payment-service" {
  source = "../modules/microservice"
  depends_on = [module.order-service] # Espera a order-service

  name           = "payment-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:payment"
  spring_profile = var.spring_profile
  container_port   = 8400
  health_check_path = "/payment-service/actuator/health"
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "payment-service" }
  )
}

# 3.5: Shipping Service (Soporte a la orden)
module "shipping-service" {
  source = "../modules/microservice"
  depends_on = [module.payment-service] # Espera a payment-service

  name           = "shipping-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:shipping"
  spring_profile = var.spring_profile
  container_port   = 8600
  health_check_type = "command"

  init_containers_config = [
    {
      name    = "wait-for-cloud-config"
      image   = "curlimages/curl:7.85.0"
      command = ["sh", "-c", "echo Waiting for Cloud Config...; until curl -s -f http://cloud-config.dev.svc.cluster.local:9296/actuator/health; do echo -n .; sleep 5; done; echo; echo Cloud Config is UP"]
    },
    {
      name    = "wait-for-service-discovery"
      image   = "curlimages/curl:7.85.0"
      command = ["sh", "-c", "echo Waiting for Service Discovery...; until curl -s -f http://service-discovery.dev.svc.cluster.local:8761/actuator/health; do echo -n .; sleep 5; done; echo; echo Service Discovery is UP"]
    }
  ]
  
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "shipping-service" }
  )
}

# 3.6: API Gateway (Punto de entrada final)
module "api-gateway" {
  source = "../modules/microservice"
  # Espera a que el último servicio de la cadena esté definido
  depends_on = [module.shipping-service]

  name           = "api-gateway"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:gateway"
  spring_profile = var.spring_profile
  container_port = 8080
  health_check_type = "command"
  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "api-gateway" }
  )
  
}