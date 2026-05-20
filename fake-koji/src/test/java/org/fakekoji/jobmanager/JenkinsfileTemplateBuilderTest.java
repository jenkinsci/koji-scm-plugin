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


    public static final String SHRT_NM = "ShrtNm";
    public static final String LONG_NAME = "Long-Name";

    private static final NamesProvider dummyNamesProvider = new NamesProvider() {
        @Override
        public String getName() {
            return LONG_NAME;
        }

        @Override
        public String getShortName() {
            return SHRT_NM;
        }
    };

    private static final String BUILD_PROVIDERS_TEMPLATE = "        <kojiBuildProviders class=\"list\">\n" +
            "            <hudson.plugins.scm.koji.KojiBuildProvider>\n" +
            "                <buildProvider>\n" +
            "                    <topUrl>" + BUILD_PROVIDER_1_TOP_URL + "</topUrl>\n" +
            "                    <downloadUrl>" + BUILD_PROVIDER_1_DOWNLOAD_URL + "</downloadUrl>\n" +
            "                </buildProvider>\n" +
            "            </hudson.plugins.scm.koji.KojiBuildProvider>\n" +
            "            <hudson.plugins.scm.koji.KojiBuildProvider>\n" +
            "                <buildProvider>\n" +
            "                    <topUrl>" + BUILD_PROVIDER_2_TOP_URL + "</topUrl>\n" +
            "                    <downloadUrl>" + BUILD_PROVIDER_2_DOWNLOAD_URL + "</downloadUrl>\n" +
            "                </buildProvider>\n" +
            "            </hudson.plugins.scm.koji.KojiBuildProvider>\n" +
            "        </kojiBuildProviders>\n";


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
                //hardcoded
                        string(name: 'executionId', defaultValue: UUID.randomUUID().toString(), description: 'Unique execution ID'),
                //build platfrom is missing, Later? Parameter? Hardcoded? Inc triggering commit?
                //run platform
                        string(name: 'ebcShortlist', defaultValue: 'f29-x64', /*eg rh-openjdk-qe-rhel9-jenkins_medium.yml*/
                               description: 'This controls the EBC shortlist to use when provisioning a Jenkins node.'),
                //hardcoded
                        string(name: 'ecosystemTracking', defaultValue: "",
                               description: 'Tracking Map as string often containing information about CI Orchestrator pipeline'),
                //hardcoded
                        string(name: 'JENKINS_JOBS_BRANCH', defaultValue: 'dev', description: 'Branch of Jenkins jobs to use for scripts execution'),
                //hardcoded
                        string(name: 'JENKINS_JOBS_ORG', defaultValue: 'cognitive-software-delivery', description: 'GitHub Org of Jenkins jobs to use for scripts execution'),
                //generated, but hardcoded
                        string(name: 'TEST_SUITE_BRANCH', defaultValue: 'master'/*eg main*/, description: 'Branch to test'),
                //generated
                        string(name: 'TEST_SUITE_URL',
                               defaultValue: 'git@my.repo', /*eg https://github.com/rh-openjdk/TestHeadlessComponents.git*/
                               description: 'URL to download for prepared suites'),
                //there should be OTOOL_VARS?
                        string(name: 'TEST_COMMAND',
                               defaultValue: 'export OTOOL_ARCH="x86_64";export OTOOL_BUILD_ARCH="x86_64";export OTOOL_BUILD_OS="f.29";export OTOOL_BUILD_OS_NAME="f";export OTOOL_BUILD_OS_VERSION="29";export OTOOL_JDK_VERSION="8";export OTOOL_JOB_NAME="tck-jdk8-testProject-f29.x86_64-release-f29.x86_64.vagrant-shenandoah.wayland.fips.lnxagent.jfron";export OTOOL_JOB_NAME_SHORTENED="tck-testProject-r-f29.x86_64.vagrant-swflj-727d50f6cb04218d";export OTOOL_OJDK="jdk8";export OTOOL_OS="f.29";export OTOOL_OS_NAME="f";export OTOOL_OS_VERSION="29";export OTOOL_PACKAGE_NAME="java-1.8.0-openjdk";export OTOOL_PROJECT_NAME="testProject";export OTOOL_TASK="tck";export OTOOL_agent="lnxagent";export OTOOL_crypto="fips";export OTOOL_debugMode="release";export OTOOL_displayProtocol="wayland";export OTOOL_garbageCollector="shenandoah";export OTOOL_jfr="jfron"; export TEST_JDK_HOME=/usr/lib/jvm/java-17-openjdk JAVA_TO_TEST=/usr/lib/jvm/java-17-openjdk/bin/java OJDK_VERSION_NUMBER=17 JREJDK=jdk TMPRESULTS=tmpresults ; /path/test.sh ; ls', //eg  bash testHeadlessComponents.sh
                               description: 'Test command to execute on the target machine'),
                //eg cleanAndInstallRpms
                //lets stay with dnf isntall for now
                //maybe jsut download and save to rpms (becasue of portables and clanAndINstall)?
                        string(name: 'GET_JAVA_COMMAND',
                               defaultValue: 'sudo dnf install -y java-17-openjdk-devel',
                               description: 'The command to get jdk installed.'),
                //those threee may be ignored
                        string(name: 'FILE_SERVER', defaultValue: '',
                               description: 'File server where the final results and artifacts will be stored for Cognitive UI'),
                        string(name: 'JVM_UNDER_TEST_PATH', defaultValue: '',
                               description: 'The location on the fileserver to find the java we want to test against'),
                        string(name: 'REPORTING_JVM', defaultValue: 'REDHAT_JDK_17',
                               description: 'The JVM value to report to cognitive')
                    ])
                ])
                
                
                //mandatory
                timestamps {
                    library 'jenkins-ci-websphere'
                
                    if (shouldSkipDueToSeedJob()) {
                        return;
                    }
                    println "Now you see me!"
                    try {
                        println "Requesting node from " + params.ebcShortlist + " with demand ID " + params.executionId
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
                                        sh params.GET_JAVA_COMMAND
                                    }
                                }
                            }
                
                            stage('testsuite-run') {
                                reportActivity(name: 'testsuite-run', executionId: params.executionId) {
                                    withCredentials([usernamePassword(credentialsId: "intranetId", usernameVariable: 'intranetId_USR', passwordVariable: 'intranetId_PSW')]) {
                                        sh params.TEST_COMMAND
                                    }
                                }
                            }
                        }
                
                    } finally {
                        node (label: 'built-in') {
                            cleanupWorkspace()
                        }
                    }
                }
                """;

        final String actualTemplate = testJob.generateJenkinsfile();
        Assertions.assertEquals(expectedTemplate, actualTemplate);
    }


}
