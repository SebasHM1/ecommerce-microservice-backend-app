# terraform/modules/microservice/main.tf

resource "kubernetes_deployment" "app" {
  metadata {
    name      = var.name
    namespace = var.namespace
    labels    = { app = var.name }
  }

  spec {
    replicas = var.replicas
    selector { match_labels = { app = var.name } }
    template {
      metadata { labels = { app = var.name } }
      spec {
        # --- INIT CONTAINERS DINÁMICOS ---
        dynamic "init_container" {
          for_each = var.init_containers_config
          content {
            name    = init_container.value.name
            image   = init_container.value.image
            command = init_container.value.command
          }
        }

        container {
          image = var.image
          name  = var.name
          port { container_port = var.container_port }

          # --- VARIABLES DE ENTORNO DINÁMICAS ---
          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = var.spring_profile
          }
          dynamic "env" {
            for_each = var.env_vars
            content {
              name  = env.key
              value = env.value
            }
          }

          # --- SONDAS DE SALUD DINÁMICAS ---
          liveness_probe {
            http_get {
              path = "${var.health_check_path}/liveness"
              port = var.container_port
            }
            initial_delay_seconds = 180 # Aumentamos el delay inicial, es más seguro
            period_seconds        = 15
            timeout_seconds       = 5
          }
          readiness_probe {
            http_get {
              path = var.health_check_path
              port = var.container_port
            }
            initial_delay_seconds = 60 # Aumentamos el delay inicial
            period_seconds        = 10
            timeout_seconds       = 5
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "app" {
  metadata {
    name      = var.name
    namespace = var.namespace
  }
  spec {
    selector = { app = var.name }
    port {
      port        = var.container_port
      target_port = var.container_port
    }
    type = "ClusterIP"
  }
}