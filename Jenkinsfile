pipeline {
    agent any

    // triggers {
    //     githubPush()
    // }

    stages {
        // hien tai chi detect cho service
        stage('CI Pipeline for Microservices') {
            failFast false
            parallel {
                // customer service
                stage('Customer Service') {
                    when {
                        beforeAgent true
                        anyOf {
                            changeset 'customer/**'
                            changeset 'pom.xml'
                        }
                    }

                    stages {
                        stage('Unit Test') {
                            steps {
                                echo 'Running unit test for customer service'
                                // sh ...
                            }
                        }

                        stage('Integration Test') {
                            steps {
                                echo 'Running integration test for customer service'
                                // sh ...
                            }
                        }

                        stage('Build') {
                            steps {
                                echo 'Building customer service'
                                // sh...
                            }
                        }

                        stage('Security Scan') {
                            steps {
                                echo 'Running security scan for customer service'
                                // sh ...
                            }
                        }

                        stage('Deploy') {
                            steps {
                                echo 'Deploying customer service'
                                // sh ...
                            }
                        }
                    }
                }

                // cart service
                stage('Cart Service') {
                    when {
                        beforeAgent true
                        anyOf {
                            changeset 'cart/**'
                            changeset 'pom.xml'
                        }
                    }

                    stages {
                        stage('Unit Test') {
                            steps {
                                echo 'Running unit test for cart service'
                                // sh ...
                            }
                        }

                        stage('Integration Test') {
                            steps {
                                echo 'Running integration test for cart service'
                                // sh ...
                            }
                        }

                        stage('Build') {
                            steps {
                                echo 'Building cart service'
                                // sh...
                            }
                        }

                        stage('Security Scan') {
                            steps {
                                echo 'Running security scan for cart service'
                                // sh ...
                            }
                        }

                        stage('Deploy') {
                            steps {
                                echo 'Deploying cart service'
                                // sh ...
                            }
                        }
                    }
                }
            }
        }
    }
}