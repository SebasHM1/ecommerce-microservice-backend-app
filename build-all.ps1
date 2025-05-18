# Script para compilar todos los microservicios
Write-Host "Compilando todos los microservicios..."

# Lista de proyectos
$projects = @(
    "service-discovery",
    "cloud-config",
    "api-gateway",
    "proxy-client",
    "order-service",
    "payment-service",
    "product-service",
    "shipping-service",
    "user-service",
    "favourite-service"
)

# Compilar cada proyecto
foreach ($project in $projects) {
    Write-Host "Compilando $project..."
    Set-Location -Path ".\$project"
    
    # Ejecutar Maven para compilar
    & .\mvnw.cmd clean package -DskipTests
    
    # Verificar si la compilación fue exitosa
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error al compilar $project. Abortando." -ForegroundColor Red
        exit $LASTEXITCODE
    }
    
    # Volver al directorio raíz
    Set-Location -Path ".."
    Write-Host "Compilación de $project completada." -ForegroundColor Green
}

Write-Host "Todos los proyectos han sido compilados exitosamente." -ForegroundColor Green
Write-Host "Ahora puedes ejecutar 'docker-compose up' para iniciar los contenedores."
