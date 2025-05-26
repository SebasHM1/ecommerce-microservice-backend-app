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
    resources:
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
    environment {
        DOCKERHUB_USER = 'sebashm1'
        DOCKERHUB_REPO_PREFIX = 'ecommerce-microservice-backend-app' 
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
                echo "--- DOCKER PS (Verificando acceso al daemon Docker del nodo) ---"
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

        stage('Build and Push Docker Images to Registry') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-sebashm1', usernameVariable: 'DOCKER_CRED_USER', passwordVariable: 'DOCKER_CRED_PSW')]) {
                    script {
                        sh "echo \$DOCKER_CRED_PSW | docker login -u \$DOCKER_CRED_USER --password-stdin"

                        // Mapeo de nombre de servicio a tag de imagen (ajusta según tus tags)
                        def serviceToTagMap = [
                            'service-discovery': 'discovery',
                            'cloud-config'     : 'config',
                            'api-gateway'      : 'gateway',
                            'proxy-client'     : 'proxy', // Asumiendo que 'proxy-client' es 'proxy' en el tag
                            'order-service'    : 'order',
                            'product-service'  : 'product', // Necesitas un tag para 'product-service'
                            'user-service'     : 'users',
                            'shipping-service' : 'shipping'
                        ]

                        for (svcDirName in serviceToTagMap.keySet()) {
                            def imageTag = serviceToTagMap[svcDirName]
                            if (imageTag) { // Solo procesa si hay un tag mapeado
                                dir(svcDirName) { // Entra al directorio del servicio
                                    def imageNameWithRegistry = "${DOCKERHUB_USER}/${DOCKERHUB_REPO_PREFIX}:${imageTag}"
                                    
                                    echo "Building Docker image ${imageNameWithRegistry} from directory ${svcDirName}..."
                                    sh "docker build -t ${imageNameWithRegistry} ."
                                    
                                    echo "Pushing image ${imageNameWithRegistry} to Docker Hub..."
                                    sh "docker push ${imageNameWithRegistry}"
                                }
                            } else {
                                echo "Skipping Docker build/push for service directory ${svcDirName} as no tag is mapped."
                            }
                        }
                        sh "docker logout"
                    }
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                    // El servicio zipkin se asume que es una imagen pública o ya está en el registro
                    // y su deployment.yaml apunta a esa imagen.
                    def servicesToDeployNames = [
                        'zipkin', // Asegúrate que el deployment de zipkin use una imagen de un registro
                        'service-discovery',
                        'cloud-config',
                        'api-gateway',
                        'proxy-client',
                        'order-service',
                        'product-service',
                        'user-service',
                        'shipping-service'
                    ]
                    for (svcName in servicesToDeployNames) {
                        // El nombre del archivo YAML podría ser diferente al nombre del servicio/tag
                        // Asumimos que los archivos YAML son como <nombreServicio>-deployment.yaml
                        // Ej. service-discovery-deployment.yaml, proxy-client-deployment.yaml
                        def yamlBaseName = svcName 
                        if (svcName == "proxy-client") { 
                            // Si el archivo es proxy-deployment.yaml pero el servicio es proxy-client
                            // podrías necesitar un mapeo o una lógica aquí.
                            // Por ahora, asumimos que el nombre del servicio es el base para el YAML.
                        }

                        dir("k8s") {
                            echo "Deploying ${yamlBaseName}..."
                            sh "kubectl apply -f ${yamlBaseName}-deployment.yaml"
                            sh "kubectl apply -f ${yamlBaseName}-service.yaml"
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