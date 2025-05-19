# Detener todos los recursos de Kubernetes definidos en la carpeta k8s/
# Ejecutar desde la raíz del proyecto
kubectl delete -f k8s/
Write-Host "Todos los pods y servicios definidos en k8s/ han sido eliminados."
