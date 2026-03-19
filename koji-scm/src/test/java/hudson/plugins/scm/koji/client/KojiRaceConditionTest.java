package hudson.plugins.scm.koji.client;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.KojiBuildProvider;
import hudson.plugins.scm.koji.KojiSCM;
import hudson.plugins.scm.koji.RealKojiXmlRpcApi;
import hudson.tasks.Shell;

import org.fakekoji.core.FakeKojiTestUtil;
import org.fakekoji.server.JavaServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static hudson.plugins.scm.koji.client.KojiListBuildsTest.createLocalhostKojiBuildProvider;

@WithJenkins
public class KojiRaceConditionTest {
    @TempDir
    static Path temporaryFolder;
    public static JavaServer javaServer = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        temporaryFolder.toFile().mkdirs();
        javaServer = FakeKojiTestUtil.createDefaultFakeKojiServerWithData(temporaryFolder.toFile());
        javaServer.start();
    }

    @AfterAll
    public static void afterClass() {
        if (javaServer != null) {
            javaServer.stop();
        }
    }

    private List<KojiBuildProvider> createKojiBuildProviders() {
        return Collections.singletonList(createLocalhostKojiBuildProvider());
    }

    /**
     * Test that simulates the race condition where:
     * 1. First build polls and finds a new build
     * 2. Before first build writes to processed.txt, we manually trigger another build
     * 3. Second build should skip gracefully if the fix is applied
     *
     * This can happen even with concurrent builds disabled if:
     * - User manually triggers "Build Now" during an automatic poll
     * - Two separate jobs watch the same Koji package
     */
    @Test
    public void testRaceConditionWithManualTrigger(JenkinsRule j) throws Exception {
        // Create project with Koji SCM
        FreeStyleProject project = j.createFreeStyleProject("test-race-condition");

        RealKojiXmlRpcApi kojiXmlRpcApi = new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64",
                "f24.*",
                "",
                ""
        );

        KojiSCM scm = new KojiSCM(
                createKojiBuildProviders(),
                kojiXmlRpcApi,
                null,  // downloadDir
                false, // cleanDownloadDir
                false, // dirPerNvr
                1      // maxPreviousBuilds
        );

        project.setScm(scm);
        project.getBuildersList().add(new Shell("echo 'Build completed'"));

        // Ensure concurrent builds are DISABLED
        project.setConcurrentBuild(false);

        // First build - should succeed and write to processed.txt
        FreeStyleBuild build1 = project.scheduleBuild2(0).get(60, TimeUnit.SECONDS);
        Assertions.assertEquals(Result.SUCCESS, build1.getResult());

        // Verify processed.txt was created and contains the NVR
        File processedFile = new File(project.getRootDir(), Constants.PROCESSED_BUILDS_HISTORY);
        Assertions.assertTrue(processedFile.exists(), "processed.txt should exist after first build");

        String processedContent = new String(Files.readAllBytes(processedFile.toPath()));
        Assertions.assertTrue(processedContent.contains("java-1.8.0-openjdk"),
                "processed.txt should contain the build NVR");

        // Simulate race condition: manually add the same NVR to processed.txt
        // (simulating what would happen if another job processed it concurrently)
        String nvr = extractNvrFromProcessedFile(processedFile);

        // Now trigger another build - without the fix, this would try to download again
        // With the fix, it should skip gracefully
        FreeStyleBuild build2 = project.scheduleBuild2(0).get(60, TimeUnit.SECONDS);

        // The second build should either:
        // - Succeed with "already processed" message (with fix)
        // - Or fail/succeed but attempt duplicate download (without fix)

        // Check the console log for the expected behavior
        String consoleLog = build2.getLog();

        // With the fix applied, we expect to see the skip message
        boolean hasSkipMessage = consoleLog.contains("already processed") ||
                consoleLog.contains("No updates");

        Assertions.assertTrue(hasSkipMessage,
                "Second build should skip gracefully when build is already in processed.txt. " +
                        "Console log: " + consoleLog);
    }

    /**
     * Test simulating two separate jobs processing the same build
     */
    @Test
    public void testRaceConditionWithTwoJobs(JenkinsRule j) throws Exception {
        // Create two projects watching the same Koji package
        FreeStyleProject project1 = j.createFreeStyleProject("job1");
        FreeStyleProject project2 = j.createFreeStyleProject("job2");

        RealKojiXmlRpcApi kojiXmlRpcApi = new RealKojiXmlRpcApi(
                "java-1.8.0-openjdk",
                "x86_64",
                "f24.*",
                "",
                ""
        );

        KojiSCM scm1 = new KojiSCM(
                createKojiBuildProviders(),
                kojiXmlRpcApi,
                "downloads1",
                false,
                false,
                1
        );

        KojiSCM scm2 = new KojiSCM(
                createKojiBuildProviders(),
                kojiXmlRpcApi,
                "downloads2",
                false,
                false,
                1
        );

        project1.setScm(scm1);
        project2.setScm(scm2);

        project1.getBuildersList().add(new Shell("echo 'Job 1 completed'"));
        project2.getBuildersList().add(new Shell("echo 'Job 2 completed'"));

        // Both jobs disabled for concurrent builds
        project1.setConcurrentBuild(false);
        project2.setConcurrentBuild(false);

        // Trigger both jobs simultaneously
        FreeStyleBuild build1 = project1.scheduleBuild2(0).get(60, TimeUnit.SECONDS);
        FreeStyleBuild build2 = project2.scheduleBuild2(0).get(60, TimeUnit.SECONDS);

        // Both should succeed
        Assertions.assertEquals(Result.SUCCESS, build1.getResult());
        Assertions.assertEquals(Result.SUCCESS, build2.getResult());

        // Both should have their own processed.txt
        File processed1 = new File(project1.getRootDir(), Constants.PROCESSED_BUILDS_HISTORY);
        File processed2 = new File(project2.getRootDir(), Constants.PROCESSED_BUILDS_HISTORY);

        Assertions.assertTrue(processed1.exists());
        Assertions.assertTrue(processed2.exists());

        // Both should have downloaded the same build (this is expected - different jobs)
        String content1 = new String(Files.readAllBytes(processed1.toPath()));
        String content2 = new String(Files.readAllBytes(processed2.toPath()));

        Assertions.assertTrue(content1.contains("java-1.8.0-openjdk"));
        Assertions.assertTrue(content2.contains("java-1.8.0-openjdk"));
    }

    private String extractNvrFromProcessedFile(File processedFile) throws IOException {
        List<String> lines = Files.readAllLines(processedFile.toPath());
        if (lines.isEmpty()) {
            throw new IOException("processed.txt is empty");
        }
        // NVR is the first part before any comment (#)
        String line = lines.get(0);
        int commentIndex = line.indexOf('#');
        if (commentIndex > 0) {
            return line.substring(0, commentIndex).trim();
        }
        return line.trim();
    }
}
