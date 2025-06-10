# terraform/dev/main.tf

# Define el proveedor que usará Terraform. 
# El bloque vacío significa que tomará la configuración del entorno (el ServiceAccount del pod de Jenkins).
provider "kubernetes" {}

# Crea el namespace "dev" si no existe. Gracias a tu RBAC, Jenkins puede hacerlo.
resource "kubernetes_namespace" "env_ns" {
  metadata {
    name = "stage"
  }
}

# Un mapa local para no repetirnos. Mapea el nombre del servicio al nombre de su imagen.
locals {
  services = {
    "service-discovery" = "discovery",
    "cloud-config"      = "config",
    "api-gateway"       = "gateway",
    "user-service"      = "users",
    "product-service"   = "product",
    "order-service"     = "order",
    "shipping-service"  = "shipping",
    "payment-service"   = "payment",
    # Añade aquí tus otros servicios como proxy-client, favourite-service, etc.
  }
}

# Bucle "for_each" que itera sobre el mapa `locals.services`
# y crea una instancia de nuestro módulo `microservice` para cada uno.
module "microservice" {
  for_each = local.services
  
  # Ruta relativa al módulo que creamos en la Fase 1
  source = "../modules/microservice"

  # Asignación de variables para el módulo
  name           = each.key # El nombre del servicio, ej. "api-gateway"
  namespace      = kubernetes_namespace.env_ns.metadata[0].name
  spring_profile = var.spring_profile

  # Construcción dinámica de la URL de la imagen completa
  image = "${var.dockerhub_user}/${var.repo_prefix}:${each.value}${var.image_tag_suffix}"

  # Se pueden sobrescribir valores por defecto para servicios específicos.
  # Por ejemplo, si 'user-service' necesitara 2 réplicas en dev:
  # replicas = each.key == "user-service" ? 2 : 1
}

# Manejo de casos especiales que no encajan en el módulo, como Zipkin.
# Lo puedes convertir a HCL desde tu YAML. Herramientas online como "YAML to HCL" ayudan.
resource "kubernetes_deployment" "zipkin" {
  # Aseguramos que se despliegue después de que el namespace esté creado
  depends_on = [kubernetes_namespace.env_ns]

  metadata {
    name      = "zipkin"
    namespace = "stage"
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
  depends_on = [kubernetes_namespace.env_ns]

  metadata {
    name      = "zipkin"
    namespace = "stage"
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