pipeline {
    agent any

    tools {
        maven 'Maven3'
        nodejs 'Node24'
    }

    environment {
        SPRING_DATASOURCE_URL = 'jdbc:mysql://db:3306/coffeeshop'
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
		    retry (3) {
			sh 'npm run build'
		    }
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

        stage('Deploy') {
            steps {
                sh 'docker compose -p coffeeshop-app -f deploy/docker-compose.app.yml up -d --build'
            }
        }

        stage('Smoke Test') {
            steps {
                sh '''
                    for i in $(seq 1 30); do
                    if curl -fs http://coffeeshop-backend:8081/actuator/health | grep -q UP; then
                    echo "Backend is UP, access http://localhost:8081/actuator/health"; break
                    fi
                    echo "Waiting for backend... ($i)"; sleep 5
                    done
                    curl -fs http://coffeeshop-backend:8081/actuator/health | grep -q UP
                    curl -fs http://coffeeshop-frontend:80 > /dev/null && echo "Frontend is UP, access http://localhost:8083"
                '''
            }
        }
    }
}

environment {
	SPRING_DATASOURCE_URL = 'jdbc:mysql://db:3306/coffeeshop'
	VITE_API_URL = 'http://localhost:8081/api'
	NODE_OPTIONS = '--dns-result-order=ipv4first'
}
