pipeline {
	agent mulesoft
	
	stages {
	  stage ('prebuild') {
	  	steps {
	    	git credentialsId: 'c8d0bb4e-36db-4983-a29f-35e9f7878869', url: 'https://github.com/devopsstk/sabreDemo.git'
	    }
	  }
	  stage ('build') {
	  	steps {
	  		sh 'mvn clean install -DskipTests'
	  		archive "target/*.zip"
	  	}
	  }
	  stage ('test') {
	  	steps {
	  		sh 'mvn test'
	  	}
	  	post {
			always {
			    junit 'target/surefire-reports/*.xml'
			    archive "target/munit-reports/coverage/**"
			}
		  }
	  }
	  stage ('deploy') {
	  	steps {
	  		sh 'sudo anypoint-cli --username=jorgegonzales --password=Monster_j5 runtime-mgr cloudhub-application modify softtek-mule-demo-app ${WORKSPACE}/target/softtek-demo-1.0.0-SNAPSHOT.zip'
	  	}
	  }
	  stage ('artifactory') {
	  	steps {
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
