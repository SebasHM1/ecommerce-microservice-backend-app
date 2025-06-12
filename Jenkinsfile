pipeline {
    agent {
        kubernetes {
            defaultContainer 'jnlp' // Usa el contenedor más simple posible
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jnlp
    image: jenkins/inbound-agent:jdk17 
    command: ['sleep']
    args: ['infinity']
'''
        }
    }
    stages {
        stage('Step 1') {
            steps {
                echo "Llegando a la aprobación..."
            }
        }
        stage('Approval') {
            steps {
                script {
                    echo "Esperando la aprobación del usuario."
                    timeout(time: 5, unit: 'MINUTES') {
                        input message: '¿Funciona este botón de Proceed?', submitter: 'admin'
                    }
                    echo "¡Aprobación recibida!"
                }
            }
        }
        stage('Step 2') {
            steps {
                echo "El pipeline continuó después de la aprobación."
            }
        }
    }
}