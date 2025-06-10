# terraform/modules/microservice/main.tf

# Recurso que define el Deployment en Kubernetes
resource "kubernetes_deployment" "app" {
  metadata {
    name      = var.name
    namespace = var.namespace
    labels    = merge({ app = var.name }, var.labels)
  }

  spec {
    replicas = var.replicas

    selector {
      match_labels = {
        app = var.name
      }
    }

    template {
      metadata {
        labels = merge({ app = var.name }, var.labels)
      }

      spec {
        container {
          image = var.image
          name  = var.name
          
          port {
            container_port = var.container_port
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = var.spring_profile
          }

          # Bucle que crea dinámicamente los bloques 'env' a partir del mapa
          dynamic "env" {
            for_each = var.env_vars
            content {
              name  = env.key
              value = env.value
            }
          }

          # BUENA PRÁCTICA: Añadir sondas de salud
          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = var.container_port
            }
            initial_delay_seconds = 60 # Dar tiempo a que Spring arranque
            period_seconds        = 15
            timeout_seconds       = 5
          }

          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = var.container_port
            }
            initial_delay_seconds = 30
            period_seconds        = 10
            timeout_seconds       = 5
          }
        }
      }
    }
  }
}

# Recurso que define el Service de tipo ClusterIP
resource "kubernetes_service" "app" {
  metadata {
    name      = var.name
    namespace = var.namespace
    labels    = merge({ app = var.name }, var.labels)
  }
  
  spec {
    selector = {
      app = var.name
    }
    
    port {
      port        = 8080 # El puerto del servicio
      target_port = var.container_port # El puerto del contenedor
    }
    
    type = "ClusterIP"
  }
}