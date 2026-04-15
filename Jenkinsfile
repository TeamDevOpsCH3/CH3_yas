/*
how to add new service/stage:
template: [
    id: 'service-name',                                             // folder name (e.g., 'payment')
    display: 'Service Display Name',                                // Jenkins UI name (e.g., 'Payment Service')
    commands: [                                                     // array of stages to run for this service
        [name: 'Stage Name', command: 'your shell command here'],
        [name: 'Unit Test', command: 'mvn test -pl service-name'],
    ]
]
*/

def microservices = [
    [id: 'customer', display: 'Customer Service', commands: [
        [name: 'Unit Test', command: 'echo "Running unit tests for Customer Service"'],
        [name: 'Integration Test', command: 'echo "Running integration tests for Customer Service"'],
        [name: 'Build', command: 'echo "Building Customer Service"'],
        [name: 'Security Scan', command: 'echo "Running security scan for Customer Service"'],
        [name: 'Deploy', command: 'echo "Deploying Customer Service to staging environment"']
    ]],
    [id: 'cart', display: 'Cart Service', commands: []],
    [id: 'backoffice-bff', display: 'Backoffice BFF Service', commands: []],
    [id: 'inventory', display: 'Inventory Service', commands: []],
    [id: 'location', display: 'Location Service', commands: []],
    [id: 'media', display: 'Media Service', commands: []],
    [id: 'order', display: 'Order Service', commands: []],
    [id: 'payment-paypal', display: 'Payment Paypal Service', commands: []],
    [id: 'payment', display: 'Payment Service', commands: []],
    [id: 'product', display: 'Product Service', commands: []],
    [id: 'promotion', display: 'Promotion Service', commands: []],
    [id: 'rating', display: 'Rating Service', commands: []],
    [id: 'search', display: 'Search Service', commands: []],
    [id: 'storefront-bff', display: 'Storefront BFF Service', commands: []],
    [id: 'tax', display: 'Tax Service', commands: []],
    [id: 'webhook', display: 'Webhook Service', commands: []],
    [id: 'sampledata', display: 'Sampledata Service', commands: []],
    [id: 'recommendation', display: 'Recommendation Service', commands: []],
    [id: 'delivery', display: 'Delivery Service', commands: []]
]

@NonCPS
def collectChangedPaths() {
    def paths = []
    currentBuild.changeSets.each { changeSet ->
        changeSet.items.each { entry ->
            entry.affectedFiles.each { file ->
                paths << file.path
            }
        }
    }
    return paths.unique()
}

def runServicePipeline(service) {
    def serviceDisplay = service.display
    def commands = service.commands ?: []

    if (commands.isEmpty()) {
        echo "No commands defined for ${serviceDisplay}, skipping..."
        return
    }

    commands.each { cmd ->
        stage("${serviceDisplay} - ${cmd.name}") {
            if (cmd.command) {
                sh cmd.command
            } else {
                echo "No command defined for stage ${cmd.name}"
            }
        }
    }
}

pipeline {
    agent any

    // triggers {
    //     githubPush()
    // }

    stages {
        stage('CI Pipeline for Microservices') {
            steps {
                script {

                    def changedPaths = collectChangedPaths()
                    def pomChanged = changedPaths.contains('pom.xml')
                    def noScmContext = changedPaths.isEmpty()

                    echo "Changed paths: ${changedPaths}"

                    def branches = [:]

                    microservices.each { service ->

                        def serviceId = service.id
                        def serviceDisplay = service.display

                        branches[serviceDisplay] = {

                            def serviceChanged = changedPaths.any { path ->
                                path.startsWith(serviceId + '/')
                            }

                            def shouldRun = noScmContext || pomChanged || serviceChanged

                            if (!shouldRun) {
                                echo "Skipping ${serviceDisplay} (no related changes)"
                                return
                            }

                            echo "Running pipeline for ${serviceDisplay}"
                            runServicePipeline(service)
                        }
                    }

                    parallel branches + [failFast: false]
                }
            }
        }
    }
}