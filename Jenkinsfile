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
        stage('Verify Tools and Minikube Status') {
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
                
                echo "Checking Minikube status..."
                # Intenta sin especificar el perfil, a ver si lo detecta
                minikube status 
                if ! minikube status | grep -qE "(host: Running|Stopped)"; then # Acepta Running o Stopped inicialmente
                    # Si no es Running o Stopped, podría ser un estado desconocido o error
                    if minikube status | grep -q "Profile .* not found"; then
                        echo "Minikube profile not found by CLI. Attempting to use kubectl context."
                        # Como fallback, si el API server es accesible, asumimos que está "ok"
                        # Esto es una suposición, pero evita el error de perfil no encontrado si el clúster está funcional
                        if kubectl cluster-info; then
                           echo "Kubectl can connect to cluster. Assuming Minikube is functional."
                        else
                           echo "ERROR: Minikube status unknown and kubectl cannot connect."
                           exit 1
                        fi
                    elif ! minikube status | grep -q "host: Running"; then
                         echo "ERROR: Minikube is not running!"
                         exit 1
                    fi
                fi
                echo "Minikube is accessible or status confirmed."
                '''
            }
        }

        stage('Set Docker to Minikube Env') {
            steps {
                sh '''
                set -ex
                echo "Attempting to set Minikube Docker environment..."
                # Intenta sin -p minikube
                eval $(minikube docker-env) 
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