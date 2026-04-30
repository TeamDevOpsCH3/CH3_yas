/*
how to add new service/stage:
template: [
    id: 'service-name',                                             // folder name (e.g., 'payment')
    display: 'Service Display Name',                                // Jenkins UI name (e.g., 'Payment Service')
    enableTest: true,                                               // set true to run mvn test + publish JUnit XML
    enableCoverage: true,                                           // set true to generate + publish JaCoCo report
    commands: [                                                     // extra stages to run for this service
        [name: 'Stage Name', command: 'your shell command here'],
    ]
]
*/ 


def microservices = [
    [id: 'customer',       display: 'Customer Service',       enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'cart',           display: 'Cart Service',           enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'backoffice-bff', display: 'Backoffice BFF Service', enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []], 
    [id: 'inventory',      display: 'Inventory Service',      enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'location',       display: 'Location Service',       enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'media',          display: 'Media Service',          enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'order',          display: 'Order Service',          enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'payment-paypal', display: 'Payment Paypal Service', enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'payment',        display: 'Payment Service',        enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'product',        display: 'Product Service',        enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'promotion',      display: 'Promotion Service',      enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'rating',         display: 'Rating Service',         enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'search',         display: 'Search Service',         enableTest: false, enableCoverage: false, enableBuild: false, commands: []], 
    [id: 'storefront-bff', display: 'Storefront BFF Service', enableTest: false, enableCoverage: false, enableBuild: false, commands: []], 
    [id: 'tax',            display: 'Tax Service',            enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'webhook',        display: 'Webhook Service',        enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'sampledata',     display: 'Sampledata Service',     enableTest: false, enableCoverage: false, enableBuild: false, commands: []],
    [id: 'recommendation', display: 'Recommendation Service', enableTest: false, enableCoverage: false, enableBuild: false, commands: []], 
    [id: 'delivery',       display: 'Delivery Service',       enableTest: false, enableCoverage: false, enableBuild: false, commands: []], 
]                    def changedPaths = collectChangedPaths()
                    def pomChanged   = changedPaths.contains('pom.xml')
                    def pipelineChanged = changedPaths.contains('Jenkinsfile')
                    def noScmContext = changedPaths.isEmpty()

                    echo "Changed paths: ${changedPaths}"
                    echo "pom.xml changed: ${pomChanged}"
                    echo "Jenkinsfile changed: ${pipelineChanged}"
                    echo "No SCM context: ${noScmContext}"

                    // Build list of services that need to run
                    env.SERVICES_TO_RUN = microservices
                        .findAll { service ->
                            def serviceChanged = changedPaths.any { it.startsWith(service.id + '/') }
                            params.FORCE_RUN_ALL || noScmContext || pomChanged || pipelineChanged || serviceChanged
                        }
                        .collect { it.display }
                        .join(',')

                    echo "Force run all: ${params.FORCE_RUN_ALL}"
                    echo "Services to run: ${env.SERVICES_TO_RUN ?: '(none)'}"
                }
            }
        }

        // ---------------------------------------------------------------- //
        // Stage 2: Run Test + Coverage sequentially (one service at a time)//
        // Running all in parallel saturates RAM → OOM → Jenkins restart   //
        // ---------------------------------------------------------------- //
        stage('Test & Coverage') {
            steps {
                script {
                    def servicesToRun = env.SERVICES_TO_RUN
                        ? env.SERVICES_TO_RUN.split(',').toList()
                        : []

                    if (servicesToRun.isEmpty()) {
                        echo 'No services to test. Skipping.'
                        return
                    }

                    microservices.each { service ->
                        if (!servicesToRun.contains(service.display)) return

                        echo '========================================'
                        echo "Running pipeline for: ${service.display}"
                        echo '========================================'

                        // Wrap each service in a timeout to prevent one stuck
                        // Maven process from hanging the entire build forever
                        timeout(time: 45, unit: 'MINUTES') {
                            runServicePipeline(service)
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------- //
        // Stage 3: Snyk scan ONCE for entire project (not per service)     //
        // [FIX OOM] Scanning per-service = 19x Snyk processes → crash      //
        // Only runs when ENABLE_SNYK_SCAN = true (manual trigger)          //
        // ---------------------------------------------------------------- //
        stage('Snyk Security Scan') {
            when {
                allOf {
                    expression { params.ENABLE_SNYK_SCAN == true }
                    anyOf {
                        // Lần đầu / manual force
                        expression { params.FORCE_RUN_ALL == true }
                        // Chỉ chạy lại khi có pom.xml thay đổi
                        expression {
                            def changedPaths = collectChangedPaths()
                            changedPaths.isEmpty() ||   // no SCM context = first run
                            changedPaths.any { it == 'pom.xml' || it.endsWith('/pom.xml') }
                        }
                    }
                }
            }
            steps {
                snykSecurity(
                    snykInstallation: 'snyk-cli',
                    snykTokenId     : 'snyk-token',
                    targetFile      : 'pom.xml',
                    projectName     : 'yas-monorepo',
                    severity        : params.SNYK_SEVERITY,
                    failOnIssues    : params.SNYK_FAIL_ON_ISSUES
                )
            }
        }

        stage('SonarCloud Scan') {
            steps {
                script {
                    def servicesToRun = env.SERVICES_TO_RUN
                        ? env.SERVICES_TO_RUN.split(',').toList()
                        : []

                    if (servicesToRun.isEmpty()) {
                        echo 'No services to scan. Skipping.'
                        return
                    }

                    sh """
                        mvn verify sonar:sonar \
                        -DskipTests \
                        -DskipITs \
                        -Dsonar.projectKey=teamdevopsch3_CH3_yas4 \
                        -Dsonar.organization=teamdevopsch3 \
                        -Dsonar.host.url=https://sonarcloud.io \
                        -Dsonar.login=\${SONAR_TOKEN} \
                        -B --no-transfer-progress
                    """
                }
            }
        }
    }
}
