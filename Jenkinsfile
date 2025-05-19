pipeline {
    agent any
    environment {
        REGISTRY = "localhost:58687"
    }
    stages {
        stage('Start Minikube if needed') {
            steps {
                bat '''
                minikube status | findstr /C:"host: Running" >nul
                if %ERRORLEVEL% NEQ 0 (
                    echo Minikube no está iniciado. Iniciando...
                    minikube start
                ) else (
                    echo Minikube ya está corriendo.
                )
                '''
            }
        }
        stage('Build & Push Images') {
            steps {
                script {
                    def services = [
                        'api-gateway',
                        'cloud-config',
                        'favourite-service',
                        'order-service',
                        'payment-service',
                        'product-service',
                        'proxy-client',
                        'service-discovery',
                        'shipping-service',
                        'user-service'
                    ]
                    for (svc in services) {
                        dir(svc) {
                            bat "mvnw.cmd clean package -DskipTests"
                            bat "docker build -t %REGISTRY%/${svc}:latest ."
                            bat "docker push %REGISTRY%/${svc}:latest"
                        }
                    }
                }
            }
        }
        stage('Deploy to Minikube') {
            steps {
                bat 'kubectl apply -f k8s/'
            }
        }
    }
}
