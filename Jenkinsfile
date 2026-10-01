pipeline {
    agent any

    tools {
        maven 'Maven3'
        nodejs 'Node24'
    }

    environment {
        SPRING_DATASOURCE_URL = 'jdbc:mysql://db:3307/coffeeshop'
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Backend: Build & Test') {
            steps {
                dir('backend') {
                    sh 'mvn -B clean verify'
                }
            }
        }

        stage('Backend: SonarQube Analysis') {
            steps {
                dir('backend') {
                    withSonarQubeEnv('SonarQube') {
                        sh '''mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                              -Dsonar.projectKey=coffeeshop-backend \
                              -Dsonar.projectName=coffeeshop-backend'''
                    }
                }
            }
        }

        stage('Backend: Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Frontend: Install & Build') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Frontend: SonarQube Analysis') {
            steps {
                dir('frontend') {
                    script {
                        def scannerHome = tool 'SonarScanner'
                        withSonarQubeEnv('SonarQube') {
                            sh "${scannerHome}/bin/sonar-scanner"
                        }
                    }
                }
            }
        }

        stage('Frontend: Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }
}