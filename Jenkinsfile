// ==================================================================
// CONFIGURACIÓN GLOBAL Y MAPAS
// ==================================================================
def SONAR_IS_AVAILABLE = false
//def builtImagesMap = [:]

// NUEVO: Mapa de servicios movido aquí para ser accesible globalmente.
// Esto es clave para poder reconstruir las variables si nos saltamos la fase de build.
def serviceToBaseTagMap = [
    'service-discovery': 'discovery', 'cloud-config': 'config', 'api-gateway': 'gateway',
    'proxy-client': 'proxy', 'order-service': 'order', 'product-service': 'product',
    'user-service': 'users', 'shipping-service': 'shipping', 'payment-service': 'payment'
]

// ==================================================================
// FUNCIONES AUXILIARES REUTILIZABLES (Sin cambios)
// ==================================================================

/**
 * Comprueba si el servidor SonarQube está disponible.
 * @return true si SonarQube está activo, false en caso contrario.
 */
def isSonarQubeAvailable() {
    try {
        sh(script: "curl --connect-timeout 5 --silent --fail ${env.SONAR_URL}/api/system/status > /dev/null", returnStatus: false)
        echo "✅ SonarQube está disponible en ${env.SONAR_URL}. Se procederá con el análisis."
        return true
    } catch (Exception e) {
        echo "⚠️ ADVERTENCIA: No se pudo contactar con el servidor SonarQube en ${env.SONAR_URL}."
        echo "Se omitirán las etapas de análisis de código."
        return false
    }
}

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
    
    // NUEVO: Parámetros para controlar la ejecución del pipeline.
    parameters {
        booleanParam(name: 'RUN_BUILD_AND_ANALYZE', defaultValue: true, description: 'Ejecutar fases de compilación, tests unitarios y análisis de SonarQube.')
        booleanParam(name: 'RUN_PACKAGE_AND_SCAN', defaultValue: true, description: 'Ejecutar fases para construir, subir y escanear imágenes Docker con Trivy.')
        booleanParam(name: 'RUN_DEPLOY_DEV', defaultValue: true, description: 'Ejecutar despliegue en el entorno de DEV.')
        booleanParam(name: 'RUN_PROMOTE_STAGING', defaultValue: true, description: 'Ejecutar promoción y despliegue en el entorno de STAGING (incluye tests E2E).')
        booleanParam(name: 'RUN_PROMOTE_PROD', defaultValue: true, description: 'Ejecutar promoción y despliegue en el entorno de PRODUCCIÓN.')
        string(name: 'EXISTING_BUILD_ID', defaultValue: '', description: 'Opcional: Si omites la fase de build/package, proporciona aquí el ID (ej: a1b2c3d) de una build anterior para desplegarla.')
    }
    
    environment {
        DOCKERHUB_USER = 'sebashm1'
        DOCKERHUB_REPO_PREFIX = 'ecommerce-microservice-backend-app' 
        K8S_NAMESPACE = "dev" 
        SPRING_ACTIVE_PROFILE_APP = "dev"
        TERRAFORM_ENV_DIR = "dev"
        RUN_E2E_TESTS = "false"
        IMAGE_TAG_SUFFIX = "" 
        MAVEN_PROFILES = "-Prun-its" 
        //TERRAFORM_SERVICE_IMAGES_VAR = ""
        SONAR_SERVER_NAME = "MiSonarQubeLocal"
        SONAR_URL = 'http://host.minikube.internal:9000' 
        TRIVY_SEVERITY = 'CRITICAL,HIGH'
    }

    stages {
        
        // ==================================================================
        // FASE 1: CONSTRUIR, PROBAR Y ETIQUETAR UN ARTEFACTO ÚNICO
        // ==================================================================
        
        stage('Initialize & Configure Build') { // MODIFICADO: Nombre más genérico
            steps {
                script {
                    sh "git config --global --add safe.directory ${WORKSPACE}"
                    
                    // MODIFICADO: Lógica para usar un build nuevo o uno existente
                    if (params.RUN_BUILD_AND_ANALYZE || params.RUN_PACKAGE_AND_SCAN) {
                        echo "Modo: NUEVA BUILD. Se generará un nuevo ID de artefacto."
                        def gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        IMAGE_TAG_SUFFIX = gitCommit
                    } else {
                        echo "Modo: RE-DESPLIEGUE. Se usará un ID de artefacto existente."
                        if (params.EXISTING_BUILD_ID.trim().isEmpty()) {
                            error("ERROR: Has omitido las fases de construcción, pero no has proporcionado un 'EXISTING_BUILD_ID'. El pipeline no sabe qué desplegar.")
                        }
                        IMAGE_TAG_SUFFIX = params.EXISTING_BUILD_ID.trim()
                    }
                    
                    echo "===================================================================="
                    echo "ID del artefacto para esta ejecución: ${IMAGE_TAG_SUFFIX}"
                    echo "===================================================================="
                    
                    // La comprobación de SonarQube se hace siempre para tener la variable lista
                    SONAR_IS_AVAILABLE = isSonarQubeAvailable()
                }
            }
        }
        
        stage('Compile and Test All Services') {
            // NUEVO: Condición de ejecución
            when { expression { return params.RUN_BUILD_AND_ANALYZE } }
            steps {
                script {
                    def servicesToProcess = serviceToBaseTagMap.keySet()
                    def MAVEN_PROFILES_FOR_BUILD = "-Pskip-its"
                    echo "ADVERTENCIA: Se usarán los perfiles Maven '${MAVEN_PROFILES_FOR_BUILD}' para saltar los tests de integración."
                    
                    for (svc in servicesToProcess) {
                        dir(svc) {
                            echo "Compilando y probando (solo unitarios) el servicio: ${svc}"
                            sh "chmod +x ./mvnw"
                            sh "./mvnw clean verify ${MAVEN_PROFILES_FOR_BUILD}"
                        }
                    }
                }
            }
        }

        stage('SonarQube Static Analysis') {
            // MODIFICADO: Combinamos la condición del parámetro con la de la disponibilidad de Sonar
            when {
                allOf {
                    expression { return params.RUN_BUILD_AND_ANALYZE }
                    expression { return SONAR_IS_AVAILABLE }
                }
            }
            steps {
                withSonarQubeEnv(SONAR_SERVER_NAME) {
                    script {
                        for (svc in serviceToBaseTagMap.keySet()) {
                            dir(svc) {
                                echo "Analizando con SonarQube el servicio: ${svc}"
                                sh """
                                   ./mvnw sonar:sonar \
                                     -Dsonar.projectKey=${DOCKERHUB_REPO_PREFIX}_${svc} \
                                     -Dsonar.projectName="[Microservice] ${svc}" \
                                     -Dsonar.java.binaries=target/classes
                                """
                            }
                        }
                    }
                }
            }
        }
        
        stage('Check SonarQube Quality Gate') {
            // MODIFICADO: Combinamos la condición del parámetro con la de la disponibilidad de Sonar
            when {
                allOf {
                    expression { return params.RUN_BUILD_AND_ANALYZE }
                    expression { return SONAR_IS_AVAILABLE }
                }
            }
            steps {
                script {
                    timeout(time: 10, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }

        stage('Build, Push & Stash Deployment Info') { 
            when { expression { return params.RUN_PACKAGE_AND_SCAN } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-sebashm1', usernameVariable: 'DOCKER_CRED_USER', passwordVariable: 'DOCKER_CRED_PSW')]) {
                    script {
                        sh "echo \$DOCKER_CRED_PSW | docker login -u \$DOCKER_CRED_USER --password-stdin"
                        
                        def localBuiltImagesMap = [ "zipkin": "openzipkin/zipkin:latest" ]

                        for (svcDirName in serviceToBaseTagMap.keySet()) {
                            def baseTag = serviceToBaseTagMap[svcDirName]
                            if (fileExists(svcDirName)) {
                                dir(svcDirName) {
                                    def fullImageName = "${DOCKERHUB_USER}/${DOCKERHUB_REPO_PREFIX}:${baseTag}-${IMAGE_TAG_SUFFIX}"
                                    sh "docker build -t ${fullImageName} ."
                                    sh "docker push ${fullImageName}"
                                    localBuiltImagesMap[baseTag] = fullImageName
                                }
                            }
                        }
                        
                        def mapAsString = localBuiltImagesMap.collect { k, v -> "\"${k}\":\"${v}\"" }.join(',')
                        def terraformVarString = "-var='service_images={${mapAsString}}'"
                        
                        // CORRECCIÓN: Escribimos la variable a un archivo y la guardamos en el stash
                        writeFile file: 'deployment_vars.txt', text: terraformVarString
                        stash name: 'deployment-info', includes: 'deployment_vars.txt'
                        
                        echo "Información de despliegue guardada en el stash."
                    }
                }
            }
        }

        stage('Scan Container Images with Trivy (Advisory)') {
            when { expression { return params.RUN_PACKAGE_AND_SCAN } }
            steps {
                script {
                    // CORRECCIÓN: Recuperamos la información del stash
                    unstash 'deployment-info'
                    def terraformVarString = readFile('deployment_vars.txt').trim()
                    
                    def imagesToScan = []
                    terraformVarString.eachMatch(/"([^"]+)":"([^"]+)"/) { match ->
                        if (match[1] != 'zipkin') {
                            imagesToScan.add([serviceName: match[1], fullImageName: match[2]])
                        }
                    }

                    for (def imageInfo in imagesToScan) {
                        def reportFile = "trivy-report-${imageInfo.serviceName}-${IMAGE_TAG_SUFFIX}.html"
                        // ... el resto del try/catch/finally no cambia ...
                        try {
                            sh """
                                trivy image --format template --template "@/root/trivy-templates/html.tpl" \
                                -o ${reportFile} --severity ${TRIVY_SEVERITY} --no-progress ${imageInfo.fullImageName}
                            """
                        } catch (Exception e) {
                            echo "❌ Error al ejecutar Trivy para la imagen ${imageInfo.fullImageName}: ${e.getMessage()}"
                        } finally {
                            archiveArtifacts artifacts: reportFile, allowEmptyArchive: true
                        }
                    }
                    sh "docker logout"
                }
            }
        }
        
        stage('Prepare Deployment Variables (if skipping build)') {
            when {
                allOf {
                    expression { return !params.RUN_PACKAGE_AND_SCAN }
                    anyOf { 
                        
                        expression { return params.RUN_DEPLOY_DEV }
                        expression { return params.RUN_PROMOTE_STAGING }
                        expression { return params.RUN_PROMOTE_PROD }

                    }
                }
            }
            steps {
                script {
                    echo "Reconstruyendo información de despliegue para el artefacto existente: ${IMAGE_TAG_SUFFIX}"
                    
                    def localBuiltImagesMap = [ "zipkin": "openzipkin/zipkin:latest" ]
                    for (svcDirName in serviceToBaseTagMap.keySet()) {
                        def baseTag = serviceToBaseTagMap[svcDirName]
                        def fullImageName = "${DOCKERHUB_USER}/${DOCKERHUB_REPO_PREFIX}:${baseTag}-${IMAGE_TAG_SUFFIX}"
                        localBuiltImagesMap[baseTag] = fullImageName
                    }
                    def mapAsString = localBuiltImagesMap.collect { k, v -> "\"${k}\":\"${v}\"" }.join(',')
                    def terraformVarString = "-var='service_images={${mapAsString}}'"
                    
                    // CORRECCIÓN: También guardamos en el stash en este caso
                    writeFile file: 'deployment_vars.txt', text: terraformVarString
                    stash name: 'deployment-info', includes: 'deployment_vars.txt'
                    
                    echo "Información de despliegue reconstruida y guardada en el stash."
                }
            }
        }
        
        // ==================================================================
        // FASE 2: SECUENCIA DE PROMOCIÓN Y DESPLIEGUE CONTROLADO
        // ==================================================================

        stage('Deploy to DEV') {
            when { expression { return params.RUN_DEPLOY_DEV } }
            steps {
                script {
                    // CORRECCIÓN: Recuperamos la información del stash antes de usarla
                    unstash 'deployment-info'
                    // Hacemos la variable local para la función deployWithTerraform
                    env.TERRAFORM_SERVICE_IMAGES_VAR = readFile('deployment_vars.txt').trim()
                    
                    echo "--> Desplegando artefacto '${IMAGE_TAG_SUFFIX}' a DEV..."
                    deployWithTerraform()
                }
            }
        }

        /*
        stage('Approval: Promote to STAGING?') {
            // MODIFICADO: La aprobación solo tiene sentido si ambas fases (DEV y STAGING) están activas.
            when { 
                allOf {
                    expression { return params.RUN_DEPLOY_DEV }
                    expression { return params.RUN_PROMOTE_STAGING }
                }
            }
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    input id: 'promoteToStagingGate', 
                          message: "El artefacto con ID '${IMAGE_TAG_SUFFIX}' ha sido desplegado en DEV. ¿Aprobar promoción a STAGING?", 
                          submitter: 'admin,release-managers' 
                }
            }
        }
        */
        stage('Deploy to STAGING & Run E2E Tests') {
            when { 
                
                allOf {
                    expression { return currentBuild.currentResult == 'SUCCESS' }
                    expression { return params.RUN_PROMOTE_STAGING }
                }

            steps {
                script {
                    // ==========================================================
                    // CORRECCIÓN: Recuperar del stash antes de desplegar
                    // ==========================================================
                    unstash 'deployment-info'
                    env.TERRAFORM_SERVICE_IMAGES_VAR = readFile('deployment_vars.txt').trim()
                    
                    echo "--> Promoviendo artefacto '${IMAGE_TAG_SUFFIX}' a STAGING..."
                    K8S_NAMESPACE = "stage"
                    SPRING_ACTIVE_PROFILE_APP = "stage"
                    TERRAFORM_ENV_DIR = "stage"
                    RUN_E2E_TESTS = "true"

                    // Ahora deployWithTerraform() usará la variable de entorno correcta
                    deployWithTerraform()
                    
                    echo "Esperando 120 segundos para la estabilización de los servicios en STAGING..."
                    sleep 120
                    runEndToEndTests()
                }
            }
        }
        /*
        stage('Approval: Promote to PRODUCTION?') {
            when { 
                allOf {
                    expression { return params.RUN_PROMOTE_STAGING }
                    expression { return params.RUN_PROMOTE_PROD }
                }
            }
            steps {
                // Esta etapa de 'input' ahora funcionará correctamente porque el
                // estado del pipeline es "limpio" y serializable.
                timeout(time: 15, unit: 'MINUTES') {
                    input id: 'promoteToProdGate',
                          message: "¡PELIGRO! El artefacto ID '${IMAGE_TAG_SUFFIX}' fue validado en STAGING. ¿Aprobar despliegue a PRODUCCIÓN?", 
                          submitter: 'admin,cto' 
                }
            }
        }
*/
        stage('Deploy to PRODUCTION') {
            when { 
                allOf {
                    expression { return params.RUN_PROMOTE_STAGING } // Debe estar activado el flujo de Staging
                    expression { return params.RUN_PROMOTE_PROD }   // Debe estar activado el flujo de Prod
                    // Esta es la condición clave:
                    expression { return currentBuild.currentResult == 'SUCCESS' }
                }
            }
            steps {
                script {
                    // ==========================================================
                    // CORRECIÓN: Recuperar del stash antes de desplegar a PROD
                    // ==========================================================
                    unstash 'deployment-info'
                    env.TERRAFORM_SERVICE_IMAGES_VAR = readFile('deployment_vars.txt').trim()
                    
                    echo "--> Promoviendo artefacto '${IMAGE_TAG_SUFFIX}' a PRODUCCIÓN..."
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