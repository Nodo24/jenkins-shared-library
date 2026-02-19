#!/usr/bin/env groovy

def call(Map config) {
    if (env.BRANCH_NAME == 'main') {
        env.APP_PORT = "3000"
        env.IMAGE_TAG = "main-${config.version}"
        env.DEPLOY_JOB = "Deploy_to_main"
    } else if (env.BRANCH_NAME == 'dev') {
        env.APP_PORT = "3001"
        env.IMAGE_TAG = "dev-${config.version}"
        env.DEPLOY_JOB = "Deploy_to_dev"
    }
    env.FULL_IMAGE = "${config.dockerUser}/${config.repoName}:${env.IMAGE_TAG}"
    
 
    sh "chmod +x scripts/build.sh && ./scripts/build.sh"
    sh "chmod +x scripts/test.sh && ./scripts/test.sh"
    
    sh "docker run --rm -i hadolint/hadolint < Dockerfile"
    sh "docker build -t ${env.FULL_IMAGE} ."
    sh "trivy image --exit-code 1 --severity HIGH,CRITICAL --skip-dirs "/usr/local" --skip-dirs "./npm/_cacache/*" ${env.FULL_IMAGE}"
    

    withCredentials([usernamePassword(
        credentialsId: config.credentialsId,
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS'
    )]) {
        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
    }
    sh "docker push ${env.FULL_IMAGE}"

    build job: env.DEPLOY_JOB, 
          wait: false, 
          parameters: [
              string(name: 'IMAGE_TAG', value: env.IMAGE_TAG),
              string(name: 'APP_PORT', value: env.APP_PORT)
          ]
}
