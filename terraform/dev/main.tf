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
# NIVEL 2: Despliegue del Descubrimiento de Servicios (Eureka)
# ==============================================================================
module "service-discovery" {
  source = "../modules/microservice"

  # ¡DEPENDENCIA CLAVE! Se crea solo después de que cloud-config esté definido.
  depends_on = [module.cloud-config]

  name           = "service-discovery"
  namespace      = "dev"
  image          = "${var.dockerhub_user}/${var.repo_prefix}:discovery"
  spring_profile = var.spring_profile
  container_port = 8761 # Puerto específico de service-discovery
}

# ==============================================================================
# NIVEL 3: Despliegue del Resto de Microservicios de la Aplicación
# ==============================================================================

# Mapa local para el resto de los servicios (excluyendo los de infraestructura)
locals {
  app_services = {
    "api-gateway"       = "gateway",
    "user-service"      = "users",
    "product-service"   = "product",
    "order-service"     = "order",
    "shipping-service"  = "shipping",
    "payment-service"   = "payment",
  }
}

# Bucle for_each para desplegar todos los servicios de la aplicación
module "application_microservices" {
  for_each = local.app_services
  
  # ¡DEPENDENCIA CLAVE! Todos estos servicios dependen de que el Service Discovery
  # y el Config Server hayan sido definidos.
  depends_on = [module.service-discovery]

  source = "../modules/microservice"

  # Asignación de variables para el módulo
  name           = each.key
  namespace      = "dev"
  spring_profile = var.spring_profile
  image          = "${var.dockerhub_user}/${var.repo_prefix}:${each.value}"
}