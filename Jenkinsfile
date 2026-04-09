pipeline {
    agent any

    // Kích hoạt trigger khi có push lên GitHub
    triggers {
        githubPush()
    }

    stages {
        stage('Build Payment Service') {
            when {
              // chỉ chạy stage này khi folder payment (chứa service payment) thay đổi
                changeset "payment/**"
            }
            steps {
                echo "Thư mục payment có thay đổi, build thử..."
            }
        }

        stage('Build Product Service') {
            when {
                changeset "product/**"
            }
            steps {
                echo "Thư mục product có thay đổi, build thử..."
            }
        }
    }
}