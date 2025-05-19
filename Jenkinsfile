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
                            bat "docker build -t ${svc}:latest ."
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
