pipeline {

    agent any

    options {
        timestamps()
        skipDefaultCheckout(true)
        skipStagesAfterUnstable()
    }

    environment {
        MAVEN_OPTS = '-Xmx2g'
        JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
        MAVEN_LOCAL_REPO = '/var/jenkins_home/.m2/repository'
    }

    stages {

        stage('Checkout') {
            agent {
                docker {
                    image 'alpine:3.21'
                    reuseNode true
                }
            }

            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            agent {
                docker {
                    image 'eclipse-temurin:25-jdk'
                    reuseNode true
                    args '-u root -v /var/jenkins_home/.m2:/var/jenkins_home/.m2'
                }
            }

            steps {
                sh '''
                    set -e

                    echo "=== Java Runtime ==="
                    java -version

                    chmod +x mvnw
                    mkdir -p "$MAVEN_LOCAL_REPO"

                    if [ ! -f .env ]; then
                        cp .env.example .env
                    fi

                    echo "=== Maven Clean Verify (Jacoco 100%) ==="
                    ./mvnw -B clean verify -Pproduction \
                        -Dmaven.repo.local="$MAVEN_LOCAL_REPO"

                    echo "=== Build & Tests Passed ==="
                    ls -lh target/*.jar || true
                    ls -lh target/site/jacoco/jacoco.xml || true
                '''
            }

            post {
                always {
                    junit(
                        allowEmptyResults: true,
                        testResults: 'target/surefire-reports/*.xml'
                    )
                    archiveArtifacts(
                        artifacts: 'target/site/jacoco/**',
                        allowEmptyArchive: true
                    )
                }
            }
        }

        stage('Trivy Security Scan') {
            agent {
                docker {
                    image 'aquasec/trivy:0.76.1'
                    reuseNode true

                    args '''
                        --entrypoint=""
                        -e HOME=/tmp
                        -e XDG_CACHE_HOME=/tmp/.cache
                        -v /var/jenkins_home/.m2:/var/jenkins_home/.m2
                    '''
                }
            }

            steps {
                sh '''
                    set -e

                    mkdir -p .trivy-cache || true
                    mkdir -p /tmp/.m2
                    ln -sfn "$MAVEN_LOCAL_REPO" /tmp/.m2/repository

                    trivy --version

                    trivy fs \
                        --cache-dir .trivy-cache \
                        --scanners vuln \
                        --severity HIGH,CRITICAL \
                        --offline-scan \
                        --skip-dirs target \
                        --skip-dirs .mvn \
                        --skip-dirs node_modules \
                        --skip-dirs .trivy-cache \
                        --format sarif \
                        --output trivy-results.sarif \
                        --exit-code 1 \
                        .

                    ls -lh trivy-results.sarif
                '''
            }

            post {
                always {
                    recordIssues(
                        enabledForFailure: true,
                        failOnError: false,
                        tools: [
                            sarif(
                                id: 'trivy',
                                name: 'Trivy Security',
                                pattern: 'trivy-results.sarif'
                            )
                        ]
                    )
                }
            }
        }

        stage('SonarQube Analysis') {
            agent {
                docker {
                    image 'sonarsource/sonar-scanner-cli:latest'
                    reuseNode true
                    args '--network cicd-network'
                }
            }

            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        set -e

                        sonar-scanner \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.java.test.binaries=target/test-classes \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package Application') {
            agent {
                docker {
                    image 'eclipse-temurin:25-jdk'
                    reuseNode true
                    args '-u root'
                }
            }

            steps {
                sh '''
                    set -e

                    apt-get update -qq
                    apt-get install -y -qq zip unzip git >/dev/null

                    rm -f latest-app.zip app.jar

                    JAR="$(ls -1 target/*-SNAPSHOT.jar 2>/dev/null | grep -v '\\.original$' | head -n 1 || true)"
                    if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
                        echo "ERROR: Spring Boot JAR tidak ditemukan di target/"
                        ls -la target || true
                        exit 1
                    fi

                    cp "$JAR" app.jar
                    git -c safe.directory="$PWD" archive --format=zip --output=latest-app.zip HEAD
                    zip -q latest-app.zip "$JAR"

                    ls -lh latest-app.zip app.jar
                '''
            }
        }

        stage('Publish Application') {
            steps {
                archiveArtifacts(
                    artifacts: 'latest-app.zip,app.jar',
                    fingerprint: true,
                    allowEmptyArchive: false
                )

                script {
                    def appName = env.JOB_NAME
                        .replaceAll('[^a-zA-Z0-9._-]', '-')
                        .replaceAll('-+', '-')
                        .replaceAll('^-|-$', '')

                    def buildId = env.BUILD_NUMBER

                    sh """
                        set -e

                        docker exec cicd-jenkins \
                            mkdir -p \
                            "/var/jenkins_home/userContent/applications/${appName}/${buildId}"

                        docker cp \
                            latest-app.zip \
                            "cicd-jenkins:/var/jenkins_home/userContent/applications/${appName}/${buildId}/latest-app.zip"
                    """

                    def jenkinsBaseUrl = env.BUILD_URL
                        .substring(0, env.BUILD_URL.indexOf('/job/'))
                        .replace('localhost', 'host.docker.internal')

                    env.ARTIFACT_URL =
                        "${jenkinsBaseUrl}/userContent/applications/${appName}/${buildId}/latest-app.zip"
                }
            }
        }

        stage('Deploy Application') {
            agent {
                docker {
                    image 'curlimages/curl:8.15.0'
                    reuseNode true
                    args '--network cicd-network'
                }
            }

            steps {
                script {
                    def redeployResponse = sh(
                        script: '''
                            set -e

                            curl -sS --fail-with-body \
                                -X POST "$URL_REDEPLOY" \
                                -H "Content-Type: application/json" \
                                -d "{
                                    \\"token_access\\": \\"$DEPLOY_TOKEN\\",
                                    \\"website_id\\": \\"$WEBSITE_ID\\",
                                    \\"source_url\\": \\"$ARTIFACT_URL\\",
                                    \\"source_type\\": \\"jenkins\\"
                                }"
                        ''',
                        returnStdout: true
                    ).trim()

                    echo redeployResponse

                    def maxAttempts = 120
                    def attempt = 0
                    def deploymentStatus = 'IN_PROGRESS'

                    while (deploymentStatus == 'IN_PROGRESS') {
                        attempt++
                        if (attempt > maxAttempts) {
                            error("Deployment timeout.")
                        }
                        sleep time: 5, unit: 'SECONDS'

                        def progressResponse = sh(
                            script: '''
                                set -e

                                curl -sS --fail-with-body \
                                    -X POST "$URL_PROGRESS" \
                                    -H "Content-Type: application/json" \
                                    -d "{
                                        \\"token_access\\": \\"$DEPLOY_TOKEN\\",
                                        \\"website_id\\": \\"$WEBSITE_ID\\"
                                    }"
                            ''',
                            returnStdout: true
                        ).trim()

                        def json = readJSON text: progressResponse
                        deploymentStatus = json?.data?.status?.toString()?.toUpperCase()

                        if (!deploymentStatus) {
                            error("Response progress tidak memiliki data.status")
                        }
                        if (deploymentStatus == 'SUCCESS') {
                            break
                        }
                        if (deploymentStatus == 'FAIL') {
                            def deploymentLog = json?.data?.log ?: 'Deployment failed tanpa log.'
                            echo deploymentLog
                            error("Deployment gagal untuk website ${WEBSITE_ID}")
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts(
                artifacts: 'trivy-results.sarif',
                allowEmptyArchive: true
            )
        }
        success {
            echo "PIPELINE SUCCESS: ${env.ARTIFACT_URL ?: '(not published)'}"
        }
        failure {
            echo "PIPELINE FAILED"
        }
        unstable {
            echo "PIPELINE UNSTABLE"
        }
    }
}