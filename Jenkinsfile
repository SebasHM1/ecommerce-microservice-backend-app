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
    resources: # Añadir recursos para el contenedor tools
      requests:
        cpu: "500m" 
        memory: "1Gi" 
      limits:
        cpu: "2"    
        memory: "2Gi" 
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
        stage('Verify Tools and Minikube Context') {
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
                
                echo "--- KUBECTL CLUSTER INFO (Verificando conexión al API Server) ---"
                # Este comando puede fallar por permisos del SA, pero indica que kubectl intenta conectar.
                kubectl cluster-info || echo "kubectl cluster-info may fail due to SA permissions, this is often OK."
                
                echo "Minikube should be running as Jenkins is hosted within it."
                # Verificar si 'docker ps' funciona aquí, indicando acceso al Docker daemon del nodo
                echo "--- DOCKER PS (Verificando acceso al daemon Docker del nodo) ---"
                docker ps 
                '''
            }
        }

        // No se necesita 'Set Docker to Minikube Env' si usamos 'docker build' directo
        // y el socket está montado.

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

        stage('Build Docker Images and Load into Minikube') {
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
                            echo "Building Docker image for ${svc} using 'docker build'..."
                            sh "docker build -t ${svc}:latest ."
                            echo "Loading image ${svc}:latest into Minikube..."
                            # Intenta sin perfil primero, si falla, considera añadir -p minikube si es tu perfil por defecto
                            sh "minikube image load ${svc}:latest" 
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
            // Comenta deleteDir() hasta que todo el pipeline sea estable
            // deleteDir()
        }
    }
}