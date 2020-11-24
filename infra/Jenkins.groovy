#!groovy

properties([
    disableConcurrentBuilds(),
    buildDiscarder(logRotator(daysToKeepStr: '7', numToKeepStr: '10'))
])

current_branch = "main"

jenkins_common_branch = "develop"
jenkins_common_repo_url = "https://github.com/vilvamani/jenkins_common_library.git"
jenkins_common_checkout_dir = "jenkins_library"
jenkins_common_file = "jenkins_common_library.groovy"

params = [
    branch_checkout_dir: 'service',
    service: 'angular-app',
    branch: current_branch,
    repo_url: 'https://github.com/vilvamani/angular-app.git',
    dockerRepoName: 'vilvamani007',
    dockerImageName: 'angularapp',
    kubeDeploymentFile: './infra/k8s-deployment.yaml',
    kubeServiceFile: './infra/k8s-service.yaml',
    jenkins_slack_channel: "infra-development",
    skip_unit_test: false,
    skip_integration_test: true,
    skip_sonar: false,
    skip_owasp: false,
    skip_artifactory: false,
    skip_docker_push: false,
    skip_kubernetes_deployment: false,
    skip_notification: false
]

node('angular-slave') {
    step([$class: 'WsCleanup'])
    jenkinsLibrary = loadJenkinsCommonLibrary()
    jenkinsLibrary.defaultConfigs(params)
    timestamps {
        try {
            jenkinsLibrary.checkOutSCM(params)
            jenkinsLibrary.installNodeModules(params)
            jenkinsLibrary.angularUnitTests(params)
            jenkinsLibrary.angularPublishTest(params)
            jenkinsLibrary.angularLint(params)
            jenkinsLibrary.angularBuild(params)
            dockerImage = jenkinsLibrary.dockerize(params)
            jenkinsLibrary.pushDockerImageToRepo(dockerImage, params)
            jenkinsLibrary.deployToKubernetes(params)
        } catch (Exception err) {
            currentBuild.result = 'FAILURE'
            throw err
        } finally {
            jenkinsLibrary.sendSlack(params)
        }
    }
}

def loadJenkinsCommonLibrary() {
    dir(jenkins_common_checkout_dir) {
        git(url: jenkins_common_repo_url, branch: jenkins_common_branch)
            def jenkinsLibrary = load "${jenkins_common_file}"
            return jenkinsLibrary
    }
}