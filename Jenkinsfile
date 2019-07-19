/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
pipeline {
  agent none
  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timeout(time: 12, unit: 'HOURS')
  }
  triggers {
    cron('@weekly')
    pollSCM('@daily')
  }
  stages {
    stage ('Debug') {
      options {
        timeout(time: 1, unit: 'HOURS')
        retry(2)
      }
      agent {
        docker {
          label 'ubuntu && !H28 && !H36 && !H40'
          image 'apachedirectory/maven-build:jdk-8'
          args '-v $HOME/.m2:/home/hnelson/.m2'
        }
      }
      steps {
        sh 'env'
      }
      post {
        always {
          deleteDir()
        }
      }
    }
    stage ('Build and Test') {
      parallel {
        stage ('Linux Java 8') {
          options {
            timeout(time: 4, unit: 'HOURS')
            retry(2)
          }
          agent {
            docker {
              label 'ubuntu && !H28 && !H36 && !H40'
              image 'apachedirectory/maven-build:jdk-8'
              args '-v $HOME/.m2:/home/hnelson/.m2'
            }
          }
          steps {
            sh '''
            mvn -V -U clean verify
            '''
          }
          post {
            always {
              junit '**/target/surefire-reports/*.xml'
              deleteDir()
            }
          }
        }
        stage ('Linux Java 11') {
          options {
            timeout(time: 4, unit: 'HOURS')
            retry(2)
          }
          agent {
            docker {
              label 'ubuntu && !H28 && !H36 && !H40'
              image 'apachedirectory/maven-build:jdk-11'
              args '-v $HOME/.m2:/home/hnelson/.m2'
            }
          }
          steps {
            sh 'mvn -V -U clean verify'
          }
          post {
            always {
              junit '**/target/surefire-reports/*.xml'
              deleteDir()
            }
          }
        }
        stage ('Linux Java 12') {
          options {
            timeout(time: 4, unit: 'HOURS')
            retry(2)
          }
          agent {
            docker {
              label 'ubuntu && !H28 && !H36 && !H40'
              image 'apachedirectory/maven-build:jdk-12'
              args '-v $HOME/.m2:/home/hnelson/.m2'
            }
          }
          steps {
            sh 'mvn -V -U clean verify'
          }
          post {
            always {
              junit '**/target/surefire-reports/*.xml'
              deleteDir()
            }
          }
        }
        stage ('Windows Java 8') {
          options {
            timeout(time: 4, unit: 'HOURS')
            retry(2)
          }
          agent {
            label 'Windows'
          }
          steps {
            // TODO: need to investigate test failure on Windows
            bat '''
            set JAVA_HOME=F:\\jenkins\\tools\\java\\latest1.8
            set MAVEN_OPTS="-Xmx512m"
            F:\\jenkins\\tools\\maven\\latest3\\bin\\mvn -V -U clean verify
            '''
          }
          post {
            always {
              junit '**/target/surefire-reports/*.xml'
              deleteDir()
            }
          }
        }
      }
    }
    stage ('Deploy') {
      options {
        timeout(time: 4, unit: 'HOURS')
        retry(2)
      }
      agent {
        label 'ubuntu && !H28 && !H36 && !H40'
      }
      // https://cwiki.apache.org/confluence/display/INFRA/JDK+Installation+Matrix
      // https://cwiki.apache.org/confluence/display/INFRA/Maven+Installation+Matrix
      steps {
        sh '''
        export JAVA_HOME=/home/jenkins/tools/java/latest1.8
        export MAVEN_OPTS="-Xmx512m"
        /home/jenkins/tools/maven/latest3/bin/mvn -V -U clean deploy -DskipTests
        '''
      }
      post {
        always {
          deleteDir()
        }
      }
    }
    stage ('Build Installers') {
      options {
        timeout(time: 2, unit: 'HOURS')
        retry(2)
      }
      agent {
        docker {
          label 'ubuntu && !H28 && !H36 && !H40'
          image 'apachedirectory/maven-build:jdk-8'
          args '-v $HOME/.m2:/home/hnelson/.m2'
          alwaysPull true
        }
      }
      steps {
        sh 'mvn -V -U clean verify -DskipTests -Pinstallers -Pdocker'
        stash name: 'deb', includes: 'installers/target/installers/*.deb,installers/target/docker/*deb*'
        stash name: 'rpm', includes: 'installers/target/installers/*.rpm,installers/target/docker/*rpm*'
        stash name: 'bin', includes: 'installers/target/installers/*.bin,installers/target/docker/*bin*'
        stash name: 'archive', includes: 'installers/target/installers/*.zip,installers/target/installers/*.tar.gz,installers/target/docker/*archive*'
      }
      post {
        always {
          archiveArtifacts 'installers/target/installers/*.zip,installers/target/installers/*.tar.gz,installers/target/installers/*.dmg,installers/target/installers/*.exe,installers/target/installers/*.bin,installers/target/installers/*.deb,installers/target/installers/*.rpm'
          deleteDir()
        }
      }
    }
    stage ('Test Installers') {
      parallel {
        stage ('deb') {
          options {
            timeout(time: 2, unit: 'HOURS')
            retry(2)
          }
          agent {
            label 'ubuntu && !H28 && !H36 && !H40'
          }
          steps {
            unstash 'deb'
            sh 'bash installers/target/docker/run-deb-tests.sh'
          }
          post {
            always {
              deleteDir()
            }
          }
        }
        stage ('rpm') {
          options {
            timeout(time: 2, unit: 'HOURS')
            retry(2)
          }
          agent {
            label 'ubuntu && !H28 && !H36 && !H40'
          }
          steps {
            unstash 'rpm'
            sh 'bash installers/target/docker/run-rpm-tests.sh'
          }
          post {
            always {
              deleteDir()
            }
          }
        }
        stage ('bin') {
          options {
            timeout(time: 2, unit: 'HOURS')
            retry(2)
          }
          agent {
            label 'ubuntu && !H28 && !H36 && !H40'
          }
          steps {
            unstash 'bin'
            sh 'bash installers/target/docker/run-bin-tests.sh'
          }
          post {
            always {
              deleteDir()
            }
          }
        }
        stage ('archive') {
          options {
            timeout(time: 2, unit: 'HOURS')
            retry(2)
          }
          agent {
            label 'ubuntu && !H28 && !H36 && !H40'
          }
          steps {
            unstash 'archive'
            sh 'bash installers/target/docker/run-archive-tests.sh'
          }
          post {
            always {
              deleteDir()
            }
          }
        }
      }
    }
  }
  post {
    failure {
      mail to: 'notifications@directory.apache.org',
      subject: "Jenkins pipeline failed: ${currentBuild.fullDisplayName}",
      body: "Jenkins build URL: ${env.BUILD_URL}"
    }
    fixed {
      mail to: 'notifications@directory.apache.org',
      subject: "Jenkins pipeline fixed: ${currentBuild.fullDisplayName}",
      body: "Jenkins build URL: ${env.BUILD_URL}"
    }
  }
}

