# Script para compilar todos los microservicios o uno específico
Write-Host "Compilando microservicios..."

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

# Si se pasa un argumento, solo compilar ese microservicio
if ($args.Count -ge 1) {
    $service = $args[0]
    if ($projects -contains $service) {
        $projects = @($service)
    } else {
        Write-Host "El microservicio '$service' no existe en la lista." -ForegroundColor Red
        exit 1
    }
}

# Compilar cada proyecto
foreach ($project in $projects) {
    Write-Host "Compilando $project..."
    Set-Location -Path ".\$project"
    
    # Ejecutar Maven para compilar
    & .\mvnw.cmd clean package -DskipTests -e
    
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
