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
                
                echo "--- KUBECTL CONTEXT ---"
                kubectl config current-context || echo "Could not get kubectl current-context"
                kubectl cluster-info || echo "Could not get kubectl cluster-info"
                
                echo "--- MINIKUBE PROFILE LIST (EXPECTED TO FAIL OR BE EMPTY) ---"
                minikube profile list || echo "minikube profile list failed as expected"

                echo "--- ATTEMPTING MINIKUBE STATUS (EXPECTED TO FAIL ON PROFILE) ---"
                minikube status || echo "minikube status failed as expected due to profile"

                echo "Assuming Minikube is running because Jenkins is in it."
                '''
            }
        }

        stage('Set Docker to Minikube Env') {
            steps {
                sh '''
                set -ex
                echo "Attempting to set Minikube Docker environment..."
                
                # Intentar forzar el contexto de Minikube al contexto actual de kubectl
                # Esto es una prueba, puede que no funcione o no sea necesario.
                # CURRENT_KUBE_CONTEXT=$(kubectl config current-context)
                # if [ -n "$CURRENT_KUBE_CONTEXT" ]; then
                #   echo "Attempting to set Minikube context to: $CURRENT_KUBE_CONTEXT"
                #   minikube profile "$CURRENT_KUBE_CONTEXT" || echo "Failed to set minikube profile, continuing..."
                # fi

                # El comando clave:
                DOCKER_ENV_COMMAND="minikube docker-env --shell sh"
                echo "Executing: $DOCKER_ENV_COMMAND"
                
                # Ejecutar y capturar la salida, incluso si falla, para verla
                DOCKER_ENV_OUTPUT=$($DOCKER_ENV_COMMAND) || DOCKER_ENV_EXIT_CODE=$?
                
                echo "--- Minikube Docker Env Output ---"
                echo "$DOCKER_ENV_OUTPUT"
                echo "----------------------------------"

                if [ -n "$DOCKER_ENV_EXIT_CODE" ] && [ "$DOCKER_ENV_EXIT_CODE" -ne 0 ]; then
                    echo "minikube docker-env failed with exit code $DOCKER_ENV_EXIT_CODE"
                    # Intentar obtener más info si falla
                    minikube docker-env --shell sh -v=7 || echo "Verbose docker-env also failed"
                    exit 1
                fi
                
                eval "$DOCKER_ENV_OUTPUT"
                
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