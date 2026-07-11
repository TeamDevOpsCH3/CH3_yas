

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
    [id: 'storefront',     display: 'Storefront UI',          enableTest: false, enableCoverage: false, enableBuild: true,  buildTool: 'node', commands: []],
    [id: 'backoffice',     display: 'Backoffice UI',          enableTest: false, enableCoverage: false, enableBuild: true,  buildTool: 'node', commands: []],
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
    def buildTool      = service.buildTool      ?: 'maven'
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

    if (enableBuild && buildTool == 'maven') {
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
        string(
            name        : 'DOCKER_BUILDX_BUILDER',
            defaultValue: 'yas-builder',
            description : 'Docker Buildx builder name used on Jenkins build agents'
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
            when { expression { !(env.BRANCH_NAME ?: '').startsWith('fastImage/') } }
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
                    def changedPaths = sh(
                        script: '''
                            set -e

                            git fetch origin "${BRANCH_NAME}" --no-tags --depth=50 || true

                            if git rev-parse HEAD~1 >/dev/null 2>&1; then
                                git diff --name-only HEAD~1 HEAD
                            else
                                git diff-tree --no-commit-id --name-only --root -r HEAD
                            fi
                        ''',
                        returnStdout: true
                    ).trim()
                    .split('\\n')
                    .collect { it.trim() }
                    .findAll { it }

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
                            params.FORCE_RUN_ALL || (env.BRANCH_NAME ?: '').startsWith('fastImage/') ||
                            pomChanged || globalConfigChanged || serviceChanged
                        }
                        .collect { it.display }
                        .join(',')

                    echo "Force run all: ${params.FORCE_RUN_ALL}"
                    echo "Services to run: ${env.SERVICES_TO_RUN ?: '(none)'}"
                }
                // built-in da co implicit checkout (khong skipDefaultCheckout) -> stash source
                // 1 lan. Parallel unstash thay vi ~14 `checkout scm` dong thoi -> tranh saturate
                // network burst cua EC2 build-agent (root cause checkout timeout). Loai .git (default
                // excludes) vi mvn test/package khong can git; Build&Push prep giu checkout rieng.
                stash name: 'source'
            }
        }

        // ---------------------------------------------------------------- //
        // Stage 2: Run Test + Coverage sequentially (one service at a time)//
        // Running all in parallel saturates RAM → OOM → Jenkins restart   //
        // ---------------------------------------------------------------- //
        stage('Test & Coverage') {
            when { expression { !(env.BRANCH_NAME ?: '').startsWith('fastImage/') } }
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
                                                retry(3) { unstash 'source' }
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
            when { expression { !(env.BRANCH_NAME ?: '').startsWith('fastImage/') } }
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

        // ---------------------------------------------------------------- //
        // Stage 4: Build + Push Docker images for changed services only    //
        // Image tag is the current commit id for traceable deployments     //
        // ---------------------------------------------------------------- //
        stage('Build & Push Images') {
            steps {
                script {
                    // ====== Build SONG SONG tối đa 2 (chunk 2). Revert TUẦN TỰ: đổi collate(2) -> collate(1). ======
                    def registry      = params.DOCKER_REGISTRY.trim()
                    def namespace     = params.DOCKER_IMAGE_NAMESPACE.trim()
                    def imagePrefix   = params.DOCKER_IMAGE_PREFIX.trim()
                    def buildxBuilder = params.DOCKER_BUILDX_BUILDER.trim()

                    def servicesToBuild = []
                    def commitTag = ''

                    // B1. Chuẩn bị 1 lần: danh sách build + tag + docker login + builder (host dùng chung)
                    node('build-agent') {
                        // Prep can .git (git rev-parse HEAD -> commit tag) -> giu checkout nhung
                        // SHALLOW depth=1 + noTags (nhe network) + retry cho network chap chon.
                        retry(3) {
                            checkout([
                                $class: 'GitSCM',
                                branches: scm.branches,
                                userRemoteConfigs: scm.userRemoteConfigs,
                                extensions: scm.extensions + [
                                    [$class: 'CloneOption', shallow: true, depth: 1, noTags: true, timeout: 30],
                                    [$class: 'CheckoutOption', timeout: 30]
                                ]
                            ])
                        }
                        def servicesToRun = env.SERVICES_TO_RUN
                            ? env.SERVICES_TO_RUN.split(',').toList()
                            : []
                        servicesToBuild = microservices.findAll { service ->
                            servicesToRun.contains(service.display) &&
                            (service.enableBuild ?: false) &&
                            fileExists("${service.id}/Dockerfile")
                        }
                        if (!servicesToBuild.isEmpty()) {
                            commitTag = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
                            withEnv([
                                "DOCKER_REGISTRY_VALUE=${registry}",
                                "DOCKER_BUILDX_BUILDER_VALUE=${buildxBuilder}"
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
                                    sh '''
                                        if ! docker buildx version >/dev/null 2>&1; then
                                            echo 'Docker Buildx is not available on this Jenkins agent.'
                                            exit 1
                                        fi
                                        if ! docker buildx inspect "${DOCKER_BUILDX_BUILDER_VALUE}" >/dev/null 2>&1; then
                                            docker buildx create --name "${DOCKER_BUILDX_BUILDER_VALUE}" --use
                                        fi
                                        docker buildx use "${DOCKER_BUILDX_BUILDER_VALUE}"
                                        docker buildx inspect --bootstrap
                                    '''
                                }
                            }
                        }
                    }

                    if (servicesToBuild.isEmpty()) {
                        echo 'No changed services with Dockerfile found. Skipping image build/push.'
                        return
                    }

                    // B2. Build "trống là vào" — giới hạn = số executor agent (đặt = 2 trong Nodes; nâng 3 sau, KHÔNG sửa code)
                    def branches = [:]
                    servicesToBuild.each { service ->
                        def svc = service
                        def imageRepository = [registry, namespace, "${imagePrefix}${svc.id}"]
                            .findAll { it }
                            .join('/')
                        def imageName = "${imageRepository}:${commitTag}"
                        branches["${svc.display}"] = {
                            node('build-agent') {
                                stage("${svc.display}: Build & Push") {
                                    def mvnHome = tool 'maven-3.9'
                                    def jdkHome = tool 'jdk-25'
                                    withEnv([
                                        "PATH+MAVEN=${mvnHome}/bin",
                                        "JAVA_HOME=${jdkHome}",
                                        "PATH+JDK=${jdkHome}/bin",
                                        "DOCKER_BUILDX_BUILDER_VALUE=${buildxBuilder}",
                                        "SERVICE_ID=${svc.id}",
                                        "IMAGE_NAME=${imageName}"
                                    ]) {
                                        try {
                                            // Build (mvn package + docker build) khong can .git -> unstash
                                            // source (0 network) thay checkout scm (chunk 2 -> nhe hon Test).
                                            retry(3) { unstash 'source' }
                                            echo "Building and pushing image for ${svc.display} with tag ${commitTag}"
                                            if ((svc.buildTool ?: 'maven') == 'maven') {
                                                sh 'mvn package -pl "${SERVICE_ID}" -am -DskipTests -B --no-transfer-progress'
                                                sh 'docker buildx build --builder "${DOCKER_BUILDX_BUILDER_VALUE}" -t "${IMAGE_NAME}" --push "${SERVICE_ID}"'
                                            } else {
                                                // UI Next.js: cap heap Node de khong OOM agent 2GB
                                                sh 'docker buildx build --builder "${DOCKER_BUILDX_BUILDER_VALUE}" --build-arg NODE_OPTIONS=--max-old-space-size=1536 -t "${IMAGE_NAME}" --push "${SERVICE_ID}"'
                                            }
                                        } finally {
                                            cleanWs()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    parallel branches

                    // B3. Logout 1 lan
                    node('build-agent') {
                        withEnv(["DOCKER_REGISTRY_VALUE=${registry}"]) {
                            sh 'docker logout "${DOCKER_REGISTRY_VALUE}" || true'
                        }
                    }
                }
            }
        }
    }
}
