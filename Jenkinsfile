pipeline {
    agent any
    environment {
        REGISTRY = "dev"
    }
    stages {
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
                        }
                        bat "docker build -t %REGISTRY%/${svc}:latest -f ${svc}/Dockerfile ${svc}"
                        bat "docker push %REGISTRY%/${svc}:latest"
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
