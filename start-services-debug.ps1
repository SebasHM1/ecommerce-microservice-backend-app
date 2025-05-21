# Script para iniciar servicios en orden específico
Write-Host "Iniciando servicios en orden controlado..." -ForegroundColor Cyan

# 1. Primero, detener y eliminar contenedores anteriores
Write-Host "Deteniendo y eliminando contenedores anteriores..." -ForegroundColor Yellow
docker-compose down

# 2. Limpiar imágenes y volúmenes no utilizados
Write-Host "Limpiando recursos Docker no utilizados..." -ForegroundColor Yellow
docker system prune -f

# 3. Iniciar Zipkin primero
Write-Host "Iniciando Zipkin..." -ForegroundColor Green
docker-compose up -d zipkin-container
Start-Sleep -Seconds 5

# 6. Iniciar Eureka (Service Discovery)
Write-Host "Iniciando Eureka Service Discovery..." -ForegroundColor Green
docker-compose up -d service-discovery-container
Start-Sleep -Seconds 20  # Esperar a que Eureka esté completamente inicializado


# 4. Iniciar Cloud Config
Write-Host "Iniciando Cloud Config Server..." -ForegroundColor Green
docker-compose up -d cloud-config-container
Start-Sleep -Seconds 20  # Esperar a que el Config Server esté completamente inicializado




# 5. Verificar que el Config Server está ejecutándose
Write-Host "Verificando estado del Cloud Config Server..." -ForegroundColor Cyan
docker logs cloud-config-container

# 7. Iniciar el resto de los servicios
Write-Host "Iniciando el resto de los servicios..." -ForegroundColor Green
docker-compose up -d

# 8. Mostrar los servicios en ejecución
Write-Host "Servicios iniciados. Lista de contenedores en ejecución:" -ForegroundColor Cyan
docker ps

# 9. Mostrar los logs
Write-Host "¿Deseas ver los logs en tiempo real? [S/N]" -ForegroundColor Yellow
$respuesta = Read-Host
if ($respuesta -eq "S" -or $respuesta -eq "s") {
    docker-compose logs -f
}
else {
    Write-Host "Puedes ver los logs en cualquier momento con: docker-compose logs -f" -ForegroundColor Cyan
}