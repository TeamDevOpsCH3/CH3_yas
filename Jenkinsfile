/*
  SNYK TEST MODE — chỉ chạy Snyk scan, bỏ qua tất cả stages khác
  Dùng xong nhớ revert về Jenkinsfile gốc
*/

pipeline {
    agent any

    tools {
        maven 'maven-3.9'
        jdk 'jdk-25'
    }

    environment {
        JAVA_TOOL_OPTIONS = '-Dorg.jenkinsci.plugins.durabletask.BourneShellScript.HEARTBEAT_CHECK_INTERVAL=86400'
        MAVEN_OPTS        = '-Xmx512m'
    }

    parameters {
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
        stage('Snyk Security Scan') {
            steps {
                echo 'Running Snyk scan for entire project...'
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