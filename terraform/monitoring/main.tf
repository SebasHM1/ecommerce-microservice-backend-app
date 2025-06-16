# terraform/monitoring/main.tf

# 1. Crear un namespace dedicado para las herramientas de monitoreo
resource "kubernetes_namespace" "monitoring" {
  metadata {
    name = "monitoring"
  }
}

# 2. Desplegar Prometheus usando el chart de Helm oficial
resource "helm_release" "prometheus" {
  name       = "prometheus"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "prometheus"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name
  version    = "15.10.0" # Usar una versión específica para consistencia

  # IMPORTANTE: Configuración para que funcione simple en Minikube
  values = [
    yamlencode({
      # Deshabilitar componentes que no necesitamos para una demo simple
      alertmanager = {
        enabled = false
      }
      pushgateway = {
        enabled = false
      }
      # Configuración del servidor Prometheus
      server = {
        # ¡NO usar almacenamiento persistente! Ideal para Minikube.
        # Los datos se perderán si el pod se reinicia.
        persistentVolume = {
          enabled = false
        }
        # AÑADIMOS LA MAGIA: Service Discovery de Kubernetes
        # Prometheus buscará automáticamente cualquier pod en CUALQUIER namespace
        # que tenga la anotación 'prometheus.io/scrape: "true"'.
        extraScrapeConfigs = <<-EOT
        - job_name: 'kubernetes-pods'
          kubernetes_sd_configs:
          - role: pod
          relabel_configs:
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
            action: keep
            regex: true
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
            action: replace
            target_label: __metrics_path__
            regex: (.+)
          - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
            action: replace
            regex: ([^:]+)(?::\d+)?;(\d+)
            replacement: $1:$2
            target_label: __address__
        EOT
      }
    })
  ]
}

# 3. Desplegar Grafana usando el chart de Helm oficial
resource "helm_release" "grafana" {
  name       = "grafana"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "grafana"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name
  version    = "6.29.3" # Usar una versión específica

  values = [
    yamlencode({
      # ¡NO usar almacenamiento persistente!
      persistence = {
        enabled = false
      }
      # Establecer la contraseña de admin (la default es aleatoria)
      # Usuario: admin, Contraseña: admin-password
      adminPassword = "admin-password"

      # ======================================================================
      # INICIO DE LA ACTUALIZACIÓN
      # Corrección para el error "no matches for kind PodSecurityPolicy".
      # Las versiones modernas de Kubernetes ya no usan PSPs, por lo que
      # debemos decirle al chart de Helm que no intente crear una.
      # ======================================================================
      podSecurityPolicy = {
        enabled = false
      }
      # ======================================================================
      # FIN DE LA ACTUALIZACIÓN
      # ======================================================================

      # AÑADIMOS LA MAGIA: Pre-configurar el Datasource de Prometheus
      datasources = {
        "datasources.yaml" = {
          apiVersion = 1
          datasources = [
            {
              name = "Prometheus-Local"
              type = "prometheus"
              # URL interna del servicio de Prometheus que desplegamos antes
              url = "http://prometheus-server.${kubernetes_namespace.monitoring.metadata[0].name}.svc.cluster.local"
              access = "proxy"
              isDefault = true
            }
          ]
        }
      }

      # AÑADIMOS MÁS MAGIA: Cargar un dashboard para Spring Boot automáticamente
      # Habilitamos el sidecar que busca dashboards en ConfigMaps
      sidecar = {
        dashboards = {
          enabled = true
          label = "grafana_dashboard" # Buscará ConfigMaps con esta etiqueta
        }
      }
    })
  ]
}

# 4. Crear el ConfigMap con el JSON del Dashboard de Spring Boot
resource "kubernetes_config_map" "spring_boot_dashboard" {
  metadata {
    name      = "spring-boot-dashboard"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
    labels = {
      # Esta etiqueta debe coincidir con la que busca el sidecar de Grafana
      grafana_dashboard = "1"
    }
  }

  # El contenido del dashboard va aquí.
  # Usaremos un archivo externo para mantenerlo limpio.
  data = {
    "spring-boot-dashboard.json" = file("${path.module}/dashboard-spring-boot.json")
  }
}