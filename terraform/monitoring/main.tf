# terraform/monitoring/main.tf (VERSIÓN FINAL)

resource "kubernetes_namespace" "monitoring" {
  metadata {
    name = "monitoring"
  }
}

resource "helm_release" "prometheus" {
  name       = "prometheus"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "prometheus"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name
  version    = "15.10.0"

  values = [
    yamlencode({
      alertmanager = {
        enabled = false
      }
      pushgateway = {
        enabled = false
      }
      kubeStateMetrics = {
        enabled = false
      }
      nodeExporter = {
        enabled = false
      }
      server = {
        persistentVolume = {
          enabled = false
        }
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

resource "helm_release" "grafana" {
  name       = "grafana"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "grafana"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  # ==============================================================
  # ACTUALIZACIÓN CRÍTICA: Usamos una versión más nueva del chart
  # que ya no intenta crear PodSecurityPolicies.
  # ==============================================================
  version    = "7.3.11"

  values = [
    yamlencode({
      persistence = {
        enabled = false
      }
      adminPassword = "admin-password"
      
      # Ya no necesitamos deshabilitar PSP porque este chart no lo usa.

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

      sidecar = {
        dashboards = {
          enabled = true
          label   = "grafana_dashboard"
        }
      }
    })
  ]
}

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