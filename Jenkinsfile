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
        K8S_NAMESPACE = "dev" // Default
        SPRING_ACTIVE_PROFILE_APP = "dev" // Perfil Spring para la aplicación desplegada
        IMAGE_TAG_SUFFIX = "-dev"
        MAVEN_PROFILES = "" // Perfiles Maven a activar
    }

    stages {
        stage('Initialize Environment & Determine Test Strategy') {
            steps {
                script {
                    // Determinar el entorno basado en la rama
                    if (env.GIT_BRANCH ==~ /.*\/develop.*/) {
                        echo "ENVIRONMENT: DEV"
                        K8S_NAMESPACE = "dev"
                        SPRING_ACTIVE_PROFILE_APP = "dev"
                        IMAGE_TAG_SUFFIX = "-dev"
                        // Para DEV: Correr unit tests (por defecto con `mvn package`), saltar ITs explícitamente
                        MAVEN_PROFILES = "-Pskip-its" 
                    } else if (env.GIT_BRANCH ==~ /.*\/staging.*/) {
                        echo "ENVIRONMENT: STAGE"
                        K8S_NAMESPACE = "stage"
                        SPRING_ACTIVE_PROFILE_APP = "stage"
                        IMAGE_TAG_SUFFIX = "-stage-${env.BUILD_NUMBER}"
                        // Para STAGE: Correr unit tests Y tests de integración
                        MAVEN_PROFILES = "-Prun-its" 
                    } else if (env.GIT_BRANCH ==~ /.*\/master.*/) {
                        echo "ENVIRONMENT: PROD"
                        K8S_NAMESPACE = "prod"
                        SPRING_ACTIVE_PROFILE_APP = "prod"
                        def gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        IMAGE_TAG_SUFFIX = "-prod-${gitCommit}" 
                        // Para PROD: Correr unit tests Y tests de integración
                        MAVEN_PROFILES = "-Prun-its"
                    } else { // Feature branches
                        echo "ENVIRONMENT: FEATURE (Dev-like)"
                        K8S_NAMESPACE = "dev" 
                        SPRING_ACTIVE_PROFILE_APP = "dev"
                        def branchName = env.GIT_BRANCH.split('/').last().replaceAll("[^a-zA-Z0-9_.-]", "_") // Sanitize branch name
                        IMAGE_TAG_SUFFIX = "-feature-${branchName}-${env.BUILD_NUMBER}"
                        MAVEN_PROFILES = "-Pskip-its"
                    }
                    echo "K8S Namespace: ${K8S_NAMESPACE}"
                    echo "Spring Profile for Deployed App: ${SPRING_ACTIVE_PROFILE_APP}"
                    echo "Image Tag Suffix: ${IMAGE_TAG_SUFFIX}"
                    echo "Maven Profiles for Tests: ${MAVEN_PROFILES}"
                }
            }
        }
    
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

        stage('Compile, Test, and Package') { // Un solo stage para build y tests
            steps {
                script {
                    def servicesToProcess = [ /* tu lista de servicios */ 
                        'service-discovery', 'cloud-config', 'api-gateway', 'proxy-client',
                        'order-service', 'product-service', 'user-service', 'shipping-service'
                    ]
                    for (svc in servicesToProcess) {
                        dir(svc) {
                            echo "Processing service: ${svc}"
                            sh "chmod +x ./mvnw"
                            // Usamos 'mvn verify' para asegurar que se ejecuten todas las fases hasta Failsafe
                            // Los perfiles activados controlarán QUÉ tests de Failsafe se ejecutan (o si se saltan).
                            // Surefire (unit tests) siempre se ejecuta en la fase 'test' a menos que se salte con -DskipTests.
                            // Tu perfil 'skip-its' solo afecta a Failsafe con la propiedad <skipITs>.
                            // Para saltar unit tests también con un perfil, tendrías que usar <skipTests>true</skipTests> en ese perfil.
                            
                            // Si MAVEN_PROFILES contiene "-Pskip-its", solo se correrán unit tests (Surefire).
                            // Si MAVEN_PROFILES contiene "-Prun-its", se correrán unit tests (Surefire) Y tests de integración (Failsafe).
                            sh "./mvnw clean verify ${MAVEN_PROFILES}"
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
                            'shipping-service' : 'shipping',
                            'payment-service'  : 'payment'
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
                        'shipping-service',
                        'payment-service'
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