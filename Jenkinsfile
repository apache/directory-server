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
  stages {
    stage ('Compile') {
      parallel {
        stage ('Compile Java 8') {
          agent {
            docker 'maven:3-jdk-8'
          }
          steps {
            sh 'mvn -V clean verify -DskipTests'
          }
        }
        stage ('Build Java 11') {
          agent {
            docker 'maven:3-jdk-11'
          }
          steps {
            sh 'mvn -V clean verify -DskipTests'
          }
        }
      }
    }
    stage ('Test') {
      parallel {
        stage ('Test Java 8') {
          agent {
            docker 'maven:3-jdk-8'
          }
          steps {
            sh 'mvn -V clean verify'
          }
          post {
            always {
              junit '**/target/surefire-reports/*.xml'
            }
          }
        }
        stage ('Test Java 11') {
          agent {
            docker 'maven:3-jdk-11'
          }
          steps {
            sh 'mvn -V clean verify'
          }
        }
      }
    }
  }
}

