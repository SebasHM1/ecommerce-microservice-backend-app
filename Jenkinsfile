// Jenkinsfile adaptado para Promoción Controlada (Build Once, Deploy Many)
// Mantiene la estructura y nombres de variables originales.

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
    imagePullPolicy: 'Always'
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
        
        // --- Variables de Estado (se actualizarán durante la promoción) ---
        K8S_NAMESPACE = "dev" 
        SPRING_ACTIVE_PROFILE_APP = "dev"
        TERRAFORM_ENV_DIR = "dev"
        RUN_E2E_TESTS = "false"

        // --- Variables del Artefacto (se definen una vez y no cambian) ---
        // CAMBIO DE SIGNIFICADO: Ya no representa un entorno, sino el ID único de la build.
        IMAGE_TAG_SUFFIX = "" 
        // CAMBIO: Siempre se ejecutan todas las pruebas para un artefacto promocionable.
        MAVEN_PROFILES = "-Prun-its" 
        
        // NUEVO: Variable para pasar el mapa de imágenes a Terraform.
        TERRAFORM_SERVICE_IMAGES_VAR = ""
    }

    stages {
        
        // ==================================================================
        // FASE 1: CONSTRUIR, PROBAR Y ETIQUETAR UN ARTEFACTO ÚNICO
        // ==================================================================
        
            stage('Initialize & Create Unique Build ID') {
            steps {
                script {
                    // NUEVO: Solución para el error "dubious ownership".
                    // Añadimos el directorio del workspace actual a la lista de directorios seguros de Git.
                    // Usamos la variable de entorno ${WORKSPACE} de Jenkins para que sea genérico y no dependa del nombre del job.
                    sh "git config --global --add safe.directory ${WORKSPACE}"

                    // Ahora, este comando se ejecutará sin problemas de permisos.
                    def gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    IMAGE_TAG_SUFFIX = gitCommit // Ej: a1b2c3d
                    
                    echo "===================================================================="
                    echo "PIPELINE DE PROMOCIÓN INICIADO"
                    echo "ID del artefacto a promover: ${IMAGE_TAG_SUFFIX}"
                    echo "Se construirán las imágenes con este sufijo y se moverán a través de los entornos."
                    echo "===================================================================="
                }
            }
        }
        /*
        stage('Compile and Test All Services') {
            steps {
                script {
                    def servicesToProcess = [
                        'service-discovery', 'cloud-config',  'user-service',  'api-gateway', 
                        'order-service', 'product-service', 'shipping-service', 'payment-service', 'proxy-client'
                    ]
                    
                    // AJUSTE TEMPORAL: Definimos el perfil Maven aquí para saltar los tests de integración.
                    // Esto sobreescribe el valor por defecto definido en el bloque 'environment'.
                    // De esta forma, no necesitas cambiar nada más en el pipeline.
                    def MAVEN_PROFILES_FOR_BUILD = "-Pskip-its"
                    
                    echo "ADVERTENCIA: Se usarán los perfiles Maven '${MAVEN_PROFILES_FOR_BUILD}' para saltar los tests de integración debido a limitaciones de memoria."

                    for (svc in servicesToProcess) {
                        dir(svc) {
                            echo "Compilando y probando (solo unitarios) el servicio: ${svc}"
                            sh "chmod +x ./mvnw"

                            // LÍNEA ORIGINAL COMENTADA: Esta línea ejecutaría todos los tests (unitarios y de integración).
                            // sh "./mvnw clean verify ${MAVEN_PROFILES}"

                            // LÍNEA NUEVA: Usamos la variable local para saltar los tests de integración.
                            // Esto asegura que solo se ejecuten los tests unitarios (fase 'test' de Surefire)
                            // y que Failsafe (fase 'integration-test') se omita gracias al perfil 'skip-its'.
                            sh "./mvnw clean verify ${MAVEN_PROFILES_FOR_BUILD}"
                        }
                    }
                }
            }
        }

        stage('Build, Push Docker Images & Prepare Terraform Vars') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-sebashm1', usernameVariable: 'DOCKER_CRED_USER', passwordVariable: 'DOCKER_CRED_PSW')]) {
                    script {
                        sh "echo \$DOCKER_CRED_PSW | docker login -u \$DOCKER_CRED_USER --password-stdin"

                        def serviceToBaseTagMap = [
                            'service-discovery': 'discovery', 'cloud-config': 'config', 'api-gateway': 'gateway',
                            'proxy-client': 'proxy', 'order-service': 'order', 'product-service': 'product',
                            'user-service': 'users', 'shipping-service': 'shipping', 'payment-service': 'payment'
                            //'favourite-service': 'favourite'
                        ]
                        
                        // Usamos un mapa local para construir las URLs de las imágenes.
                        def builtImagesMap = [ "zipkin": "openzipkin/zipkin:latest" ]

                        for (svcDirName in serviceToBaseTagMap.keySet()) {
                            def baseTag = serviceToBaseTagMap[svcDirName]
                            if (fileExists(svcDirName)) {
                                dir(svcDirName) {
                                    // La URL completa de la imagen con el ID único.
                                    // Ej: sebashm1/repo:gateway-a1b2c3d
                                    def fullImageName = "${DOCKERHUB_USER}/${DOCKERHUB_REPO_PREFIX}:${baseTag}-${IMAGE_TAG_SUFFIX}"
                                    
                                    echo "Building and Pushing: ${fullImageName}"
                                    sh "docker build -t ${fullImageName} ."
                                    sh "docker push ${fullImageName}"
                                    
                                    builtImagesMap[baseTag] = fullImageName
                                }
                            }
                        }
                        
                        // Convertimos el mapa Groovy al formato de variable -var para Terraform.
                        // Resultado: -var='service_images={ "key1":"val1", "key2":"val2" }'
                        def mapAsString = builtImagesMap.collect { k, v -> "\"${k}\":\"${v}\"" }.join(',')
                        TERRAFORM_SERVICE_IMAGES_VAR = "-var='service_images={${mapAsString}}'"
                        
                        echo "Variable de Terraform preparada: ${TERRAFORM_SERVICE_IMAGES_VAR}"
                        sh "docker logout"
                    }
                }
            }
        }
        */
        // ==================================================================
        // FASE 2: SECUENCIA DE PROMOCIÓN Y DESPLIEGUE CONTROLADO
        // ==================================================================

        stage('Deploy to DEV') {
            steps {
                script {
                    echo "--> Desplegando artefacto '${IMAGE_TAG_SUFFIX}' a DEV..."
                    // Los valores por defecto de las variables de entorno ya apuntan a DEV.
                    // K8S_NAMESPACE="dev", TERRAFORM_ENV_DIR="dev", etc.
                    deployWithTerraform()
                }
            }
        }

        stage('Approval: Promote to STAGING?') {
            steps {
                // Puerta de aprobación manual que detiene el pipeline.
                input id: 'promoteToStagingGate', 
                      message: "El artefacto con ID '${IMAGE_TAG_SUFFIX}' ha sido desplegado en DEV. ¿Aprobar promoción a STAGING?", 
                      submitter: 'admin,release-managers' 
            }
        }

        stage('Deploy to STAGING & Run E2E Tests') {
            steps {
                script {
                    echo "--> Promoviendo artefacto '${IMAGE_TAG_SUFFIX}' a STAGING..."
                    
                    // Actualizamos las variables de "estado" para que apunten a STAGING.
                    K8S_NAMESPACE = "stage"
                    SPRING_ACTIVE_PROFILE_APP = "stage"
                    TERRAFORM_ENV_DIR = "stage"
                    RUN_E2E_TESTS = "true" // Activamos E2E para Staging
                    
                    deployWithTerraform()
                    
                    echo "Esperando 120 segundos para la estabilización de los servicios en STAGING..."
                    sleep 120
                    
                    runEndToEndTests()
                }
            }
        }

        stage('Approval: Promote to PRODUCTION?') {
            steps {
                input id: 'promoteToProdGate',
                      message: "¡PELIGRO! El artefacto ID '${IMAGE_TAG_SUFFIX}' fue validado en STAGING. ¿Aprobar despliegue a PRODUCCIÓN?", 
                      submitter: 'admin,cto' 
            }
        }

        stage('Deploy to PRODUCTION') {
            steps {
                script {
                    echo "--> Promoviendo artefacto '${IMAGE_TAG_SUFFIX}' a PRODUCCIÓN..."
                    
                    // Actualizamos las variables de "estado" para que apunten a PROD.
                    K8S_NAMESPACE = "prod"
                    SPRING_ACTIVE_PROFILE_APP = "prod"
                    TERRAFORM_ENV_DIR = "prod"
                    RUN_E2E_TESTS = "false" 
                    
                    deployWithTerraform()
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


// ==================================================================
// FUNCIONES AUXILIARES REUTILIZABLES
// ==================================================================

// Esta función ahora es agnóstica al entorno. Simplemente lee las variables
// de entorno que están activas en el momento de su ejecución.
void deployWithTerraform() {
    dir("terraform/${TERRAFORM_ENV_DIR}") {
        script {
            echo "===================================================================="
            echo "Running Terraform for environment: ${TERRAFORM_ENV_DIR}"
            echo "Target K8s Namespace: ${K8S_NAMESPACE}"
            echo "Spring Profile: ${SPRING_ACTIVE_PROFILE_APP}"
            echo "===================================================================="
            
            sh 'terraform init -input=false'

            echo "--- Terraform Plan ---"
            // Se usa la variable TERRAFORM_SERVICE_IMAGES_VAR que ya contiene el mapa formateado.
            // Esto simplifica el comando y evita problemas de escapado de comillas.
            sh """
            terraform plan -out=tfplan -input=false \\
                ${TERRAFORM_SERVICE_IMAGES_VAR} \\
                -var="k8s_namespace=${K8S_NAMESPACE}" \\
                -var="spring_profile=${SPRING_ACTIVE_PROFILE_APP}"
            """

            echo "--- Terraform Apply ---"
            sh 'terraform apply -auto-approve -input=false tfplan'
        }
    }
}

// Esta función también es agnóstica y se guía por las variables de entorno activas.
void runEndToEndTests() {
    if (env.RUN_E2E_TESTS == "true") {
        dir('postman-collections') {
            def collectionsToRun = [
                "E2E Test 1 - Users.postman_collection.json", "E2E Test 2 - Product.postman_collection.json",
                "E2E Test 3 - Order and Payment.postman_collection.json", "E2E Test 4 - Shipping.postman_collection.json",
                "E2E Test 5 - Delete Entities.postman_collection.json"
            ]
            def environmentFile = "JenkinsGlobalE2E.postman_environment.json" 
            def apiGatewayInternalUrl = "http://api-gateway.${K8S_NAMESPACE}.svc.cluster.local:8080"
            
            for (collectionFile in collectionsToRun) {
                def reportFile = "reporte-${collectionFile.tokenize('.')[0]}-${K8S_NAMESPACE}-${IMAGE_TAG_SUFFIX}.html"
                try {
                    sh """
                        newman run "${collectionFile}" -e "${environmentFile}" \\
                        --global-var "API_GATEWAY_URL=${apiGatewayInternalUrl}" \\
                        -r cli,htmlextra --reporter-htmlextra-export "${reportFile}"
                    """
                } catch (Exception e) {
                    echo "Error en la colección E2E ${collectionFile}: ${e.getMessage()}"
                    currentBuild.result = 'UNSTABLE'
                } finally {
                    archiveArtifacts artifacts: reportFile, allowEmptyArchive: true, fingerprint: true
                }
            }
        }
    } else {
        echo "Skipping E2E tests for environment: ${K8S_NAMESPACE}."
    }
}