#!/usr/bin/env groovy

def call(Map config) {
    withCredentials([usernamePassword(
        credentialsId: config.credentialsId,
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS'
    )]) {
        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
    }
    

    sh "docker pull ${config.imageName}"
    
    sh """
        docker stop ${config.containerName} 2>/dev/null || true
        docker rm ${config.containerName} 2>/dev/null || true
    """
    

    sh """
        docker run -d \
          -p ${config.port}:3000 \
          --name ${config.containerName} \
          -e HOST=0.0.0.0 \
          ${config.imageName}
    """
}
