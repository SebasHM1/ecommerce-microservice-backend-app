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
  version    = "15.10.0"

  values = [
    yamlencode({
      # Deshabilitar componentes que no necesitamos para una demo simple
      alertmanager = {
        enabled = false
      }
      pushgateway = {
        enabled = false
      }

      # ======================================================================
      # INICIO DE LA ACTUALIZACIÓN
      # Deshabilitamos componentes extra para evitar errores de permisos
      # y mantener el despliegue lo más simple posible.
      # ======================================================================
      kubeStateMetrics = {
        enabled = false
      }
      nodeExporter = {
        enabled = false # <-- Esto evita el error de permisos para "daemonsets"
      }
      # ======================================================================
      # FIN DE LA ACTUALIZACIÓN
      # ======================================================================

      # Configuración del servidor Prometheus
      server = {
        # ¡NO usar almacenamiento persistente!
        persistentVolume = {
          enabled = false
        }
        # Service Discovery para encontrar tus microservicios
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
  version    = "6.29.3"

  values = [
    yamlencode({
      persistence = {
        enabled = false
      }
      adminPassword = "admin-password"

      # Corrección para el error "no matches for kind PodSecurityPolicy".
      podSecurityPolicy = {
        enabled = false
      }

      # Pre-configurar el Datasource de Prometheus
      datasources = {
        "datasources.yaml" = {
          apiVersion = 1
          datasources = [
            {
              name      = "Prometheus-Local"
              type      = "prometheus"
              url       = "http://prometheus-server.${kubernetes_namespace.monitoring.metadata[0].name}.svc.cluster.local"
              access    = "proxy"
              isDefault = true
            }
          ]
        }
      }

      # Cargar el dashboard de Spring Boot automáticamente
      sidecar = {
        dashboards = {
          enabled = true
          label   = "grafana_dashboard"
        }
      }
    })
  ]
}

# 4. Crear el ConfigMap con el JSON del Dashboard
resource "kubernetes_config_map" "spring_boot_dashboard" {
  metadata {
    name      = "spring-boot-dashboard"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
    labels = {
      grafana_dashboard = "1"
    }
  }

  data = {
    "spring-boot-dashboard.json" = file("${path.module}/dashboard-spring-boot.json")
  }
}