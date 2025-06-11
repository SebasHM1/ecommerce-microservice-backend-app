# terraform/prod/backend.tf

terraform {
  backend "kubernetes" {
    # El sufijo sigue siendo específico para el entorno 'stage'
    secret_suffix    = "stage" # El Secret se llamará 'tfstate-stage'
    
    # ¡CAMBIO CLAVE! Guardamos el estado en el namespace 'jenkins'.
    # Este namespace es estable y no se borra con los despliegues.
    namespace        = "jenkins" 
  }
}