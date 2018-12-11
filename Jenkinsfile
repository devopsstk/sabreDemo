pipeline {
  agent {
    label 'mulesoft'
  }
  stages {
    stage('prebuild') {
      steps {
        git(credentialsId: 'c8d0bb4e-36db-4983-a29f-35e9f7878869', url: 'https://github.com/devopsstk/sabreDemo.git')
      }
    }
    stage('build') {
      steps {
        sh 'mvn clean install -DskipTests'
        archive 'target/*.zip'
        stash(includes: 'docker/**/**', name: 'docker')
        stash(includes: 'target/**/**', name: 'target')
      }
    }
    stage('test') {
      steps {
        sh 'mvn test'
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
          publishHTML(allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: 'target/munit-reports/coverage', reportFiles: 'summary.html', reportName: 'MUnit Coverage Report', reportTitles: '')
          
        }
        
      }
    }
    stage('docker build') {
      agent {
        label 'docker'
      }
      steps {
        unstash 'docker'
        unstash 'target'
        sh 'mv target/*.zip docker/'
        script {
          wrap([$class: 'AmazonAwsCliBuildWrapper',
          credentialsId: 'awsCloud',
          defaultRegion: 'us-west-1']) {
            
            sh '''
$(aws ecr get-login --no-include-email  --region us-west-1)
docker build -t demoapp docker
docker tag demoapp:latest 792971870453.dkr.ecr.us-west-1.amazonaws.com/demoapp:v_${BUILD_NUMBER}
docker push 792971870453.dkr.ecr.us-west-1.amazonaws.com/demoapp:v_${BUILD_NUMBER}
'''
          }
        }
        
      }
    }
    stage('deploy') {
      steps {
        parallel(
          "aws": {
          	node ('docker') {
	            unstash 'docker'
	            script {
	              wrap([$class: 'AmazonAwsCliBuildWrapper',
	              credentialsId: 'awsCloud',
	              defaultRegion: 'us-west-1']) {
	                
	                sh '''#!/bin/bash
REGION=us-west-1
REPOSITORY_NAME=demoapp
CLUSTER=cludbeesAgents
FAMILY=demoapp-dev
NAME=demoapp-dev
SERVICE_NAME=${NAME}-service
#Store the repositoryUri as a variable
REPOSITORY_URI=`aws ecr describe-repositories --repository-names ${REPOSITORY_NAME} --region ${REGION} | jq .repositories[].repositoryUri | tr -d '"'`
#Replace the build number and respository URI placeholders with the constants above
sed -e "s;%BUILD_NUMBER%;${BUILD_NUMBER};g" -e "s;%REPOSITORY_URI%;${REPOSITORY_URI};g" docker/taskdef.json > ${NAME}-v_${BUILD_NUMBER}.json
#Register the task definition in the repository
aws ecs register-task-definition --family ${FAMILY} --cli-input-json file://${WORKSPACE}/${NAME}-v_${BUILD_NUMBER}.json --region ${REGION}
SERVICES=`aws ecs describe-services --services ${SERVICE_NAME} --cluster ${CLUSTER} --region ${REGION} | jq .failures[]`
#Get latest revision
REVISION=`aws ecs describe-task-definition --task-definition ${NAME} --region ${REGION} | jq .taskDefinition.revision`
#Create or update service
echo $SERVICE
if [ "$SERVICES" == "" ]; then
	echo "entered existing service"
	DESIRED_COUNT=`aws ecs describe-services --services ${SERVICE_NAME} --cluster ${CLUSTER} --region ${REGION} | jq .services[].desiredCount`
	if [ ${DESIRED_COUNT} = "0" ]; then
		DESIRED_COUNT="1"
	fi
        echo $SERVICE_NAME
	aws ecs update-service --cluster ${CLUSTER} --region ${REGION} --service ${SERVICE_NAME} --task-definition ${FAMILY}:${REVISION} --desired-count ${DESIRED_COUNT}  
else
	echo "entered new service"
	aws ecs create-service --service-name ${SERVICE_NAME} --desired-count 1 --task-definition ${FAMILY} --cluster ${CLUSTER} --region ${REGION}  --role "ecsServiceRole" --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:us-west-1:792971870453:targetgroup/demoapp-dev/ae3425a9f5f2f5ec,containerName=demoapp,containerPort=8081" 
fi
'''
			    }	
              }
            }
            
            
          },
          "cloudhub": {
            sh 'sudo anypoint-cli --username=msardinas1 --password=Sardinas1 runtime-mgr cloudhub-application modify softtek-mule-demo-app ${WORKSPACE}/target/softtek-demo-1.0.0-SNAPSHOT.zip'
            script {
            	slackSend(channel: '#demo_deploy', color: 'good', message: "Deployment to Sandbox environment completed (<${env.RUN_DISPLAY_URL}|Open Build #${env.BUILD_NUMBER}>)", teamDomain: 'coedevops', token: 'E01HyRsfgvEcsNkXzqIQZhP7')
            }
            
          }
        )
      }
    }
    stage('artifactory') {
      steps {
        script {
          def server = Artifactory.server 'artifactory'
          def buildInfo = Artifactory.newBuildInfo()
          
          def artifactoryUploadDsl = """{
            "files": [
              {
                "pattern": "target/*.zip",
                "target": "sabre/demo/"
              }
            ]
          }"""
          
          server.upload(artifactoryUploadDsl, buildInfo)
          server.publishBuildInfo(buildInfo)
        }
        
      }
    }
  }
  post {
    failure {
  	  script {
      	slackSend(channel: '#demo_deploy', color: 'bad', message: "There is an error on the build job (<${env.RUN_DISPLAY_URL}|Open Build #${env.BUILD_NUMBER}>)", teamDomain: 'coedevops', token: 'E01HyRsfgvEcsNkXzqIQZhP7')
      }
      
    }
    
  }
}

