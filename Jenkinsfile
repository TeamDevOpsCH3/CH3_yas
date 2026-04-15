pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                echo 'Running Build phase...'
            }
        }
        stage('Test') {
            steps {
                echo 'Running Test phase...'
            }
        }
    }
    
    post {
        success {
            echo 'Great! The pipeline ran successfully.'
        }
        failure {
            echo 'Oops! The pipeline failed.'
        }
    }
}