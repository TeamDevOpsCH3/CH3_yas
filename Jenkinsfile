/*
how to add new service/stage:
template: [
    id: 'service-name',                                             // folder name (e.g., 'payment')
    display: 'Service Display Name',                                // Jenkins UI name (e.g., 'Payment Service')
    enableTest: true,                                               // set true to run mvn test + publish JUnit XML
    enableCoverage: true,                                           // set true to generate + publish JaCoCo report
    enableSnyk: true,                                               // set true to run Snyk dependency scan
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
    def enableSnyk     = (service.enableSnyk == null) ? true : service.enableSnyk
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

    // ------------------------------------------------------------------ //
    // Stage Snyk: scan OSS vulnerabilities from service pom.xml           //
    // ------------------------------------------------------------------ //
    if (enableSnyk && params.ENABLE_SNYK_SCAN) {
        stage("Snyk Vulnerability Scan - ${serviceDisplay}") {
            if (!fileExists("${serviceId}/pom.xml")) {
                echo "Skipping Snyk for ${serviceDisplay}: ${serviceId}/pom.xml not found"
            } else {
                echo "Starting Snyk scan for ${serviceDisplay}..."
                snykSecurity(
                    snykInstallation: 'snyk-cli',
                    snykTokenId: 'snyk-token',
                    targetFile: "${serviceId}/pom.xml",
                    projectName: "yas-${serviceId}",
                    severity: params.SNYK_SEVERITY,
                    failOnIssues: params.SNYK_FAIL_ON_ISSUES
                )
            }
        }
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

    parameters {
        booleanParam(
            name: 'ENABLE_SNYK_SCAN',
            defaultValue: true,
            description: 'Run Snyk OSS vulnerability scan per selected microservice'
        )
        choice(
            name: 'SNYK_SEVERITY',
            choices: ['low', 'medium', 'high', 'critical'],
            description: 'Fail/report threshold for Snyk scan'
        )
        booleanParam(
            name: 'SNYK_FAIL_ON_ISSUES',
            defaultValue: false,
            description: 'Fail build when issues at/above threshold are found'
        )
    }

    tools {
        // The name must be same as the name in Jenkins → Manage Jenkins → Tools → Maven
        maven 'maven-3.9'
    }

    // triggers {
    //     githubPush()
    // }

    stages {
        stage('CI Pipeline for Microservices') {
            steps {
                script {
                    def changedPaths = collectChangedPaths()
                    def pomChanged   = changedPaths.contains('pom.xml')
                    def noScmContext = changedPaths.isEmpty()

                    echo "Changed paths: ${changedPaths}"

                    def branches = [:]

                    microservices.each { service ->
                        def serviceId      = service.id
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
