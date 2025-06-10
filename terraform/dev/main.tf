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
# NIVEL 1: Despliegue del Servidor de Configuración (Cloud Config)
# ==============================================================================
module "cloud-config" {
  source = "../modules/microservice"
  
  # Asegura que el namespace y zipkin se creen primero
  depends_on = [kubernetes_deployment.zipkin]

  name           = "cloud-config"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:config"
  spring_profile = var.spring_profile
  container_port = 9296 # Puerto específico de cloud-config
}

# ==============================================================================
# NIVEL 2: Despliegue Secuencial de Aplicaciones 1x1
# ==============================================================================

# 2.1: User Service (Entidad fundamental)
module "user-service" {
  source = "../modules/microservice"
  depends_on = [module.service-discovery] # Depende de la infra

  name           = "user-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:users"
  spring_profile = var.spring_profile
}

# 2.2: Product Service (Entidad de catálogo)
module "product-service" {
  source = "../modules/microservice"
  depends_on = [module.user-service] # Espera a que user-service esté definido

  name           = "product-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:product"
  spring_profile = var.spring_profile
}

# 2.3: Order Service (Lógica de negocio principal)
module "order-service" {
  source = "../modules/microservice"
  depends_on = [module.product-service] # Espera a product-service

  name           = "order-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:order"
  spring_profile = var.spring_profile
}

# 2.4: Payment Service (Soporte a la orden)
module "payment-service" {
  source = "../modules/microservice"
  depends_on = [module.order-service] # Espera a order-service

  name           = "payment-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:payment"
  spring_profile = var.spring_profile
}

# 2.5: Shipping Service (Soporte a la orden)
module "shipping-service" {
  source = "../modules/microservice"
  depends_on = [module.payment-service] # Espera a payment-service

  name           = "shipping-service"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:shipping"
  spring_profile = var.spring_profile
}

# 2.6: API Gateway (Punto de entrada final)
module "api-gateway" {
  source = "../modules/microservice"
  # Espera a que el último servicio de la cadena esté definido
  depends_on = [module.shipping-service]

  name           = "api-gateway"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:gateway"
  spring_profile = var.spring_profile
}