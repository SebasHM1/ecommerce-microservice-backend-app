# terraform/dev/variables.tf

variable "image_tag_suffix" {
  description = "El sufijo de la tag de la imagen a desplegar (ej. '-dev', '-stage-123')."
  type        = string
}

variable "dockerhub_user" {
  description = "Usuario de Docker Hub."
  type        = string
}

variable "repo_prefix" {
  description = "El prefijo del repositorio en Docker Hub."
  type        = string
}

variable "spring_profile" {
  description = "Perfil de Spring a activar para las aplicaciones."
  type        = string
}