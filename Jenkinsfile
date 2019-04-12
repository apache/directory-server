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
    buildDiscarder(logRotator(numToKeepStr: '3'))
    timeout(time: 8, unit: 'HOURS')
  }
  triggers {
    cron('@weekly')
    pollSCM('@daily')
  }
  stages {
    stage ('Debug') {
      options {
        timeout(time: 1, unit: 'HOURS')
      }
      agent {
        docker {
          label 'ubuntu'
          image 'apachedirectory/maven-build:jdk-8'
          args '-v $HOME/.m2:/var/maven/.m2'
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
            timeout(time: 2, unit: 'HOURS')
          }
          agent {
            docker {
              label 'ubuntu'
              image 'apachedirectory/maven-build:jdk-8'
              args '-v $HOME/.m2:/home/user/.m2'
            }
          }
          steps {
            sh '''
            # Temporary workaround with Surefire on Debian with Java 8 and limit memory usage
            # https://issues.apache.org/jira/browse/SUREFIRE-1588
            # https://stackoverflow.com/questions/53010200/maven-surefire-could-not-find-forkedbooter-class
            export MAVEN_OPTS="-Xmx1024m"
            export _JAVA_OPTIONS="-Djdk.net.URLClassPath.disableClassPathURLCheck=true -Xmx1024m"
            mvn -V clean verify
            '''
          }
          post {
            always {
              //junit '**/target/surefire-reports/*.xml'
              archiveArtifacts artifacts: '**/target/surefire-reports/**'
              deleteDir()
            }
          }
        }
        stage ('Linux Java 11') {
          options {
            timeout(time: 2, unit: 'HOURS')
          }
          agent {
            docker {
              label 'ubuntu'
              image 'apachedirectory/maven-build:jdk-11'
              args '-v $HOME/.m2:/var/maven/.m2'
            }
          }
          steps {
            sh 'mvn -V clean verify'
          }
          post {
            always {
              deleteDir()
            }
          }
        }
        stage ('Linux Java 12') {
          options {
            timeout(time: 2, unit: 'HOURS')
          }
          agent {
            docker {
              label 'ubuntu'
              image 'apachedirectory/maven-build:jdk-12'
              args '-v $HOME/.m2:/var/maven/.m2'
            }
          }
          steps {
            sh 'mvn -V clean verify'
          }
          post {
            always {
              deleteDir()
            }
          }
        }
        stage ('Windows Java 8') {
          options {
            timeout(time: 2, unit: 'HOURS')
          }
          agent {
            label 'Windows'
          }
          steps {
            // TODO: need to investigate test failure on Windows
            bat '''
            set JAVA_HOME=F:\\jenkins\\tools\\java\\latest1.8
            set MAVEN_OPTS="-Xmx512m"
            F:\\jenkins\\tools\\maven\\latest3\\bin\\mvn -V clean verify -DskipTests
            '''
          }
          post {
            always {
              deleteDir()
            }
          }
        }
      }
    }
    stage ('Deploy') {
      options {
        timeout(time: 2, unit: 'HOURS')
      }
      agent {
        label 'ubuntu'
      }
      // https://cwiki.apache.org/confluence/display/INFRA/JDK+Installation+Matrix
      // https://cwiki.apache.org/confluence/display/INFRA/Maven+Installation+Matrix
      // TODO: do not deploy before merged to master
      steps {
        sh '''
        export JAVA_HOME=/home/jenkins/tools/java/latest1.8
        export MAVEN_OPTS="-Xmx512m"
        #/home/jenkins/tools/maven/latest3/bin/mvn -V clean install source:jar deploy
        /home/jenkins/tools/maven/latest3/bin/mvn -V clean install source:jar
        '''
      }
      post {
        always {
          deleteDir()
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
  }
}

