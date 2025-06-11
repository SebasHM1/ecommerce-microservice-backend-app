# terraform/dev/backend.tf

terraform {
  backend "kubernetes" {
    # El sufijo sigue siendo específico para el entorno 'dev'
    secret_suffix    = "tfstate-dev" # El Secret se llamará 'tfstate-dev'
    
    # ¡CAMBIO CLAVE! Guardamos el estado en el namespace 'jenkins'.
    # Este namespace es estable y no se borra con los despliegues.
    namespace        = "jenkins" 
  }
}