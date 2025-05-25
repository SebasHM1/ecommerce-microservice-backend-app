pipeline {
    agent any
    stages {
        stage('Start Minikube if needed') {
            steps {
                sh '''
                if ! minikube status | grep -q "host: Running"; then
                    echo "Minikube no está iniciado. Iniciando..."
                    minikube start --cpus=6 --memory=3800
                else
                    echo "Minikube ya está corriendo."
                fi
                '''
            }
        }

        stage('Set Docker to Minikube Env') {
            steps {
                sh '''
                eval $(minikube -p minikube docker-env)
                '''
            }
        }

        stage('Build and Package Services') {
            steps {
                script {
                    def servicesToBuild = [
                        'service-discovery',
                        'cloud-config',
                        'api-gateway',
                        'proxy-client',
                        'order-service',
                        'product-service',
                        'user-service',
                        'shipping-service'
                    ]
                    for (svc in servicesToBuild) {
                        dir(svc) {
                            echo "Building and packaging ${svc}..."
                            sh "./mvnw clean package -DskipTests"
                            
                        }
                    }
                }
            }
        }

        stage('Build Images in Minikube Docker') {
            steps {
                script {
                    def servicesToImage = [
                        'service-discovery',
                        'cloud-config',
                        'api-gateway',
                        'proxy-client',
                        'order-service',
                        'product-service',
                        'user-service',
                        'shipping-service'
                    ]
                    for (svc in servicesToImage) {
                        dir(svc) {
                            echo "Building Docker image for ${svc}..."
                            sh "minikube image build -t ${svc}:latest ."
            
                        }
                    }
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                    def servicesToDeploy = [
                        'zipkin', 
                        'service-discovery',
                        'cloud-config',
                        'api-gateway',
                        'proxy-client',
                        'order-service',
                        'product-service',
                        'user-service',
                        'shipping-service',/*
                        'favourite-service',
                        'payment-service'*/
                    ]
                    for (svc in servicesToDeploy) {
                        echo "Deploying ${svc}..."
                        sh "kubectl apply -f k8s/${svc}-deployment.yaml"
                        sh "kubectl apply -f k8s/${svc}-service.yaml"
                        // Si quieres forzar el reinicio del deployment
                        // sh "kubectl rollout restart deployment/${svc}"
                    }
                }
            }
        }
    }
}