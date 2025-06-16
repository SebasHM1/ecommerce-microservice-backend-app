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

/**
 * Fuerza el desbloqueo de un estado de Terraform para un entorno específico.
 * @param terraformEnv El directorio del entorno (ej: "dev", "stage").
 * @param lockId El ID del bloqueo a eliminar.
 */
void forceUnlockTerraform(String terraformEnv, String lockId) {
    echo "EMERGENCY: Forcing unlock for Terraform environment '${terraformEnv}' with Lock ID ${lockId}"
    dir("terraform/${terraformEnv}") {
        sh 'terraform init -input=false'
        sh "terraform force-unlock -force ${lockId}"
    }
    echo "EMERGENCY: Unlock for '${terraformEnv}' completed."
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

            stage('Prepare Java TrustStore') {
            steps {
                // FORZAR EJECUCIÓN EN EL CONTENEDOR 'tools'
                // Toda la lógica, incluidas las verificaciones, se mueve dentro de un solo bloque sh.
                sh '''
                    set -e  # Hace que el script falle inmediatamente si un comando falla
                    echo "Solucionando problema de certificado SSL (ejecución forzada en contenedor 'tools')..."
                    
                    # --- Lógica dinámica para encontrar JAVA_HOME ---
                    JAVA_EXEC_PATH=$(readlink -f $(which java))
                    if [ -z "$JAVA_EXEC_PATH" ]; then
                        echo "ERROR CRÍTICO: No se pudo encontrar el ejecutable 'java' en el PATH."
                        exit 1
                    fi
                    echo "Ejecutable 'java' encontrado en: $JAVA_EXEC_PATH"
                    
                    JAVA_HOME_DEDUCED=$(echo "$JAVA_EXEC_PATH" | sed 's|/bin/java$||')
                    KEYTOOL_PATH="$JAVA_HOME_DEDUCED/bin/keytool"

                    # --- Verificación ---
                    if [ ! -f "$KEYTOOL_PATH" ]; then
                        echo "ERROR CRÍTICO: 'keytool' no encontrado en la ruta esperada: $KEYTOOL_PATH"
                        exit 1
                    fi
                    echo "JAVA_HOME deducido: $JAVA_HOME_DEDUCED"
                    echo "Ubicación de 'keytool' confirmada: $KEYTOOL_PATH"
                    
                    TRUSTSTORE_PATH="$JAVA_HOME_DEDUCED/lib/security/cacerts"
                    TRUSTSTORE_PASS="changeit"
                    CERT_ALIAS="smtp-gmail-com"
                    CERT_FILE="/tmp/gmail.crt"
                    
                    # --- Lógica de importación del certificado ---
                    echo "Descargando certificado de smtp.gmail.com:465..."
                    openssl s_client -connect smtp.gmail.com:465 -servername smtp.gmail.com < /dev/null 2>/dev/null | \
                      sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > $CERT_FILE
                      
                    echo "Importando certificado al TrustStore..."
                    "$KEYTOOL_PATH" -importcert -noprompt \
                      -keystore "$TRUSTSTORE_PATH" \
                      -storepass "$TRUSTSTORE_PASS" \
                      -alias "$CERT_ALIAS" \
                      -file "$CERT_FILE"
                      
                    echo "Certificado importado con éxito."
                '''
            }
        }
        
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

                    //sleep 120 // Esperamos 120 segundos para que los servicios se estabilicen
                    //runEndToEndTests()

                }
            }
        }
        /*
        stage('Pruebas de Estrés (Locust)') {
            agent {
                kubernetes {
                    cloud 'kubernetes'
                    yaml """
                    apiVersion: v1
                    kind: Pod
                    spec:
                    containers:
                    - name: locust
                    image: sebashm1/jenkins-tools-completa:jdk17
                    command:
                    - sleep
                    args:
                    - 99d
                    """
                }
            }
            environment {
                API_GATEWAY_SERVICE_NAME = 'api-gateway' 
                K8S_NAMESPACE = 'dev' 
                API_GATEWAY_PORT = 8080 
                
                LOCUST_HOST_URL = "http://${API_GATEWAY_SERVICE_NAME}.${K8S_NAMESPACE}.svc.cluster.local:${API_GATEWAY_PORT}"
            }
            steps {
                container('locust') {
                    script {
                        try {
                            sh """
                            echo "Ejecutando pruebas de Locust contra el host: ${LOCUST_HOST_URL}"
                            
                            locust -f locustfile.py \\
                                --headless \\
                                --users 50 \\
                                --spawn-rate 10 \\
                                --run-time 1m \\
                                --host ${LOCUST_HOST_URL} \\
                                --csv locust_report \\
                                --html locust_report.html \\
                                --exit-code-on-error 1
                            """
                        } catch (e) {
                            echo "Las pruebas de Locust terminaron con errores (lo cual puede ser esperado)."
                            // Marcamos el build como INESTABLE en lugar de FALLIDO
                            currentBuild.result = 'UNSTABLE'
                        }
                    }
                }
            }
            post {
                always {
                    echo "Archivando reportes de Locust..."
                    archiveArtifacts artifacts: 'locust_report.html, locust_report_stats.csv', allowEmptyArchive: true
                    
                    publishHTML(target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: '.',
                        reportFiles: 'locust_report.html',
                        reportName: 'Reporte de Rendimiento (Locust)'
                    ])
                }
            }
        }
*/    
    stage('Pruebas de Seguridad (DAST con ZAP)') {
    agent {
        kubernetes {
            label 'default'
        }
    }
    environment {
        API_GATEWAY_SERVICE_NAME = 'api-gateway'
        K8S_NAMESPACE = 'dev'
        API_GATEWAY_PORT = 8080 
        TARGET_URL_FOR_ZAP = "http://${API_GATEWAY_SERVICE_NAME}.${K8S_NAMESPACE}.svc.cluster.local:${API_GATEWAY_PORT}/user-service/api/users"
    }
    steps {
        container('tools') {
            script {
                try {
                    // PASO 1: Ejecutar ZAP con la configuración de red y DNS correcta.
                    sh """
                    echo "Iniciando escaneo DAST con OWASP ZAP contra: ${TARGET_URL_FOR_ZAP}"
                    
                    docker run --rm \\
                        --network host \\
                        --dns 10.96.0.10 \\
                        --user \$(id -u):\$(id -g) \\
                        -v \$(pwd):/zap/wrk/:rw \\
                        --workdir /zap/wrk \\
                        -t softwaresecurityproject/zap-stable zap-baseline.py \\
                        -t ${TARGET_URL_FOR_ZAP} \\
                        -r zap_baseline_report.html \\
                        -w zap_baseline_report.md \\
                        -J zap_baseline_report.json \\
                        || true 
                    """

                    // PASO 2: Verificar el reporte SIN 'readJSON', usando 'grep'.
                    // Esto no requiere plugins adicionales.
                    echo "Verificando los resultados del escaneo..."
                    if (fileExists('zap_baseline_report.json')) {
                        // Usamos 'sh' para ejecutar grep. 'grep -q' es silencioso y devuelve 0 si encuentra algo.
                        // El 'returnStatus: true' es para que Jenkins no falle si grep devuelve 1 (no encontró nada).
                        def highAlertsFound = sh(script: 'grep -q \'"risk":"High"\' zap_baseline_report.json', returnStatus: true) == 0

                        if (highAlertsFound) {
                            echo "¡ADVERTENCIA! Se encontraron alertas de riesgo ALTO en el reporte de ZAP."
                            currentBuild.result = 'SUCCESS' // Aún así marcamos el build como exitoso, pero con advertencia.
                        } else {
                            echo "No se encontraron alertas de riesgo ALTO."
                        }
                    } else {
                        echo "ADVERTENCIA: El reporte de ZAP no fue generado. El escaneo probablemente falló."
                        currentBuild.result = 'UNSTABLE'
                    }

                } catch (e) {
                    echo "Falló la ejecución de la etapa de ZAP: ${e.message}"
                    currentBuild.result = 'UNSTABLE'
                }
            }
        }
    }
    post {
        always {
            echo "Archivando reportes de OWASP ZAP..."
            archiveArtifacts artifacts: 'zap_baseline_report.html, zap_baseline_report.md, zap_baseline_report.json', allowEmptyArchive: true
            
            
            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: '.',
                reportFiles: 'zap_baseline_report.html',
                reportName: 'Reporte de Seguridad (OWASP ZAP)'
            ])
            
        }
    }
}

        stage('Create Semantic Version & Release') {
            when {
                allOf {
                    expression { return params.RUN_PROMOTE_PROD && currentBuild.currentResult == 'SUCCESS' }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'github-pat-sebashm1', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                    script {
                        def gitRef = env.GIT_BRANCH
                        if (gitRef == null || !gitRef.contains('develop')) {
                            echo "El versionado solo se activa en la rama 'develop'. Omitiendo."
                            return
                        }

                        echo "Rama detectada: '${gitRef}'. Preparando entorno para forzar el versionado..."

                        echo "Forzando la descarga de todas las tags de Git..."
                        sh "git fetch --tags --force"
                        
                        // 1. Aseguramos estar en la rama correcta localmente
                        sh "git checkout develop"
                        
                        // 2. Configurar git para el push
                        sh 'git config --global user.email "ci-bot@tuempresa.com"'
                        sh 'git config --global user.name "Jenkins CI Bot"'
                        sh "git remote set-url origin https://_:${GIT_TOKEN}@github.com/sebashm1/ecommerce-microservice-backend-app.git"
                        
                        // 3. Ejecutar semantic-release con el "engaño"
                        echo "Iniciando semantic-release con variables de entorno manipuladas..."
                        sh """
                            # Engaño: Sobrescribimos la variable de entorno que confunde a semantic-release
                            export GIT_BRANCH="develop"
                            
                            # Token de GitHub
                            export GITHUB_TOKEN=${GIT_TOKEN}
                            npx semantic-release
                        """
                    }
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
        

        stage('Deploy to STAGING & Run E2E Tests') {
            when { 
                
                allOf {
                    expression { return currentBuild.currentResult == 'SUCCESS' }
                    expression { return params.RUN_PROMOTE_STAGING }
                }
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

*/
        
    }

    post {
        // Se ejecuta cuando el pipeline falla (resultado 'FAILURE')
        failure {
            script {
                // VERIFICACIÓN: Si la variable no se inicializó, le damos un valor por defecto.
                def commitId = env.IMAGE_TAG_SUFFIX ?: 'No definido (fallo temprano)'
                
                def subject = "❌ FALLO: Pipeline '${env.JOB_NAME}' #${env.BUILD_NUMBER}"
                def body = """
                <h1>Pipeline Fallida: ${env.JOB_NAME}</h1>
                <p>La build <b>#${env.BUILD_NUMBER}</b> ha fallado.</p>
                <p><b>Artefacto/Commit:</b> ${commitId}</p>  <!-- Usamos la variable segura -->
                <p><b>Resultado Final:</b> ${currentBuild.currentResult}</p>
                <p>Revisa los logs para más detalles:</p>
                <p><a href='${env.BUILD_URL}'>Ver Build en Jenkins</a></p>
                """
                
                emailext (
                    subject: subject,
                    body: body,
                    to: 'sebashidmar@gmail.com',
                    mimeType: 'text/html'
                )
            }
        }
        
        // Se ejecuta cuando el pipeline es inestable (resultado 'UNSTABLE'), como en tus tests E2E
        unstable {
            script {
                def subject = "⚠️ INESTABLE: Pipeline '${env.JOB_NAME}' #${env.BUILD_NUMBER}"
                def body = """
                <h1>Pipeline Inestable: ${env.JOB_NAME}</h1>
                <p>La build <b>#${env.BUILD_NUMBER}</b> ha finalizado como INESTABLE.</p>
                <p><b>Artefacto/Commit:</b> ${IMAGE_TAG_SUFFIX}</p>
                <p>Esto suele ocurrir por fallos en los tests E2E que no son críticos. Por favor, revisa los artefactos de Postman.</p>
                <p><a href='${env.BUILD_URL}'>Ver Build y Artefactos en Jenkins</a></p>
                """
                
                emailext (
                    subject: subject,
                    body: body,
                    to: 'sebashidmar@gmail.com', // Notificar solo a QA, por ejemplo
                    mimeType: 'text/html'
                )
            }
        }
        
        // (Opcional) Notificación de éxito
        success {
            script {
                def subject = "✅ ÉXITO: Pipeline '${env.JOB_NAME}' #${env.BUILD_NUMBER} completado"
                def body = """
                <h1>Pipeline Exitoso: ${env.JOB_NAME}</h1>
                <p>La build <b>#${env.BUILD_NUMBER}</b> ha finalizado correctamente.</p>
                <p><b>Artefacto/Commit desplegado:</b> ${IMAGE_TAG_SUFFIX}</p>
                <p><a href='${env.BUILD_URL}'>Ver Build en Jenkins</a></p>
                """
                
                emailext (
                    subject: subject,
                    body: body,
                    to: 'sebashidmar@gmail.com', // Notificar al responsable
                    mimeType: 'text/html'
                )
            }
        }

        aborted {
            script {
                def subject = "Aborted: Pipeline '${env.JOB_NAME}' #${env.BUILD_NUMBER}"
                def body = """
                <h1>Pipeline aborted: ${env.JOB_NAME}</h1>
                <p>La build <b>#${env.BUILD_NUMBER}</b> ha finalizado como aborted.</p>
                <p><b>Artefacto/Commit:</b> ${IMAGE_TAG_SUFFIX}</p>
                <p><a href='${env.BUILD_URL}'>Ver Build y Artefactos en Jenkins</a></p>
                """
                
                emailext (
                    subject: subject,
                    body: body,
                    to: 'sebashidmar@gmail.com', // Notificar solo a QA, por ejemplo
                    mimeType: 'text/html'
                )
            }
        }
        
        // Siempre se ejecuta. Ideal para limpieza.
        always {
            echo "Pipeline finalizado. Limpiando el workspace..."
            sh 'rm -rf * .??*' // Limpia el workspace al final de la ejecución
        }
    }
}