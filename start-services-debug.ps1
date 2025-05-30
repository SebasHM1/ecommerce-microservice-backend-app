# Script para iniciar servicios en orden específico
Write-Host "`n==> Iniciando servicios en orden controlado..." -ForegroundColor Cyan

# 1. Detener y eliminar contenedores anteriores
Write-Host "1. Deteniendo y eliminando contenedores anteriores..." -ForegroundColor Yellow
docker-compose down

#2. (Opcional) Limpiar recursos Docker no utilizados
Write-Host "2. Limpiando recursos Docker no utilizados..." -ForegroundColor Yellow
docker system prune -f

# 3. Iniciar Zipkin
Write-Host "3. Iniciando Zipkin..." -ForegroundColor Green
docker-compose up -d zipkin
Start-Sleep -Seconds 5


# 4. Iniciar Eureka (Service Discovery)
Write-Host "4. Iniciando Eureka Service Discovery..." -ForegroundColor Green
docker-compose up -d service-discovery
Start-Sleep -Seconds 20

# 5. Iniciar Cloud Config Server
Write-Host "5. Iniciando Cloud Config Server..." -ForegroundColor Green
docker-compose up -d cloud-config
Start-Sleep -Seconds 20

# 5. Verificar que el Config Server está ejecutándose
Write-Host "6. Verificando estado del Cloud Config Server..." -ForegroundColor Cyan
docker logs cloud-config --tail 20


# 7. Iniciar el resto de los servicios
Write-Host "7. Iniciando el resto de los servicios..." -ForegroundColor Green
docker-compose up -d api-gateway proxy-client order-service product-service user-service shipping-service payment-service
# No iniciados:  y favourite-service

# 8. Mostrar los servicios en ejecución
Write-Host "`n8. Servicios iniciados. Lista de contenedores en ejecución:" -ForegroundColor Cyan
docker ps

# 9. Mostrar los logs si el usuario lo desea
Write-Host "`n¿Deseas ver los logs en tiempo real? [S/N]" -ForegroundColor Yellow
$respuesta = Read-Host
if ($respuesta -eq "S" -or $respuesta -eq "s") {
    docker-compose logs -f
}
else {
    Write-Host "Puedes ver los logs en cualquier momento con: docker-compose logs -f" -ForegroundColor Cyan
}
