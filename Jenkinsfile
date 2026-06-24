

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
        stage("${serviceDisplay}: Test") {
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
        stage("${serviceDisplay}: Coverage") {
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
        stage("${serviceDisplay}: Build") {
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
    agent none

    options {
        buildDiscarder(logRotator(
            numToKeepStr: '5',
            artifactNumToKeepStr: '2'
        ))
        disableConcurrentBuilds()
    }
    
    tools {
        maven 'maven-3.9'
        jdk 'jdk-25'
    }

    environment {
        JAVA_TOOL_OPTIONS = '-Dorg.jenkinsci.plugins.durabletask.BourneShellScript.HEARTBEAT_CHECK_INTERVAL=86400'

        MAVEN_OPTS = '-Xmx512m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -Djava.awt.headless=true'

        SONAR_TOKEN = credentials('SONAR_TOKEN')
    }

    parameters {
        string(
            name        : 'DOCKER_REGISTRY',
            defaultValue: 'docker.io',
            description : 'Docker registry used to push service images'
        )
        string(
            name        : 'DOCKER_IMAGE_NAMESPACE',
            defaultValue: 'methylch3',
            description : 'Docker image namespace/organization'
        )
        string(
            name        : 'DOCKER_IMAGE_PREFIX',
            defaultValue: 'yas-',
            description : 'Prefix for generated image names, e.g. yas-product'
        )
        string(
            name        : 'DOCKER_CREDENTIALS_ID',
            defaultValue: 'dockerhub-creds',
            description : 'Jenkins username/password credentials ID for Docker registry login'
        )
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
            agent { label 'built-in' }
            steps {
                script {
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
            agent { label 'built-in' }
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

                    def parallelStages = [:]

                    microservices.each { service ->
                        if (servicesToRun.contains(service.display)) {
                            def s = service
                            parallelStages[service.display] = {
                                throttle(['ci-services']) {
                                    node('build-agent') {
                                        def mvnHome = tool 'maven-3.9'
                                        def jdkHome = tool 'jdk-25'

                                        withEnv([
                                            "PATH+MAVEN=${mvnHome}/bin",
                                            "JAVA_HOME=${jdkHome}",
                                            "PATH+JDK=${jdkHome}/bin"
                                        ]) {
                                            try {
                                                checkout scm
                                                echo ">>> Parallel Task: ${s.display}"
                                                timeout(time: 45, unit: 'MINUTES') {
                                                    runServicePipeline(s)
                                                }
                                            } finally {
                                                cleanWs()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    parallel parallelStages
                }
            }
        }

        // ---------------------------------------------------------------- //
        // Stage 3: Build + Push Docker images for changed services only    //
        // Image tag is the current commit id for traceable deployments     //
        // ---------------------------------------------------------------- //
        stage('Build & Push Images') {
            agent { label 'build-agent' }
            steps {
                script {
                    checkout scm

                    def servicesToRun = env.SERVICES_TO_RUN
                        ? env.SERVICES_TO_RUN.split(',').toList()
                        : []

                    def servicesToBuild = microservices.findAll { service ->
                        servicesToRun.contains(service.display) &&
                        (service.enableBuild ?: false) &&
                        fileExists("${service.id}/Dockerfile")
                    }

                    if (servicesToBuild.isEmpty()) {
                        echo 'No changed services with Dockerfile found. Skipping image build/push.'
                        return
                    }

                    def mvnHome = tool 'maven-3.9'
                    def jdkHome = tool 'jdk-25'
                    def commitTag = sh(
                        script: 'git rev-parse --short=12 HEAD',
                        returnStdout: true
                    ).trim()
                    def registry = params.DOCKER_REGISTRY.trim()
                    def namespace = params.DOCKER_IMAGE_NAMESPACE.trim()
                    def imagePrefix = params.DOCKER_IMAGE_PREFIX.trim()

                    withEnv([
                        "PATH+MAVEN=${mvnHome}/bin",
                        "JAVA_HOME=${jdkHome}",
                        "PATH+JDK=${jdkHome}/bin",
                        "DOCKER_REGISTRY_VALUE=${registry}"
                    ]) {
                        withCredentials([usernamePassword(
                            credentialsId: params.DOCKER_CREDENTIALS_ID,
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )]) {
                            sh '''
                                set +x
                                printf '%s' "${DOCKER_PASSWORD}" | docker login "${DOCKER_REGISTRY_VALUE}" -u "${DOCKER_USERNAME}" --password-stdin
                                set -x
                            '''

                            servicesToBuild.each { service ->
                                def imageRepository = [registry, namespace, "${imagePrefix}${service.id}"]
                                    .findAll { it }
                                    .join('/')
                                def imageName = "${imageRepository}:${commitTag}"

                                stage("${service.display}: Build & Push Image") {
                                    echo "Building image ${imageName}"
                                    withEnv([
                                        "SERVICE_ID=${service.id}",
                                        "IMAGE_NAME=${imageName}"
                                    ]) {
                                        sh 'mvn package -pl "${SERVICE_ID}" -am -DskipTests -B --no-transfer-progress'
                                        sh 'docker buildx build --push -t "${IMAGE_NAME}" "${SERVICE_ID}"'
                                    }
                                }
                            }

                            sh 'docker logout "${DOCKER_REGISTRY_VALUE}"'
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------- //
        // Stage 4: Snyk scan ONCE for entire project (not per service)     //
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
            agent { label 'build-agent' }
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
            agent { label 'build-agent' }
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
