package org.fakekoji.jobmanager;

import static org.fakekoji.DataGenerator.BUILD_PROVIDER_1_DOWNLOAD_URL;
import static org.fakekoji.DataGenerator.BUILD_PROVIDER_1_TOP_URL;
import static org.fakekoji.DataGenerator.BUILD_PROVIDER_2_DOWNLOAD_URL;
import static org.fakekoji.DataGenerator.BUILD_PROVIDER_2_TOP_URL;
import static org.fakekoji.DataGenerator.TEST_PROJECT_NAME;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.O_TOOL;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.RUN_SCRIPT_NAME;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.VAGRANT;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.XML_NEW_LINE;

import org.apache.commons.io.FileUtils;
import org.fakekoji.DataGenerator;
import org.fakekoji.jobmanager.model.NamesProvider;
import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JenkinsfileTemplateBuilderTest {

    private static final TaskVariant jvm = DataGenerator.getJvmVariant();
    private static final TaskVariant debugMode = DataGenerator.getDebugModeVariant();
    private static final TaskVariant jreSdk = DataGenerator.getJreSdk();
    private static final TaskVariant garbageCollector = DataGenerator.getGarbageCollectorCategory();
    private static final TaskVariant displayProtocol = DataGenerator.getDisplayProtocolCategory();
    private static final TaskVariant agent = DataGenerator.getAgent();
    private static final TaskVariant crypto = DataGenerator.getCrypto();
    private static final TaskVariant jfr = DataGenerator.getJfr();

    private static final Map<TaskVariant, TaskVariantValue> buildVariants = DataGenerator.getBuildVariants();
    private static final Map<TaskVariant, TaskVariantValue> testVariants = DataGenerator.getTestVariants();

    private static final Product jdk8Product = DataGenerator.getJDK8Product();
    private static final JDKVersion jdk8 = DataGenerator.getJDKVersion8();

    @TempDir
    static Path temporaryFolder;

    private File scriptsRoot;

    @BeforeEach
    public void setup() throws IOException {
        scriptsRoot = temporaryFolder.toFile();
        FileUtils.deleteDirectory(scriptsRoot);
        scriptsRoot.mkdirs();
    }


    @Test
    public void buildTestJobTemplateOfJDKTestProject() throws IOException {

        final Set<BuildProvider> buildProviders = DataGenerator.getBuildProviders();
        final Task testTask = DataGenerator.getTestTask();
        final Platform buildPlatform = DataGenerator.getF29x64();
        final Platform testPlatform = DataGenerator.getF29x64();

        final TaskVariantValue release = DataGenerator.getReleaseVariant();

        final Map<TaskVariant, TaskVariantValue> buildVariants = new HashMap<TaskVariant, TaskVariantValue>() {{
            put(debugMode, release);
        }};
        final Map<TaskVariant, TaskVariantValue> testVariants = DataGenerator.getTestVariants();

        final List<String> denylist = Arrays.asList(
                "pckgA", "pckgB"
        );
        final List<String> allowlist = Arrays.asList(
                "pckgC", "pckgD"
        );

        final TestJob testJob = new TestJob(
                VAGRANT,
                TEST_PROJECT_NAME,
                Project.ProjectType.JDK_TEST_PROJECT,
                jdk8Product,
                jdk8,
                buildProviders,
                testTask,
                testPlatform,
                testVariants,
                buildPlatform,
                null,
                null,
                buildVariants,
                denylist,
                allowlist,
                scriptsRoot,
                null
        );

        final List<String> expectedDenylist = new ArrayList<>();
        expectedDenylist.addAll(denylist);
        expectedDenylist.addAll(testTask.getRpmLimitation().getDenylist());
        expectedDenylist.addAll(release.getSubpackageDenylist().get());
        final List<String> expectedAllowlist = new ArrayList<>();
        expectedAllowlist.addAll(allowlist);
        expectedAllowlist.addAll(testTask.getRpmLimitation().getAllowlist());
        expectedAllowlist.addAll(release.getSubpackageAllowlist().get());

        final String expectedTemplate = """
                properties([
                    parameters([
                        string(name: 'executionId', defaultValue: UUID.randomUUID().toString(), description: 'Unique execution ID'),
                //build platfrom is missing, Later? Parameter? Hardcoded? Inc triggering commit?
                //run platform
                        string(name: 'ebcShortlist', defaultValue: 'f29-x64', /*eg rh-openjdk-qe-rhel9-jenkins_medium.yml*/
                               description: 'This controls the EBC shortlist to use when provisioning a Jenkins node.'),
                        string(name: 'ecosystemTracking', defaultValue: "",
                               description: 'Tracking Map as string often containing information about CI Orchestrator pipeline'),
                        string(name: 'JENKINS_JOBS_BRANCH', defaultValue: 'dev', description: 'Branch of Jenkins jobs to use for scripts execution'),
                        string(name: 'JENKINS_JOBS_ORG', defaultValue: 'cognitive-software-delivery', description: 'GitHub Org of Jenkins jobs to use for scripts execution'),
                        string(name: 'JDK_DOWNLOAD_URL', defaultValue: '',
                               description: 'URL to download the JDK tarball from (REQUIRED - e.g., passed from TemurinBuildMockup job)'),
                        string(name: 'TEST_SUITE_BRANCH', defaultValue: 'master'/*eg main*/, description: 'Branch to test'),
                        string(name: 'TEST_SUITE_URL',
                               defaultValue: 'git@my.repo', /*eg https://github.com/rh-openjdk/TestHeadlessComponents.git*/
                               description: 'URL to download for prepared suites'),
                        string(name: 'TEST_COMMAND',
                               defaultValue: 'export OTOOL_ARCH="x86_64";export OTOOL_BUILD_ARCH="x86_64";export OTOOL_BUILD_OS="f.29";export OTOOL_BUILD_OS_NAME="f";export OTOOL_BUILD_OS_VERSION="29";export OTOOL_JDK_VERSION="8";export OTOOL_JOB_NAME="tck-jdk8-testProject-f29.x86_64-release-f29.x86_64.vagrant-shenandoah.wayland.fips.lnxagent.jfron";export OTOOL_JOB_NAME_SHORTENED="tck-testProject-r-f29.x86_64.vagrant-swflj-727d50f6cb04218d";export OTOOL_OJDK="jdk8";export OTOOL_OS="f.29";export OTOOL_OS_NAME="f";export OTOOL_OS_VERSION="29";export OTOOL_PACKAGE_NAME="java-1.8.0-openjdk";export OTOOL_PROJECT_NAME="testProject";export OTOOL_TASK="tck";export OTOOL_agent="lnxagent";export OTOOL_crypto="fips";export OTOOL_debugMode="release";export OTOOL_displayProtocol="wayland";export OTOOL_garbageCollector="shenandoah";export OTOOL_jfr="jfron"; export JREJDK=jdk TMPRESULTS=tmpresults ; /path/test.sh ; ls', //eg  bash testHeadlessComponents.sh
                               description: 'Test command to execute on the target machine (TEST_JDK_HOME, JAVA_TO_TEST, and OJDK_VERSION_NUMBER are auto-detected)'),
                //? cleanAndInstallRpms
                        string(name: 'FILE_SERVER', defaultValue: '',
                               description: 'File server where the final results and artifacts will be stored for Cognitive UI'),
                        string(name: 'REPORTING_JVM', defaultValue: 'REDHAT_JDK_17',
                               description: 'The JVM value to report to cognitive')              \s
                    ])
                ])
                
                timestamps {
                    library 'jenkins-ci-websphere'
                
                    if (shouldSkipDueToSeedJob()) {
                        return;
                    }
                    try {
                        println "Requesting node from " + params.ebcShortlist + " with demand ID " + params.executionId \s
                        onEBC(
                            demandId: params.executionId,
                            ebcShortlist: params.ebcShortlist,
                            ebcPriority: params.ebcPriority,
                            autoCompleteAfterXHours: 24
                        ) {
                            stage('testsuite-clone') {
                                reportActivity(name: 'testsuite-clone', executionId: params.executionId) {
                                    println "Cloning the ${params.TEST_SUITE_URL} repo, branch ${params.TEST_SUITE_BRANCH}"
                
                                    git(
                                        branch: "${params.TEST_SUITE_BRANCH}",
                                        url: "${params.TEST_SUITE_URL}",
                                        submoduleCfg: [],  // enables submodule handling
                                        changelog: false,
                                        poll: false
                                    )
                
                                    // initialize & update submodules recursively
                                    sh ""\"
                                        git submodule update --init --recursive
                                    ""\"
                                }
                            }
                
                            stage('jvm-setup') {
                                reportActivity(name: 'jvm-setup', executionId: params.executionId) {
                                    withCredentials([usernamePassword(credentialsId: "intranetId", usernameVariable: 'intranetId_USR', passwordVariable: 'intranetId_PSW')]) {
                                        if (!params.JDK_DOWNLOAD_URL || params.JDK_DOWNLOAD_URL == '') {
                                            error("JDK_DOWNLOAD_URL parameter is required but was not provided")
                                        }
                
                                        println "Downloading JDK from: ${params.JDK_DOWNLOAD_URL}"
                                        sh ""\"
                                            set -e
                                            mkdir -p ~/jdk-downloads
                                            cd ~/jdk-downloads
                
                                            # Download the JDK tarball
                                            curl -L -o jdk-download.tar.gz "${params.JDK_DOWNLOAD_URL}"
                
                                            # Extract the JDK
                                            tar -xzf jdk-download.tar.gz
                
                                            # Find the extracted JDK directory
                                            JDK_DIR=\\$(find . -maxdepth 1 -type d -name "jdk*" | head -n 1)
                
                                            if [ -z "\\${JDK_DIR}" ]; then
                                                echo "ERROR: Could not find extracted JDK directory"
                                                exit 1
                                            fi
                
                                            # Get absolute path
                                            JDK_DIR=\\$(cd "\\${JDK_DIR}" && pwd)
                
                                            # Verify the JDK
                                            "\\${JDK_DIR}/bin/java" -version
                
                                            echo "JDK downloaded and extracted to: \\${JDK_DIR}"
                
                                            # Auto-detect JDK version
                                            JAVA_VERSION=\\$("\\${JDK_DIR}/bin/java" -version 2>&1 | head -n 1 | awk -F '"' '{print \\$2}')
                                            OJDK_VERSION_NUMBER=\\$(echo "\\${JAVA_VERSION}" | awk -F '.' '{print \\$1}')
                
                                            # Export environment variables for subsequent stages
                                            echo "export TEST_JDK_HOME=\\${JDK_DIR}" > ~/jdk-env.sh
                                            echo "export JAVA_TO_TEST=\\${JDK_DIR}/bin/java" >> ~/jdk-env.sh
                                            echo "export OJDK_VERSION_NUMBER=\\${OJDK_VERSION_NUMBER}" >> ~/jdk-env.sh
                
                                            echo ""
                                            echo "=========================================="
                                            echo "Auto-detected JDK configuration:"
                                            echo "  TEST_JDK_HOME=\\${JDK_DIR}"
                                            echo "  JAVA_TO_TEST=\\${JDK_DIR}/bin/java"
                                            echo "  OJDK_VERSION_NUMBER=\\${OJDK_VERSION_NUMBER}"
                                            echo "=========================================="
                                        ""\"
                                    }
                                }
                            }
                
                            stage('testsuite-run') {
                                reportActivity(name: 'testsuite-run', executionId: params.executionId) {
                                    withCredentials([usernamePassword(credentialsId: "intranetId", usernameVariable: 'intranetId_USR', passwordVariable: 'intranetId_PSW')]) {
                                        sh ""\"
                                            # Source the auto-detected JDK environment variables
                                            source ~/jdk-env.sh
                
                                            # Execute the test command
                                            ${params.TEST_COMMAND}
                                        ""\"
                                    }
                                }
                            }
                
                            stage('Test result upload') {
                                withCredentials([usernamePassword(credentialsId: 'artifactoryToken', usernameVariable: 'artifactory_username', passwordVariable: 'artifactory_password')]) {
                                    sh ""\"
                                        # Assign credentials to bash variables
                                        USERNAME="\\$artifactory_username"
                                        PASSWORD="\\$artifactory_password"
                
                                        # Set up upload parameters
                                        EXECUTION_ID="${params.executionId}"
                                        ARTIFACTORY_URL="https://eu.artifactory.swg-devops.com/artifactory/rhh-team-openjdk-qe-generic-local/\\${EXECUTION_ID}/"
                                        FILE_PATH="$HOME/Build/workspace/%{TEST_SUITE_RESULTS_ARCHIVE_STUB}"
                
                                        # Upload file to Artifactory
                                        curl -u "\\${USERNAME}:\\${PASSWORD}" -T "\\${FILE_PATH}" "\\${ARTIFACTORY_URL}"
                                    ""\"
                                }
                            }
                        }
                
                    } finally {
                        node (label: 'built-in') {
                            cleanupWorkspace()
                        }
                    }
                }   \s
                
                """;

        final String actualTemplate = testJob.generateJenkinsfile();
        //there was so much random empty-filled lines that we msut drop them...
        Assertions.assertEquals(
                expectedTemplate.lines().map(s-> s.isBlank()?s.trim():s).collect(Collectors.joining("\n")),
                actualTemplate.lines().map(s->s.isBlank()?s.trim():s).collect(Collectors.joining("\n")));
    }


}
