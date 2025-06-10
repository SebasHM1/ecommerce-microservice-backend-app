# terraform/dev/backend.tf

terraform {
  backend "kubernetes" {
    # El sufijo del Secret donde se guardará el estado. Será "tfstate-dev"
    secret_suffix    = "state-stage" 
    
    # El namespace donde se creará el Secret del estado.
    # Es crucial que este namespace exista. Jenkins tiene permisos para crearlo.
    namespace        = "stage" 
    
    # Dentro del pod de Jenkins, no se necesita un path explícito,
    # Terraform usará automáticamente el ServiceAccount del pod.
    # config_path      = "~/.kube/config" # No es necesario en el pod
  }
}