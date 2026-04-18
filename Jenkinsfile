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
            sh "mvn test -pl ${serviceId} -am -DskipITs -B --no-transfer-progress"
        }
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
            sh "mvn jacoco:report -pl ${serviceId} -am -DskipTests -B --no-transfer-progress"
        }
        publishHTML(target: [
            allowMissing         : true,
            alwaysLinkToLastBuild: true,
            keepAll              : true,
            reportDir            : "${serviceId}/target/site/jacoco",
            reportFiles          : 'index.html',
            reportName           : "JaCoCo Report - ${serviceDisplay}"
        ])
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
        maven 'maven-3.9'
        jdk 'jdk-21'
    }

    environment {
        // Fix JENKINS-48300: suppress false-positive "wrapper script not touching log" warning
        JAVA_TOOL_OPTIONS = '-Dorg.jenkinsci.plugins.durabletask.BourneShellScript.HEARTBEAT_CHECK_INTERVAL=86400'

        // [FIX OOM] Cap Maven heap to 512m per process.
        // Running services in parallel with default -Xmx1024m = RAM exhausted → crash.
        MAVEN_OPTS = '-Xmx512m'
    }

    parameters {
        booleanParam(
            name        : 'FORCE_RUN_ALL',
            defaultValue: false,
            description : 'Force run Test + Coverage for ALL services regardless of which files changed'
        )
        booleanParam(
            name        : 'ENABLE_SNYK_SCAN',
            defaultValue: false,
            description : 'Run Snyk OSS vulnerability scan once for the entire project (heavy — enable manually)'
        )
        choice(
            name        : 'SNYK_SEVERITY',
            choices     : ['low', 'medium', 'high', 'critical'],
            description : 'Fail/report threshold for Snyk scan'
        )
        booleanParam(
            name        : 'SNYK_FAIL_ON_ISSUES',
            defaultValue: false,
            description : 'Fail build when Snyk finds issues at/above threshold'
        )
    }

    // triggers {
    //     githubPush()
    // }

    stages {
        // ---------------------------------------------------------------- //
        // Stage 1: Detect which services have changed (fast, no Maven)     //
        // ---------------------------------------------------------------- //
        stage('Detect Changes') {
            steps {
                script {
                    def changedPaths = collectChangedPaths()
                    def pomChanged   = changedPaths.contains('pom.xml')
                    def noScmContext = changedPaths.isEmpty()

                    echo "Changed paths: ${changedPaths}"

                    env.SERVICES_TO_RUN = microservices
                        .findAll { service ->
                            def serviceChanged = changedPaths.any { it.startsWith(service.id + '/') }
                            params.FORCE_RUN_ALL || noScmContext || pomChanged || serviceChanged
                        }
                        .collect { it.display }
                        .join(',')

                    echo "Force run all: ${params.FORCE_RUN_ALL}"
                    echo "Services to run: ${env.SERVICES_TO_RUN ?: '(none)'}"
                }
            }
        }

        // ---------------------------------------------------------------- //
        // Stage 2: Run Test + Coverage SEQUENTIALLY (one service at a time)//
        // [FIX OOM] Parallel execution saturates RAM → OOM → Jenkins crash //
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

                        // Timeout per service to prevent stuck Maven from hanging forever
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
                expression { params.ENABLE_SNYK_SCAN == true }
            }
            steps {
                echo "Running Snyk scan for entire project..."
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
    }
}
