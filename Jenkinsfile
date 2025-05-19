pipeline {
    agent any
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

        stage('Set Docker to Minikube Env') {
            steps {
                bat '''
                for /f "delims=" %%i in ('minikube docker-env --shell cmd') do call %%i
                '''
            }
        }

        stage('Build Images in Minikube Docker') {
            steps {
                script {
                    def services = [
                        'service-discovery',
                        'cloud-config',
                        'api-gateway',
                        'proxy-client',
                        'order-service',
                        'payment-service',
                        'product-service',
                        'shipping-service',
                        'user-service',
                        'favourite-service'
                    ]
                    for (svc in services) {
                        dir(svc) {
                            //bat "mvnw.cmd clean package -DskipTests"
                            bat "minikube image build -t ${svc}:latest ."
                        }
                    }
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                    def services = [
                        'zipkin',
                        'service-discovery',
                        'cloud-config',
                        'api-gateway',
                        'proxy-client',
                        'order-service',
                        'payment-service',
                        'product-service',
                        'shipping-service',
                        'user-service',
                        'favourite-service'
                    ]
                    for (svc in services) {
                        bat "kubectl apply -f k8s/${svc}-deployment.yaml"
                        bat "kubectl apply -f k8s/${svc}-service.yaml"
                        // Forzar el reinicio del deployment
                        bat "kubectl rollout restart deployment/${svc}"
                    }
                }
            }
        }
    }
}
