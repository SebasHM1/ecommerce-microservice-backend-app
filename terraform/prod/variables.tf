# terraform/dev/variables.tf

variable "service_images" {
  type = map(string)
  description = "Un mapa de nombres de servicio a la URL COMPLETA de la imagen Docker a desplegar. Jenkins sobreescribirá estos valores."
  default = {
    // Valores por defecto para pruebas locales de Terraform (terraform apply)
    // El pipeline de Jenkins proporcionará los valores correctos durante la ejecución.
    "discovery" = "sebashm1/ecommerce-microservice-backend-app:discovery-latest"
    "config"    = "sebashm1/ecommerce-microservice-backend-app:config-latest"
    "gateway"   = "sebashm1/ecommerce-microservice-backend-app:gateway-latest"
    "users"     = "sebashm1/ecommerce-microservice-backend-app:users-latest"
    "product"   = "sebashm1/ecommerce-microservice-backend-app:product-latest"
    "order"     = "sebashm1/ecommerce-microservice-backend-app:order-latest"
    "payment"   = "sebashm1/ecommerce-microservice-backend-app:payment-latest"
    "shipping"  = "sebashm1/ecommerce-microservice-backend-app:shipping-latest"
    "proxy"     = "sebashm1/ecommerce-microservice-backend-app:proxy-latest"
    "favourite" = "sebashm1/ecommerce-microservice-backend-app:favourite-latest"
    "zipkin"    = "openzipkin/zipkin:latest" // También se puede gestionar aquí
  }
}



variable "k8s_namespace" {
  description = "El namespace de Kubernetes donde se desplegarán los recursos."
  type        = string
  default     = "dev"
}

variable "spring_profile" {
  description = "Perfil de Spring a activar para las aplicaciones."
  type        = string
  default     = "dev"
}