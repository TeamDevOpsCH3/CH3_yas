

def microservices = [
    [id: 'customer',       display: 'Customer Service',       enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'cart',           display: 'Cart Service',           enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'backoffice-bff', display: 'Backoffice BFF Service', enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []], 
    [id: 'inventory',      display: 'Inventory Service',      enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'location',       display: 'Location Service',       enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'media',          display: 'Media Service',          enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'order',          display: 'Order Service',          enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'payment-paypal', display: 'Payment Paypal Service', enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'payment',        display: 'Payment Service',        enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'product',        display: 'Product Service',        enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'promotion',      display: 'Promotion Service',      enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'rating',         display: 'Rating Service',         enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'search',         display: 'Search Service',         enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []], 
    [id: 'storefront-bff', display: 'Storefront BFF Service', enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []], 
    [id: 'tax',            display: 'Tax Service',            enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'webhook',        display: 'Webhook Service',        enableTest: true,  enableCoverage: true,  enableBuild: true,  commands: []],
    [id: 'sampledata',     display: 'Sampledata Service',     enableTest: false, enableCoverage: false, enableBuild: true,  commands: []],
    [id: 'recommendation', display: 'Recommendation Service', enableTest: false, enableCoverage: false, enableBuild: true,  commands: []], 
    [id: 'delivery',       display: 'Delivery Service',       enableTest: false, enableCoverage: false, enableBuild: false, commands: []], 
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
    def enableBuild    = service.enableBuild    ?: false
    def commands       = service.commands       ?: []

    // ------------------------------------------------------------------ //
    // Stage Test: mvn test (unit tests only) + publish JUnit XML          //
    // ------------------------------------------------------------------ //
    if (enableTest) {
        stage("${serviceDisplay} - Test") {
            sh "mvn test -pl ${serviceId} -am -DskipITs -Dsurefire.excludes='**/it/**,**/*IT.java,**/*ITCase.java,**/*IT*.java,**/ProductCdcConsumerTest.java,**/ApplicationTest.java' -B --no-transfer-progress"
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
            sh "mvn jacoco:report -pl ${serviceId} -DskipTests -B --no-transfer-progress"
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
            ignoreParsingErrors: true,
            qualityGates: [[
                threshold  : 70.0,
                metric     : 'INSTRUCTION',
                baseline   : 'PROJECT',
                criticality: 'FAILURE'
            ]]
        )
    }

    if (enableBuild) {
        stage("${serviceDisplay} - Build") {
            sh "mvn package -pl ${serviceId} -am -DskipTests -B --no-transfer-progress"
        }
        archiveArtifacts(
            artifacts        : "${serviceId}/target/*.jar",
            fingerprint      : true,
            allowEmptyArchive: true
        )
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

    options {
        buildDiscarder(logRotator(
            numToKeepStr: '1',
            artifactNumToKeepStr: '1'
        ))
    }
    
    tools {
        maven 'maven-3.9'
        jdk 'jdk-25'
    }

    environment {
        JAVA_TOOL_OPTIONS = '-Dorg.jenkinsci.plugins.durabletask.BourneShellScript.HEARTBEAT_CHECK_INTERVAL=86400'

        // Cap Maven heap to 512m per process.
        // Running 18 services in parallel with -Xmx1024m each = ~18GB RAM → OOM.
        // Sequential execution + 512m is safe on most CI servers.
        MAVEN_OPTS = '-Xmx512m'

        SONAR_TOKEN = credentials('SONAR_TOKEN')
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

    stages {
        stage('Gitleaks Security Scan') {
            steps {
                script {
                    echo "Running Gitleaks security scan on the entire codebase..."
                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE', message: 'Gitleaks scan failed: potential secrets detected. Check the logs for details.') {
                        echo "Running Gitleaks security scan on the entire codebase..."
                        sh "gitleaks detect --source=. --config=gitleaks.toml --verbose --no-banner"
                    }
                }
            }
        }

        // ---------------------------------------------------------------- //
        // Stage 1: Detect which services have changed (fast, no Maven)     //
        // ---------------------------------------------------------------- //
        stage('Detect Changes') {
            steps {
                script {
                    def changedPaths = collectChangedPaths()
                    def pomChanged   = changedPaths.contains('pom.xml')
                    def pipelineChanged = changedPaths.contains('Jenkinsfile')
                    def noScmContext = changedPaths.isEmpty()

                    echo "Changed paths: ${changedPaths}"
                    echo "pom.xml changed: ${pomChanged}"
                    echo "Jenkinsfile changed: ${pipelineChanged}"
                    echo "No SCM context: ${noScmContext}"

                    def globalConfigChanged = changedPaths.any { 
                        it.startsWith('common-library/') || 
                        it.startsWith('docker/') || 
                        it.contains('docker-compose.yml') 
                    }

                    // Build list of services that need to run
                    env.SERVICES_TO_RUN = microservices
                        .findAll { service ->
                            def serviceChanged = changedPaths.any { it.startsWith(service.id + '/') }
                            params.FORCE_RUN_ALL || noScmContext || pomChanged || pipelineChanged || globalConfigChanged || serviceChanged
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
        // Only runs when ENABLE_SNYK_SCAN = true (manual trigger)          //
        // ---------------------------------------------------------------- //
        stage('Snyk Security Scan') {
            when {
                allOf {
                    expression { params.ENABLE_SNYK_SCAN == true }
                    anyOf {
                        expression { params.FORCE_RUN_ALL == true }
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
