#!groovy

def workerNode = "devel11"
def teamEmail = 'metascrum@dbc.dk'
def teamSlack = 'meta-notifications'

pipeline {
    agent {
        label workerNode
    }
    triggers {
        gitlab(triggerOnPush: true, triggerOnMergeRequest: true, branchFilterType: 'All')
    }
    options {
        timestamps()
        gitLabConnection('isworker')
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
                    def status = sh returnStatus: true, script:  """
                        rm -rf \$WORKSPACE/.repo
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo dependency:resolve dependency:resolve-plugins >/dev/null || true
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo clean
                    """

                    // We want code-coverage and pmd/spotbugs even if unittests fails
                    status += sh returnStatus: true, script:  """
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo verify pmd:pmd pmd:cpd spotbugs:spotbugs javadoc:aggregate
                    """

                    junit testResults: '**/target/*-reports/TEST-*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    def javadoc = scanForIssues tool: [$class: 'JavaDoc']
                    publishIssues issues:[java, javadoc], unstableTotalAll:1

                    def pmd = scanForIssues tool: [$class: 'Pmd'], pattern: '**/target/pmd.xml'
                    publishIssues issues:[pmd], unstableTotalAll:1

                    def spotbugs = scanForIssues tool: [$class: 'SpotBugs'], pattern: '**/target/spotbugsXml.xml'
                    publishIssues issues:[spotbugs], unstableTotalAll:1

                    status += sh returnStatus: true, script:  """
                          mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo -Pnative verify
                    """

                    archiveArtifacts artifacts: 'target/mconv', fingerprint: true

                    if (status != 0) {
                        error("build failed")
                    }
                }
            }
        }
        stage("build-npm") {
            agent {
                docker {
                    image 'docker.dbc.dk/dbc-node'
                    args '--user isworker'
                    alwaysPull true
                    reuseNode true
                }
            }
            steps {
                sh """
                    scripts/build-npm package.json target/mconv
                """
                archiveArtifacts artifacts: '**/target/*.tgz', fingerprint: true
            }

        }
        stage("deploy") {
            when {
                branch "master"
            }
            steps {
                sh "mvn verify deploy:deploy"
            }
        }
    }

    post {
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
            updateGitlabCommitStatus name: 'build', state: 'failed'
        }

        success {
            script {
                if (env.BRANCH_NAME == 'master' && currentBuild.getPreviousBuild() != null && currentBuild.getPreviousBuild().result == 'FAILURE') {
                    emailext(
                        recipientProviders: [developers(), culprits()],
                        to: teamEmail,
                        subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER} back to normal",
                        mimeType: 'text/html; charset=UTF-8',
                        body: "<p>The master build is back to normal.</p><p><a href=\"${env.BUILD_URL}\">Build information</a>.</p>",
                        attachLog: false)
                    slackSend(channel: teamSlack,
                        color: 'good',
                        message: "${env.JOB_NAME} #${env.BUILD_NUMBER} back to normal: ${env.BUILD_URL}",
                        tokenCredentialId: 'slack-global-integration-token')
                }
            }
            updateGitlabCommitStatus name: 'build', state: 'success'
        }
    }
}
