pipeline {
    agent {
        kubernetes {
            defaultContainer 'tools'
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jnlp
    image: jenkins/inbound-agent:jdk17 
  - name: tools
    image: sebashm1/jenkins-tools-completa:jdk17 
    command: ['sleep']
    args: ['infinity']
    tty: true
    volumeMounts:
    - name: docker-sock
      mountPath: /var/run/docker.sock
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: Socket
'''
        }
    }
    stages {
        stage('Verify Tools and Minikube Status') { // Cambiado el nombre del stage
            steps {
                sh '''
                set -ex
                echo "Verifying tools in custom image..."
                java -version
                mvn -version
                docker --version
                kubectl version --client
                minikube version
                echo "All tools verified."
                
                echo "Checking Minikube status (it should be running as Jenkins is in it)..."
                minikube status
                if ! minikube status | grep -q "host: Running"; then
                    echo "ERROR: Minikube is not running or status cannot be determined!"
                    echo "This pipeline expects to run within an active Minikube insulina."
                    exit 1 # Falla el pipeline si Minikube no está como se espera
                fi
                echo "Minikube is running."
                '''
            }
        }

        // EL STAGE 'Start Minikube if needed' HA SIDO REEMPLAZADO/FUSIONADO ARRIBA

        stage('Set Docker to Minikube Env') {
            steps {
                sh '''
                set -ex
                echo "Attempting to set Minikube Docker environment..."
                # Usamos el Minikube existente donde corre este pod
                eval $(minikube -p minikube docker-env) 
                echo "Minikube Docker environment hopefully set."
                docker ps 
                '''
            }
        }

        // ... El resto de los stages (Build, Deploy) permanecen igual ...
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
                            sh "chmod +x ./mvnw"
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
                        'shipping-service'
                    ]
                    for (svc in servicesToDeploy) {
                        dir("k8s") {
                            echo "Deploying ${svc}..."
                            sh "kubectl apply -f ${svc}-deployment.yaml"
                            sh "kubectl apply -f ${svc}-service.yaml"
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            echo "Pipeline finished."
            // deleteDir()
        }
    }
}