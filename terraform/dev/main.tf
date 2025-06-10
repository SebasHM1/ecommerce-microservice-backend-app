# terraform/dev/main.tf

# Define el proveedor que usará Terraform. 
provider "kubernetes" {}

# ==============================================================================
# Patrón de Creación Condicional para el Namespace (Se mantiene igual)
# ==============================================================================
data "kubernetes_namespace" "existing" {
  metadata {
    name = "dev"
  }
}
resource "kubernetes_namespace" "created" {
  count = data.kubernetes_namespace.existing.id == null ? 1 : 0
  metadata {
    name = "dev"
  }
}

# ==============================================================================
# NIVEL 0: Despliegue de Servicios sin Dependencias (Zipkin)
# ==============================================================================
resource "kubernetes_deployment" "zipkin" {
  depends_on = [kubernetes_namespace.created]
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
# NIVEL 1: Infraestructura Central
# ==============================================================================
module "cloud-config" {
  source = "../modules/microservice"
  depends_on = [resource.kubernetes_deployment.zipkin]

  name           = "cloud-config"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:config"
  spring_profile = var.spring_profile
  container_port = 9296

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

module "service-discovery" {
  source = "../modules/microservice"
  depends_on = [resource.kubernetes_deployment.zipkin]

  name           = "service-discovery"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:discovery"
  spring_profile = var.spring_profile
  container_port = 8761

  env_vars = {
    "SPRING_ZIPKIN_BASE_URL" = "http://zipkin.dev.svc.cluster.local:9411/"
  }
}

# ==============================================================================
# NIVEL 2: Servicios de Aplicación
# ==============================================================================
# Un mapa local para definir las variables de entorno comunes
locals {
  common_app_env_vars = {
    "EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE" = "http://service-discovery.dev.svc.cluster.local:8761/eureka"
    "SPRING_CONFIG_IMPORT"                  = "optional:configserver:http://cloud-config.dev.svc.cluster.local:9296"
    "SPRING_ZIPKIN_BASE_URL"                = "http://zipkin.dev.svc.cluster.local:9411/"
  }
}

module "user-service" {
  source = "../modules/microservice"
  depends_on = [module.cloud-config, module.service-discovery]

  name           = "user-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:users"
  spring_profile = var.spring_profile
  container_port = 8700
  health_check_path = "/user-service/actuator/health"

  env_vars = merge(
    local.common_app_env_vars,
    { "EUREKA_INSTANCE" = "user-service" }
  )
}

# --- Y ahora replica este patrón para cada uno de tus otros servicios ---

module "product-service" {
  source = "../modules/microservice"
  depends_on = [module.user-service] # O las dependencias que correspondan

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
}

# 3.4: Payment Service (Soporte a la orden)
module "payment-service" {
  source = "../modules/microservice"
  depends_on = [module.order-service] # Espera a order-service

  name           = "payment-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:payment"
  spring_profile = var.spring_profile
}

# 3.5: Shipping Service (Soporte a la orden)
module "shipping-service" {
  source = "../modules/microservice"
  depends_on = [module.payment-service] # Espera a payment-service

  name           = "shipping-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:shipping"
  spring_profile = var.spring_profile
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
}