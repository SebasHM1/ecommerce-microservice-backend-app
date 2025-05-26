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
    image: openjdk:17-jdk # Sigue siendo esta, pero ahora sabemos que es Oracle Linux
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

                    # Oracle Linux usa dnf (o yum)
                    # Asegurarse de que los repositorios estén actualizados (dnf makecache o yum makecache)
                    # No es estrictamente 'update' como en apt, sino asegurar que los metadatos estén frescos.
                    dnf makecache --timer # --timer solo actualiza si el cache es viejo

                    # Instalar Maven, sudo, y otras herramientas necesarias
                    # Nombres de paquetes pueden variar. ej. 'maven' puede ser 'apache-maven'
                    # 'git' es usualmente 'git'
                    # 'docker.io' sería 'docker-ce-cli' o similar de los repos de Docker.
                    # Vamos a asumir nombres comunes, puede requerir ajuste.
                    dnf install -y maven sudo curl wget git # Instalar 'which' para depuración futura
                    
                    # Instalar 'which' si no está, para depuración
                    if ! command -v which &> /dev/null; then
                        dnf install -y which
                    fi

                    if ! command -v kubectl &> /dev/null; then
                        echo "Installing kubectl..."
                        # La instalación de kubectl es universal
                        curl -sLO "https://dl.k8s.io/release/$(curl -sL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
                        install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
                    else
                        echo "kubectl already installed"
                    fi
                    kubectl version --client

                    if ! command -v minikube &> /dev/null; then
                        echo "Installing Minikube CLI..."
                        # La instalación de Minikube es universal
                        curl -sLo minikube https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
                        install -o root -g root -m 0755 minikube /usr/local/bin/minikube
                    else
                        echo "Minikube CLI already installed"
                    fi
                    minikube version

                    if ! command -v docker &> /dev/null; then
                        echo "Installing Docker client..."
                        # Para Oracle Linux 8 / RHEL 8, se usa el repo de Docker
                        # Esto es un poco más complejo que un simple 'dnf install docker.io'
                        # Paso 1: Añadir el repositorio de Docker (si no está ya)
                        # Puede que necesites 'dnf install -y dnf-utils device-mapper-persistent-data lvm2' para 'dnf config-manager'
                        dnf install -y dnf-plugins-core
                        dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
                        # Paso 2: Instalar Docker CE CLI
                        dnf install -y docker-ce-cli --nobest # --nobest para evitar problemas de dependencias si el daemon no se instala
                    else
                        echo "Docker client already installed"
                    fi
                    docker --version
                    
                    mvn -version
                    '''
                }
            }
        }

        // RESTO DE LOS STAGES SIN CAMBIOS
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
            // deleteDir() 
        }
    }
}