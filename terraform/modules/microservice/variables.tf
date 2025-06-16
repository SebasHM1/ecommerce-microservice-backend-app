# terraform/modules/microservice/variables.tf

variable "name" {
  description = "El nombre del microservicio (ej. 'api-gateway'). Se usará para nombrar los recursos."
  type        = string
}

variable "namespace" {
  description = "El namespace de Kubernetes donde se desplegará el servicio."
  type        = string
}

variable "image" {
  description = "La URL completa de la imagen Docker a usar (ej. 'sebashm1/repo:tag')."
  type        = string
}

variable "replicas" {
  description = "Número de réplicas para el Deployment."
  type        = number
  default     = 1 # Un valor por defecto sensato para entornos de desarrollo.
}

variable "container_port" {
  description = "Puerto que expone el contenedor de la aplicación."
  type        = number
  default     = 8080
}

variable "spring_profile" {
  description = "El perfil de Spring Boot a activar en el contenedor (dev, stage, prod)."
  type        = string
}

variable "labels" {
  description = "Un mapa de etiquetas adicionales para aplicar a los recursos."
  type        = map(string)
  default     = {}
}

variable "env_vars" {
  description = "Un mapa de variables de entorno para inyectar en el contenedor."
  type        = map(string)
  default     = {}
}

variable "init_containers_config" {
  description = "Una lista de mapas, donde cada mapa define un initContainer."
  type = list(object({
    name    = string
    image   = string
    command = list(string)
  }))
  default = [] # Por defecto, no hay initContainers
}

variable "health_check_type" {
  description = "Tipo de sonda de salud: 'http' o 'command'."
  type        = string
  default     = "http"
}

# --- ¡AÑADE ESTA VARIABLE FALTANTE! ---
variable "health_check_path" {
  description = "La ruta para las sondas de salud HTTP (ej. /actuator/health)."
  type        = string
  default     = "/actuator/health"
}

variable "actuator_path" {
  description = "The full path to the prometheus actuator endpoint."
  type        = string
  default     = "/actuator/prometheus" # Valor por defecto para los que no tienen context-path
}