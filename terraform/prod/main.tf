# terraform/prod/main.tf

# Define el proveedor que usará Terraform. 
# El bloque vacío significa que tomará la configuración del entorno (el ServiceAccount del pod de Jenkins).
provider "kubernetes" {}

# ==============================================================================
# Patrón de Creación Condicional para el Namespace
# ==============================================================================

# 1. Intenta LEER el namespace usando una data source.
#    Si el namespace no existe, este data source quedará "vacío" o sin resolver,
#    pero no detendrá el plan gracias al paso siguiente.
data "kubernetes_namespace" "existing" {
  metadata {
    name = "prod"
  }
}

# 2. DEFINE el recurso del namespace, pero con una condición de creación.
#    Este es el núcleo de la lógica.
resource "kubernetes_namespace" "created" {
  # count será 1 (créalo) si el ID del data source es nulo (no se encontró).
  # count será 0 (no hagas nada) si el ID tiene un valor (se encontró).
  count = data.kubernetes_namespace.existing.id == null ? 1 : 0

  metadata {
    name = "prod"
  }
}

# ==============================================================================
# El resto de tu código ahora usa el nombre del namespace directamente,
# ya que está garantizado que existirá después del bloque anterior.
# ==============================================================================

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
  
  # Añadimos una dependencia explícita para asegurar que el namespace se evalúe primero.
  # Esto asegura que Terraform resuelva la data source y el recurso condicional antes de continuar.
  depends_on = [
    data.kubernetes_namespace.existing,
    kubernetes_namespace.created
  ]

  # Ruta relativa al módulo que creamos en la Fase 1
  source = "../modules/microservice"

  # Asignación de variables para el módulo
  name           = each.key # El nombre del servicio, ej. "api-gateway"
  namespace      = "prod" # Podemos usar el valor hardcodeado porque está garantizado que existe
  spring_profile = var.spring_profile

  # Construcción dinámica de la URL de la imagen completa
  image = "${var.dockerhub_user}/${var.repo_prefix}:${each.value}"

  # Se pueden sobrescribir valores por defecto para servicios específicos.
  # Por ejemplo, si 'user-service' necesitara 2 réplicas en prod:
  # replicas = each.key == "user-service" ? 2 : 1
}

# Manejo de casos especiales que no encajan en el módulo, como Zipkin.
resource "kubernetes_deployment" "zipkin" {
  # Aseguramos que se despliegue después de que el namespace se evalúe.
  depends_on = [
    data.kubernetes_namespace.existing,
    kubernetes_namespace.created
  ]

  metadata {
    name      = "zipkin"
    namespace = "prod" # Usamos el valor directamente
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
  # El deployment de zipkin ya depende del namespace, así que esta dependencia es implícita,
  # pero ser explícito no hace daño.
  depends_on = [kubernetes_deployment.zipkin]

  metadata {
    name      = "zipkin"
    namespace = "prod" # Usamos el valor directamente
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