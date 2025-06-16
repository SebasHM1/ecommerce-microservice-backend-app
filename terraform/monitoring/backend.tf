# terraform/monitoring/backend.tf

terraform {
  # Le decimos a Terraform que use Kubernetes para guardar su estado.
  backend "kubernetes" {
    # El nombre del Secreto de Kubernetes donde se guardará el estado.
    secret_suffix = "monitoring-state"
    
    # El namespace donde se creará este Secreto. 
    # Usaremos 'default' para simplicidad, pero podría ser 'jenkins'.
    namespace = "default"
    
    # IMPORTANTE: Le dice a Terraform que se está ejecutando dentro de un pod
    # y que debe usar las credenciales inyectadas, no un archivo kubeconfig.
    in_cluster = true 
  }
}