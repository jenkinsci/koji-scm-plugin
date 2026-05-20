#!/bin/bash

## resolve folder of this script, following all symlinks,
## http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SCRIPT_SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SCRIPT_SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
  SCRIPT_SOURCE="$(readlink "$SCRIPT_SOURCE")"
  # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
  [[ $SCRIPT_SOURCE != /* ]] && SCRIPT_SOURCE="$SCRIPT_DIR/$SCRIPT_SOURCE"
done
readonly SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"

set -exo pipefail
cd $SCRIPT_DIR
DEMO_DIR=$SCRIPT_DIR/anoterGeneratedOutput.demo
rm  -rf $DEMO_DIR
mkdir $DEMO_DIR

if [ ! -e  $SCRIPT_DIR/fake-koji/target/fake-koji-jar-with-dependencies.jar ] ; then
  mvn clean install -DskipTests
fi
cp -v  $SCRIPT_DIR/fake-koji/target/fake-koji-jar-with-dependencies.jar $DEMO_DIR/otool.jar
 
cat << EOF > $DEMO_DIR/otool.properties
port.file.download=9849
port.xml.rpc=9848
port.ssh=9822
url.jenkins=http://localhost:8080/
jenkins.ssh.host=localhost
jenkins.ssh.port=9999
jenkins.ssh.user=otool
jenkins.ssh.keypath=/$HOME/.ssh/otool_rsa_jenkins
url.comparator=http://localhost:9090/comp.html
port.webapp=8888
root.repos=$DEMO_DIR/upstream-repos/
root.build.db=$DEMO_DIR/local-builds/
root.jenkins.jobs=$DEMO_DIR/jobs/
root.jenkins.job.archive=$DEMO_DIR/job-archive/
root.configs=$DEMO_DIR/configs/
root.scripts=$DEMO_DIR/TckScripts/
report.exec.defaultparams=--surpass best --cacheUrl http://localhost:8888/misc/resultsDb/set?nvr={NVR}&job={JOB}&buildId={BUILDID}&score={SCORE}
report.exec.defaultchartparams=--wipecharts true --interpolate true
root.jenkinsfiles=$DEMO_DIR/jenkinsfiles/
EOF
#root.jenkinsfiles=$DEMO_DIR/jenkinsfiles/ is NEW!!!

CONFIGS=$DEMO_DIR/configs
mkdir $DEMO_DIR/local-builds/
mkdir $DEMO_DIR/upstream-repos/
mkdir $CONFIGS
mkdir $DEMO_DIR/jobs/
mkdir $DEMO_DIR/job-archive/
mkdir $DEMO_DIR/TckScripts/
mkdir $DEMO_DIR/jenkinsfiles/

mkdir $CONFIGS/buildProviders
mkdir $CONFIGS/jdkProjects
mkdir $CONFIGS/jdkTestProjects
mkdir $CONFIGS/jdkVersions
mkdir $CONFIGS/platforms
mkdir $CONFIGS/tasks
mkdir $CONFIGS/taskVariants

cat << EOF >$CONFIGS/platforms/el9.x86_64.json
{
  "id" : "el9.x86_64",
  "os" : "el",
  "version" : "9",
  "versionNumber" : "9",
  "architecture" : "x86_64",
  "kojiArch" : "x86_64",
    "providers" : [ {
    "id" : "ibm",
    "hwNodes" : [ ],
    "vmNodes" : [ "fyre" ]
  } ],
  "vmName" : "rh-openjdk-qe-rhel9-jenkins_medium.yml",
  "testingYstream" : "True",
  "stableZstream" : "False",
  "tags" : [ ],
  "variables" : [ ],
  "osVersion" : "el9"
}
EOF
cat << EOF >$CONFIGS/tasks/headless~components.json
{
  "id" : "headless~components",
  "script" : "export TEST_JDK_HOME=/usr/lib/jvm/java-17-openjdk JAVA_TO_TEST=/usr/lib/jvm/java-17-openjdk/bin/java OJDK_VERSION_NUMBER=17 JREJDK=jdk TMPRESULTS=tmpresults ; bash testHeadlessComponents.sh ; ls",
  "repository" : "https://github.com/rh-openjdk/TestHeadlessComponents.git",
  "branch" : "main",
  "type" : "TEST",
  "scmPollSchedule" : "H */5 * * *",
  "machinePreference" : "VM",
  "productLimitation" : {
    "list" : [ ],
    "flag" : "NONE"
  },
  "platformLimitation" : {
    "list" : [ ],
    "flag" : "NONE"
  },
  "fileRequirements" : {
    "source" : false,
    "noarch" : false,
    "binary" : "BINARY"
  },
  "xmlTemplate" : " ",
  "xmlViewTemplate" : "",
  "rpmLimitation" : {
    "denylist" : [ ".*win.debuginfo.*" ],
    "allowlist" : [ ]
  },
  "variables" : [ ],
  "timeoutInHours" : 24
}
EOF
cat << EOF >$CONFIGS/jdkVersions/jp17.json
{
  "id": "jp17",
  "label": "JDK 17",
  "version": "17",
  "packageNames": [
    "java-17-openjdk",
    "java-17-openjdk-portable"
  ]
}
EOF
cat << EOF >$CONFIGS/jdkTestProjects/ojdk17~rpms.json
{
  "id" : "ojdk17~rpms",
  "product" : {
    "jdk" : "jp17",
    "packageName" : "java-17-openjdk"
  },
  "type" : "JDK_TEST_PROJECT",
  "buildProviders" : [ "ibm" ],
  "variables" : [ ],
  "subpackageDenylist" : [ ".*accessibility.*", ".*demo.*", ".*openjfx.*" ],
  "subpackageAllowlist" : [ ],
  "jobConfiguration" : {
    "platforms" : [ {
      "id" : "el9.x86_64",
      "variants" : [ {
        "map" : {
          "debugMode" : "release",
          "jresdk" : "sdk"
        },
        "platforms" : [ {
          "id" : "el9.x86_64",
          "tasks" : [ {
            "id" : "headless~components",
            "variants" : [ {
              "map" : {
                "displayProtocol" : "x11",
                "garbageCollector" : "defaultgc",
                "cryptosetup" : "legacy",
                "agent" : "lnxagent",
                "jfr" : "jfroff"
              },
              "platforms" : [ ]
            } ]
          } ],
          "provider" : "ibm"
        } ]
      } ]
    } ]
  }
}
EOF
cat << EOF >$CONFIGS/buildProviders/ibm.json
{
  "id": "ibm",
  "label": "ibm-unused",
  "topUrl": "unused",
  "downloadUrl": "unused",
  "packageInfoUrl": "unused"
}
EOF

scp -r tester@hydra.lab.eng.brq2.redhat.com:/mnt/raid/configs/taskVariants  $CONFIGS/

java -Xmx2G -cp $DEMO_DIR/otool.jar org.fakekoji.server.JavaServer $DEMO_DIR/otool.properties &
MAIN_PID=$!
sleep 2
set +x
echo "***************************************************************"
echo "*** Main server is running, kill $MAIN_PID to cleanm it up  ***"
echo "***  Now sleeping before running the generate all command   ***"
echo "***************************************************************"
set -x
sleep 2
curl -s "http://localhost:8888/misc/regenerateAll/jdkTestProjects?project=ALL&allowlist=.*.*"
ls -l $DEMO_DIR/jenkinsfiles/
more $DEMO_DIR/jenkinsfiles/headless~components-jp17-ojdk17~rpms-el9.x86_64-release.sdk-el9.x86_64.ibm-x11.defaultgc.legacy.lnxagent.jfroff.jenkinsfile
echo "Is it ok?"
find $DEMO_DIR/jenkinsfiles/
set +x
echo "********************************************************************"
echo "*** http://localhost:8888/  still running, kill it as  $MAIN_PID ***"
echo "********************************************************************"
