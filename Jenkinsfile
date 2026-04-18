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
    [id: 'customer',       display: 'Customer Service',       enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'cart',           display: 'Cart Service',           enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'backoffice-bff', display: 'Backoffice BFF Service', enableTest: false, enableCoverage: false, commands: []],
    [id: 'inventory',      display: 'Inventory Service',      enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'location',       display: 'Location Service',       enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'media',          display: 'Media Service',          enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'order',          display: 'Order Service',          enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'payment-paypal', display: 'Payment Paypal Service', enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'payment',        display: 'Payment Service',        enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'product',        display: 'Product Service',        enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'promotion',      display: 'Promotion Service',      enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'rating',         display: 'Rating Service',         enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'search',         display: 'Search Service',         enableTest: false, enableCoverage: false, commands: []],
    [id: 'storefront-bff', display: 'Storefront BFF Service', enableTest: false, enableCoverage: false, commands: []],
    [id: 'tax',            display: 'Tax Service',            enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'webhook',        display: 'Webhook Service',        enableTest: true,  enableCoverage: true,  commands: []],
    [id: 'sampledata',     display: 'Sampledata Service',     enableTest: false, enableCoverage: false, commands: []],
    [id: 'recommendation', display: 'Recommendation Service', enableTest: false, enableCoverage: false, commands: []],
    [id: 'delivery',       display: 'Delivery Service',       enableTest: true,  enableCoverage: true,  commands: []],
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
    def serviceId      = service.id
    def serviceDisplay = service.display
    def enableTest     = service.enableTest     ?: false
    def enableCoverage = service.enableCoverage ?: false
    def commands       = service.commands       ?: []

    // ------------------------------------------------------------------ //
    // Stage Test: mvn test (unit tests only) + publish JUnit XML          //
    // ------------------------------------------------------------------ //
    if (enableTest) {
        stage("${serviceDisplay} - Test") {
            // -DskipITs: skip integration tests (handled by failsafe, not surefire)
            // -pl ${serviceId}: only build this module in the multi-module project
            // -am: also make dependencies if needed
            sh "mvn test -pl ${serviceId} -am -DskipITs -B --no-transfer-progress"
        }
        // Publish JUnit XML report to Jenkins (always, even if tests fail)
        junit(
            testResults: "${serviceId}/target/surefire-reports/*.xml",
            allowEmptyResults: true
        )
    }

    // ------------------------------------------------------------------ //
    // Stage Coverage: jacoco:report -> HTML + XML, publish to Jenkins     //
    // ------------------------------------------------------------------ //
    if (enableCoverage) {
        stage("${serviceDisplay} - Coverage Report") {
            // jacoco:report reads the .exec file produced by prepare-agent during mvn test
            // -DskipTests: do NOT re-run tests, just generate the report
            sh "mvn jacoco:report -pl ${serviceId} -am -DskipTests -B --no-transfer-progress"
        }
        // Publish JaCoCo HTML report via HTML Publisher plugin
        publishHTML(target: [
            allowMissing         : true,
            alwaysLinkToLastBuild: true,
            keepAll              : true,
            reportDir            : "${serviceId}/target/site/jacoco",
            reportFiles          : 'index.html',
            reportName           : "JaCoCo Report - ${serviceDisplay}"
        ])
        // Record JaCoCo XML coverage via Coverage Plugin (modern, replaces old JaCoCo Plugin)
        // Uses recordCoverage() step from the Coverage Plugin
        recordCoverage(
            tools: [[
                parser: 'JACOCO',
                pattern: "${serviceId}/target/site/jacoco/jacoco.xml"
            ]],
            id              : "jacoco-${serviceId}",
            name            : "JaCoCo - ${serviceDisplay}",
            ignoreParsingErrors: true
        )
    }

    // Extra ad-hoc stages defined per-service
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

    tools {
        // The name must be same as the name in Jenkins → Manage Jenkins → Tools → Maven
        maven 'maven-3.9'
    }

    environment {
        // Fix JENKINS-48300: suppress false-positive "wrapper script not touching log" warning
        // that appears when Maven compiles silently for a long time with no stdout output.
        JAVA_TOOL_OPTIONS = '-Dorg.jenkinsci.plugins.durabletask.BourneShellScript.HEARTBEAT_CHECK_INTERVAL=86400'

        // Cap Maven heap to 512m per process.
        // Running 14 services in parallel with -Xmx1024m each = ~14GB RAM → OOM.
        // Sequential execution + 512m is safe on most CI servers.
        MAVEN_OPTS = '-Xmx512m'
    }

    // triggers {
    //     githubPush()
    // }

    stages {
        // ---------------------------------------------------------------- //
        // Stage 1: Detect which services have changed (fast, no Maven)    //
        // ---------------------------------------------------------------- //
        stage('Detect Changes') {
            steps {
                script {
                    def changedPaths = collectChangedPaths()
                    def pomChanged   = changedPaths.contains('pom.xml')
                    def noScmContext = changedPaths.isEmpty()

                    echo "Changed paths: ${changedPaths}"

                    // Build list of services that need to run
                    env.SERVICES_TO_RUN = microservices
                        .findAll { service ->
                            def serviceChanged = changedPaths.any { it.startsWith(service.id + '/') }
                            noScmContext || pomChanged || serviceChanged
                        }
                        .collect { it.display }
                        .join(',')

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

                        echo "========================================"
                        echo "Running pipeline for: ${service.display}"
                        echo "========================================"

                        // Wrap each service in a timeout to prevent one stuck
                        // Maven process from hanging the entire build forever
                        timeout(time: 45, unit: 'MINUTES') {
                            runServicePipeline(service)
                        }
                    }
                }
            }
        }
    }
}