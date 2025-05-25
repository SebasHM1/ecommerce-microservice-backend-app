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
    image: openjdk:17-jdk-slim
  - name: tools
    image: maven:3.8-openjdk-17
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
        stage('Install Prerequisite Tools in Tools Container') {
            steps {
                container('tools') {
                    sh '''
                    set -ex

                    if ! command -v sudo &> /dev/null; then
                        apt-get update && apt-get install -y sudo
                    fi
                    
                    apt-get update && apt-get install -y curl wget apt-transport-https ca-certificates gnupg git

                    if ! command -v kubectl &> /dev/null; then
                        echo "Installing kubectl..."
                        curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
                        install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
                    else
                        echo "kubectl already installed"
                    fi
                    kubectl version --client

                    if ! command -v minikube &> /dev/null; then
                        echo "Installing Minikube CLI..."
                        curl -Lo minikube https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
                        install -o root -g root -m 0755 minikube /usr/local/bin/minikube
                    else
                        echo "Minikube CLI already installed"
                    fi
                    minikube version

                    if ! command -v docker &> /dev/null; then
                        echo "Installing Docker client..."
                        apt-get install -y docker.io
                    else
                        echo "Docker client already installed"
                    fi
                    docker --version
                    '''
                }
            }
        }

        stage('Start Minikube if needed') {
            steps {
                sh '''
                set -ex
                if ! minikube status | grep -q "host: Running"; then
                    echo "Minikube no está iniciado. Iniciando..."
                    minikube start --driver=docker --cpus=6 --memory=3800
                else
                    echo "Minikube ya está corriendo."
                fi
                minikube status
                '''
            }
        }

        stage('Set Docker to Minikube Env') {
            steps {
                sh '''
                set -ex
                eval $(minikube -p minikube docker-env)
                docker ps
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
            deleteDir()
        }
    }
}