# terraform/modules/microservice/outputs.tf

output "deployment_name" {
  description = "El nombre del Deployment creado."
  value       = kubernetes_deployment.app.metadata[0].name
}

output "service_name" {
  description = "El nombre del Service creado."
  value       = kubernetes_service.app.metadata[0].name
}

output "service_cluster_ip" {
  description = "La IP interna del Service en el clúster."
  value       = kubernetes_service.app.spec[0].cluster_ip
}