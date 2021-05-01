pipeline {
    agent any
    stages {
        stage('Chekout') {
            steps {
                echo 'Checouting..'
                git branch: 'master', credentialsId: 'GH', url: 'https://github.com/naivs/money-tracker.git'
                sh "ls -la"
            }
        }
        stage('Test') {
            steps {
                echo 'Testing..'
                sh returnStdout: true, script: 'docker run --rm -u root -v /home/ivan/jenkins-data/workspace/money-tracker:/home/gradle/project -w /home/gradle/project gradle:6.8.3-jdk15 gradle test'
            }
        }
        stage('Build') {
            steps {
                echo 'Building..'
                sh "docker rm -f money-tracker"
                sh "docker build -t money-tracker ."
            }
        }
        stage('Run') {
            steps {
                echo 'Run..'
                withCredentials([string(credentialsId: 'finansistApiKey', variable: 'API_KEY'), string(credentialsId: 'finansistBotName', variable: 'NAME')]) {
                    sh returnStdout: true, script: 'docker run -d -p 8083:8080 -v /home/ivan/money-tracker:/money-tracker/database --env BOT_API_KEY=${API_KEY} --env BOT_NAME=${NAME} --name money-tracker money-tracker'
                }
            }
        }
        stage('Cleanup') {
            steps {
                cleanWs()
                sh returnStdout: true, script: 'docker rmi $(docker images -f "dangling=true" -q)'
            }
        }
    }
}
