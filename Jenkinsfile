#!groovy

def workerNode = "devel11"
def teamEmail = 'de-team@dbc.dk'
def teamSlack = 'de-notifications'

pipeline {
    agent {
        label workerNode
    }
    options {
        timestamps()
    }
    tools {
        jdk 'jdk11'
        maven "Maven 3"
    }

    stages {
        stage("clear workspace") {
            steps {
                deleteDir()
                checkout scm
            }
        }

        stage("build") {
			steps {
                script {
                    def status = sh returnStatus: true, script: """
                        ./scripts/build
                        """

                    junit testResults: '**/target/*-reports/*.xml'

                    if (status != 0) {
                        error("build failed")
                    }
                }
            }
        }

        stage("deploy") {
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                        mvn -Dbuild_number=${BUILD_NUMBER} -Dmaven.repo.local=\$WORKSPACE/.repo -pl cli-native build-helper:attach-artifact deploy:deploy
                    """
                }
            }
        }
    }

    post {
        fixed {
            script {
                if (env.BRANCH_NAME == 'master') {
                    emailext(
                        recipientProviders: [developers(), culprits()],
                        to: teamEmail,
                        subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER} back to normal",
                        mimeType: 'text/html; charset=UTF-8',
                        body: "<p>The master branch is back to normal.</p><p><a href=\"${env.BUILD_URL}\">Build information</a>.</p>",
                        attachLog: false)
                    slackSend(channel: teamSlack,
                        color: 'good',
                        message: "${env.JOB_NAME} #${env.BUILD_NUMBER} back to normal: ${env.BUILD_URL}",
                        tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }

        failure {
            script {
                if (env.BRANCH_NAME == 'master') {
                    emailext(
                        recipientProviders: [developers(), culprits()],
                        to: teamEmail,
                        subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER} failed",
                        mimeType: 'text/html; charset=UTF-8',
                        body: "<p>The master build failed. Log attached.</p><p><a href=\"${env.BUILD_URL}\">Build information</a>.</p>",
                        attachLog: true
                    )
                    slackSend(channel: teamSlack,
                        color: 'warning',
                        message: "${env.JOB_NAME} #${env.BUILD_NUMBER} failed and needs attention: ${env.BUILD_URL}",
                        tokenCredentialId: 'slack-global-integration-token')

                } else {
                    emailext(
                        recipientProviders: [developers()],
                        subject: "[Jenkins] ${env.BUILD_TAG} failed and needs your attention",
                        mimeType: 'text/html; charset=UTF-8',
                        body: "<p>${env.BUILD_TAG} failed and needs your attention. </p><p><a href=\"${env.BUILD_URL}\">Build information</a>.</p>",
                        attachLog: false
                    )
                }
            }
        }

        success {
            archiveArtifacts artifacts: 'cli-native/target/mconv,cli-native/target/npm-dist/*.tgz', fingerprint: true
        }
    }
}
