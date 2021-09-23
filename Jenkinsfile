/**
 *  Copyright (c) 2020 - 2021 Henix, henix.fr
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
pipeline{
    agent {label 'maven-builder-jdk11'}
    environment{
        RELEASE_BRANCH='main'
    }
    stages {
        stage('DNS workaround'){
            steps {
                container('maven'){
                    sh '''
                    echo 212.129.40.238 nexus.squashtest.org >> /etc/hosts
                    echo 212.129.62.143 sonar.squashtest.org >> /etc/hosts
                    '''
                }
            }
        }
        stage('compile') {
            steps {
                container('maven'){
                    sh 'mvn clean compile'
                }
            }
        }
        stage('QA'){
            when{
           /* the release branch is systematically merged with dev at release 
               and both are pushed simultaneously, triggering a race condition 
               in Sonar. Here we avoid this by bypassing QA in the 'release-xxx' branches.
            */
                not{
                    branch 'release-*'
                }
            }
            steps {
                container('maven'){
                    sh 'mvn org.jacoco:jacoco-maven-plugin:prepare-agent test'
                    withSonarQubeEnv('SonarQube'){
                        sh 'mvn -Dsonar.scm.disabled=true sonar:sonar'
                    }
                    timeout(time: 10, unit: 'MINUTES'){
                        sh 'mvn package javadoc:javadoc'
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }
        stage('publish'){
            when{
                not{
                    branch RELEASE_BRANCH
                }
            }
            steps{
                container('maven'){
                    sh 'mvn package deploy:deploy'
                }
            }
        }
        stage('release'){
            when{branch RELEASE_BRANCH}
            steps{
                container('maven'){
                    echo 'Performing release'
                    // BEGIN workaround missing git configuration in maven builder
                    sh 'git config --global user.email jenkins@squashtest.org'
                    sh 'git config --global user.name "Forge"'
                    // END workaround missing git configuration in maven builder
                    sh 'mvn -Prelease-logic org.codehaus.gmaven:gmaven-plugin:execute@release-logic'
                    script{
                        env.RELEASE_VERSION = readFile 'target/version.txt'
                    }
                    sh 'git checkout -b release-${RELEASE_VERSION}'
                    sh 'mvn -B release:prepare release:perform'
                    withCredentials([usernamePassword(credentialsId: 'jenkins-gitlab-creds', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]){
                        sh 'git config --local credential.helper "!f() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; f"'
                        sh 'git push --set-upstream origin release-${RELEASE_VERSION};git push --tags'
                        sh 'git fetch origin dev +refs/heads/dev:refs/remotes/origin/dev;git checkout -b dev origin/dev;git merge release-${RELEASE_VERSION};git push --set-upstream origin dev'
                    }
                }
            }
            post{
                success{
                    mattermostSend(
                        color: "#00FF00", 
                        channel: "tf-build-status", 
                        message: "${currentBuild.fullDisplayName} Release ${currentBuild.currentResult} : ${env.BUILD_URL}"
                    )
                }
            }
        }
    }
    post{
        failure{
            mattermostSend(
                color: "#FF0000", 
                channel: "tf-build-status", 
                message: "${currentBuild.fullDisplayName} ${currentBuild.currentResult} : ${env.BUILD_URL}"
            )
        }
        fixed{
            mattermostSend(
                color: "#00FF00", 
                channel: "tf-build-status", 
                message: "${currentBuild.fullDisplayName} ${currentBuild.currentResult} : ${env.BUILD_URL}"
            )
        }
    }
}