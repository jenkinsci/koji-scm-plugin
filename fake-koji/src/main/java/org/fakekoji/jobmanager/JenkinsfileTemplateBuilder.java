package org.fakekoji.jobmanager;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.*;

import org.fakekoji.jobmanager.model.NamesProvider;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.OToolVariable;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class JenkinsfileTemplateBuilder {

    private String template;
    private final NamesProvider job;

    static final String RUN_PLATFORM_YML= "%{RUN_PLATFORM_YML}";
    static final String TEST_SUITE_URL= "%{TEST_SUITE_URL}";
    static final String TEST_SUITE_BRANCH = "%{TEST_SUITE_BRANCH}";
    static final String EXPORTED_VARIABLES_SINGLELINE = "%{EXPORTED_VARIABLES_SINGLELINE}";

    public JenkinsfileTemplateBuilder(String template, NamesProvider job) {
        this.template = template;
        this.job = job;
    }


    public JenkinsfileTemplateBuilder processVariables(
            Task task,
            //String provider,
            Platform platform,
            List<OToolVariable> exportedVariables) throws IOException {
      //  final Platform.Provider platformProvider = findProvider(provider, platform);
      //  exportedVariables.add(new OToolVariable(PLATFORM_PROVIDER_VAR, platformProvider.getId()));
        if (job != null) {
            if (job.getName() != null) {
                exportedVariables.add(new OToolVariable(JOB_NAME, job.getName()));
            }
            if (job.getShortName() != null) {
                exportedVariables.add(new OToolVariable(JOB_NAME_SHORTENED, job.getShortName()));
            }
        }
        exportedVariables.add(new OToolVariable(TASK_VAR, task.getId()));
        exportedVariables.add(new OToolVariable(OS_VAR, platform.toOsVar()));
        exportedVariables.add(new OToolVariable(OS_NAME_VAR, platform.getOs()));
        exportedVariables.add(new OToolVariable(OS_VERSION_VAR, platform.getVersionNumber()));
        exportedVariables.add(new OToolVariable(ARCH_VAR, platform.getArchitecture()));
        //final  VmWithNodes  mWithNodes = getVmWithNodes(task, platform, exportedVariables, platformProvider);
        //exportedVariables.add(new OToolVariable(VM_NAME_OR_LOCAL_VAR, mWithNodes.vmName));
        template = template.replace(EXPORTED_VARIABLES_SINGLELINE, getExportedVariablesString(exportedVariables));
        return this;
    }

    private String getExportedVariablesString(final List<OToolVariable> exportedVariables) {
        return JenkinsJobTemplateBuilder.getExportedVariablesString(exportedVariables, null);
    }

    private JenkinsfileTemplateBuilder processPlatform(Platform platform) {
        template = template.replace(RUN_PLATFORM_YML, platform.getVmName());
        return this;
    }

    private JenkinsfileTemplateBuilder processTask(Task task) {
        template = template.replace(TASK_SCRIPT, task.getScript())
                .replace(TEST_SUITE_URL,task.getRepository())
                .replace(TEST_SUITE_BRANCH,task.getBranch());
        return this;
    }

    String getTemplate() {
        return template;
    }

    public String expandAll(
            Set<BuildProvider> buildProviders,
            String projectName,
            Map<TaskVariant,TaskVariantValue> buildVariants,
            String buildPlatform,
            Task task,
            String platformProvider,
            Platform platform,
            File scriptsRoot,
            List<OToolVariable> exportedVariables) throws IOException{
        return this.processVariables(task, platform, exportedVariables)
                .processPlatform(platform)
                .processTask(task)
                .getTemplate();
    }


}
