# terraform/dev/imports.tf
#
# Este archivo es TEMPORAL y se usa para importar recursos existentes
# al estado de Terraform de forma declarativa.

# Importación del Namespace (si lo estás gestionando con 'resource')
import {
  to = kubernetes_namespace.env_ns
  id = "dev"
}

# Importación de los recursos de Zipkin
import {
  to = kubernetes_deployment.zipkin
  id = "dev/zipkin"
}
import {
  to = kubernetes_service.zipkin
  id = "dev/zipkin"
}

# --- Importación de todos los microservicios ---

# api-gateway
import {
  to = module.microservice["api-gateway"].kubernetes_deployment.app
  id = "dev/api-gateway"
}
import {
  to = module.microservice["api-gateway"].kubernetes_service.app
  id = "dev/api-gateway"
}

# user-service
import {
  to = module.microservice["user-service"].kubernetes_deployment.app
  id = "dev/user-service"
}
import {
  to = module.microservice["user-service"].kubernetes_service.app
  id = "dev/user-service"
}

# product-service
import {
  to = module.microservice["product-service"].kubernetes_deployment.app
  id = "dev/product-service"
}
import {
  to = module.microservice["product-service"].kubernetes_service.app
  id = "dev/product-service"
}

# order-service
import {
  to = module.microservice["order-service"].kubernetes_deployment.app
  id = "dev/order-service"
}
import {
  to = module.microservice["order-service"].kubernetes_service.app
  id = "dev/order-service"
}

# payment-service
import {
  to = module.microservice["payment-service"].kubernetes_deployment.app
  id = "dev/payment-service"
}
import {
  to = module.microservice["payment-service"].kubernetes_service.app
  id = "dev/payment-service"
}

# shipping-service
import {
  to = module.microservice["shipping-service"].kubernetes_deployment.app
  id = "dev/shipping-service"
}
import {
  to = module.microservice["shipping-service"].kubernetes_service.app
  id = "dev/shipping-service"
}

# service-discovery
import {
  to = module.microservice["service-discovery"].kubernetes_deployment.app
  id = "dev/service-discovery"
}
import {
  to = module.microservice["service-discovery"].kubernetes_service.app
  id = "dev/service-discovery"
}

# cloud-config
import {
  to = module.microservice["cloud-config"].kubernetes_deployment.app
  id = "dev/cloud-config"
}
import {
  to = module.microservice["cloud-config"].kubernetes_service.app
  id = "dev/cloud-config"
}