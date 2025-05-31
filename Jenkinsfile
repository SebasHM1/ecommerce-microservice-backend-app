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
        RUN_E2E_TESTS = "true" // Nueva variable para controlar tests E2E
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
                        RUN_E2E_TESTS = "true" // de pruebas
                    } else if (env.GIT_BRANCH ==~ /.*\/staging.*/) {
                        echo "ENVIRONMENT: STAGE"
                        K8S_NAMESPACE = "stage"
                        SPRING_ACTIVE_PROFILE_APP = "stage"
                        IMAGE_TAG_SUFFIX = "-stage-${env.BUILD_NUMBER}"
                        // Para STAGE: Correr unit tests Y tests de integración
                        MAVEN_PROFILES = "-Prun-its" 
                        RUN_E2E_TESTS = "true" // E2E en Staging
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
                        RUN_E2E_TESTS = "false" // E2E en Prod (Master)
                    }
                    echo "K8S Namespace: ${K8S_NAMESPACE}"
                    echo "Spring Profile for Deployed App: ${SPRING_ACTIVE_PROFILE_APP}"
                    echo "Image Tag Suffix: ${IMAGE_TAG_SUFFIX}"
                    echo "Maven Profiles for Tests: ${MAVEN_PROFILES}"
                    echo "Run E2E Tests: ${RUN_E2E_TESTS}"
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
                node -v || echo "NodeJS no encontrado"
                npm -v || echo "NPM no encontrado"
                newman -v || echo "Newman no encontrado"
                echo "All tools verified."
                echo "--- DOCKER PS (Verificando acceso al daemon Docker del nodo) ---"
                docker ps 
                '''
            }
        }

        
        /*
        stage('Compile, Test, and Package') { // Un solo stage para build y tests
            steps {
                script {
                    def servicesToProcess = [
                        'service-discovery', 'cloud-config',  'user-service',  'api-gateway', 'order-service', 'product-service', 'shipping-service', 'payment-service', 'proxy-client'
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

        */

        stage('Deploy to Kubernetes Environment') {
            steps {
                script {
                    def servicesToDeploy = [ 
                        'zipkin', 
                        'service-discovery', 'cloud-config', 'api-gateway', 'proxy-client',
                        'order-service', 'product-service', 'user-service', 'shipping-service',
                        'payment-service', 'favourite-service'
                    ]
                    def serviceToFixedTagMap  = [
                        'service-discovery': 'discovery', 'cloud-config': 'config',
                        'api-gateway': 'gateway', 'proxy-client': 'proxy',
                        'order-service': 'order', 'product-service': 'product',
                        'user-service': 'users', 'shipping-service': 'shipping',
                        'payment-service': 'payment', 'favourite-service': 'favourite'
                    ]

                    for (yamlBaseName in servicesToDeploy) {
                        if (yamlBaseName == "zipkin") {
                            imageToDeployInK8s = "openzipkin/zipkin:latest" 
                            // Reemplazar la línea completa de imagen de Zipkin o un placeholder específico
                            // Usamos (?m) para modo multilínea y ^ para inicio de línea, \s* para espacios opcionales
                            processedDeploymentContent = originalDeploymentContent.replaceAll(~/(?m)^\s*image:\s*openzipkin\/zipkin:.*?$/, "image: ${imageToDeployInK8s}")
                            processedDeploymentContent = processedDeploymentContent.replaceAll(~/(?m)^\s*image:\s*IMAGE_PLACEHOLDER_ZIPKIN/, "image: ${imageToDeployInK8s}")
                        } else {
                            def imageBaseTag = serviceDirToImageBaseTag.get(yamlBaseName)
                            if (imageBaseTag == null) {
                                echo "ADVERTENCIA: No se encontró imageBaseTag para ${yamlBaseName}. El YAML no será modificado para la imagen."
                                // Aquí podrías decidir fallar el pipeline si el tag es crucial:
                                // error("No se encontró imageBaseTag para ${yamlBaseName}")
                            } else {
                                def finalImageTag = "${imageBaseTag}${IMAGE_TAG_SUFFIX}"
                                imageToDeployInK8s = "${DOCKERHUB_USER}/${DOCKERHUB_REPO_PREFIX}:${finalImageTag}"
                                
                                // Patrón para encontrar y reemplazar la línea de imagen.
                                // Busca 'image: tu/repo:tagBase' o 'image: tu/repo:tagBase-con-sufijo-anterior'
                                // Lo importante es que el nombre del repo y el imageBaseTag coincidan.
                                // (?m) - modo multilínea, ^\s* - inicio de línea con espacios opcionales
                                // \s*:\s* - dos puntos con espacios opcionales alrededor
                                // (?:-[a-zA-Z0-9_.-]*)? - un sufijo opcional que comienza con guion
                                String exactImageToReplacePattern = ~/(?m)^\s*image:\s*${DOCKERHUB_USER}\/${DOCKERHUB_REPO_PREFIX}:${imageBaseTag}(?:-[a-zA-Z0-9_.-]*)?\s*$/
                                String newLineForImage = "image: ${imageToDeployInK8s}"

                                // Alternativa: si el tag base en el YAML no tiene sufijos, es más simple:
                                String baseImageInYaml = "${DOCKERHUB_USER}/${DOCKERHUB_REPO_PREFIX}:${imageBaseTag}"

                                if (originalDeploymentContent.contains(baseImageInYaml)) {
                                    // Intenta reemplazar la línea completa que contiene la imagen base
                                    // Esto es más seguro si la línea 'image:' está bien formada.
                                    // Regex: busca la línea que empieza con 'image: ' seguido de la imagen base, y cualquier cosa hasta el fin de línea.
                                    def imageLineRegex = ~"image:\\s*${baseImageInYaml.replace(".", "\\.")}(\\s.*)?\$" 
                                    // El .replace(".", "\\.") es para escapar puntos en el nombre de la imagen si los hubiera (no en tu caso actual)
                                    processedDeploymentContent = originalDeploymentContent.replaceAll(imageLineRegex, newLineForImage)
                                } else {
                                // Fallback a un placeholder más genérico si el anterior no coincide
                                processedDeploymentContent = originalDeploymentContent.replaceAll(~/(image:\s*)IMAGE_PLACEHOLDER_FOR_SERVICE/, "\$1${imageToDeployInK8s}")
                                }

                                if (processedDeploymentContent == originalDeploymentContent) {
                                    echo "ADVERTENCIA: El reemplazo de imagen NO tuvo efecto para ${yamlBaseName}."
                                    echo "   - YAML original contenía: ${baseImageInYaml} ? ${originalDeploymentContent.contains(baseImageInYaml)}"
                                    echo "   - Se intentó reemplazar usando un patrón derivado de: ${baseImageInYaml}"
                                    echo "   - O el placeholder: IMAGE_PLACEHOLDER_FOR_SERVICE"
                                    echo "   - Nueva línea de imagen deseada: ${newLineForImage}"
                                    // Para depurar, imprime el contenido original si el reemplazo falla
                                    // echo "Contenido original del deployment para ${yamlBaseName}:\n${originalDeploymentContent}"
                                }
                            }
                        }

                        // Reemplazar el perfil Spring
                        processedDeploymentContent = processedDeploymentContent.replaceAll(~/(value:\s*)SPRING_PROFILE_PLACEHOLDER/, "\$1\"${SPRING_PROFILE_FOR_APP}\"")

                        writeFile(file: "processed-deployment.yaml", text: processedDeploymentContent)
                        sh "kubectl apply -f processed-deployment.yaml -n ${K8S_NAMESPACE}"
                        sh "rm processed-deployment.yaml"

                        // ... resto del código del stage Deploy ...
                        }
                        // ... (apply serviceFile) ...
                    }
            
                }
            }
        }

        stage('Run E2E Tests with Newman') {
            when { expression { env.RUN_E2E_TESTS == "true" } }
            steps {
                dir('postman-collections') { // Entra al directorio donde están los archivos
                    script {
                        def collectionsToRun = [
                            "E2E Test 1 - Users.postman_collection.json",
                            "E2E Test 2 - Product.postman_collection.json",
                            "E2E Test 3 - Order and Payment.postman_collection.json",
                            "E2E Test 4 - Shipping.postman_collection.json",
                            "E2E Test 5 - Delete Entities.postman_collection.json"
                        ]

                        // Usar el archivo de entorno global para Jenkins
                        def environmentFile = "JenkinsGlobalE2E.postman_environment.json" 

                        // Construir la URL base del API Gateway para el entorno actual
                        // Esto asume que tu API Gateway está desplegado y se llama 'api-gateway'
                        // en el K8S_NAMESPACE actual y escucha en el puerto 8080.
                        def apiGatewayInternalUrl = "http://api-gateway.${K8S_NAMESPACE}.svc.cluster.local:8080"
                        
                        if (!fileExists(environmentFile)) {
                            error("Archivo de entorno global de Postman '${environmentFile}' no encontrado en 'postman-collections/'.")
                        }
                        
                        for (int i = 0; i < collectionsToRun.size(); i++) {
                            def collectionFile = collectionsToRun[i]
                            def reportFileNameBase = collectionFile.replace(".postman_collection.json", "").replaceAll("[^a-zA-Z0-9.-]", "_")
                            def reportFile = "reporte-${reportFileNameBase}-${K8S_NAMESPACE}-${IMAGE_TAG_SUFFIX}-${env.BUILD_NUMBER}.html"

                            echo "===================================================================="
                            echo "Ejecutando Colección E2E: ${collectionFile}"
                            echo "Usando Entorno Postman: ${environmentFile}"
                            echo "Inyectando API_GATEWAY_URL: ${apiGatewayInternalUrl}"
                            echo "Reporte se guardará en: ${reportFile}"
                            echo "===================================================================="

                            if (!fileExists(collectionFile)) {
                                error("Archivo de colección de Postman '${collectionFile}' no encontrado en 'postman-collections/'.")
                            }

                            try {
                                sh """
                                    newman run "${collectionFile}" \
                                    -e "${environmentFile}" \
                                    --global-var "API_GATEWAY_URL=${apiGatewayInternalUrl}" \
                                    -r cli,htmlextra \
                                    --reporter-htmlextra-export "${reportFile}" \
                                    --reporter-htmlextra-omitResponseBodies \
                                    --reporter-htmlextra-showMarkdownLinks \
                                    # Decide si quieres --suppress-exit-code o no
                                    # Si lo quitas, el pipeline fallará si un test E2E falla.
                                """
                            } catch (Exception e) {
                                echo "Error al ejecutar la colección ${collectionFile}: ${e.getMessage()}"
                                currentBuild.result = 'UNSTABLE' // Marcar build como inestable si una colección falla
                            } finally {
                                archiveArtifacts artifacts: reportFile, allowEmptyArchive: true, fingerprint: true
                            }
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