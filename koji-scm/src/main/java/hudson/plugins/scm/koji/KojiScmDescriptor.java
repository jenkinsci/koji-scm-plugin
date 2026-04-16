package hudson.plugins.scm.koji;

import hudson.DescriptorExtensionList;
import hudson.scm.SCMDescriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KojiScmDescriptor extends SCMDescriptor<KojiSCM> {

    private static final Logger LOG = LoggerFactory.getLogger(SCMDescriptor.class);
    private boolean KojiSCMConfig_requireWorkspace = true;
    private boolean KojiSCMConfig_skipPoolingIfJobRuns = true;
    private boolean KojiSCMConfig_checkOnCheckout = false;

    public KojiScmDescriptor() {
        super(KojiSCM.class, KojiRepositoryBrowser.class);
        try {
            //this may be killing tests
            load();
        } catch (Exception ex) {
            LOG.error(ex.toString());
            ex.printStackTrace();
        }
    }

    public DescriptorExtensionList<KojiXmlRpcApi, KojiXmlRpcApi.KojiXmlRpcApiDescriptor> getKojiXmlRpcApiDescriptorList() {
        return Jenkins.get().<KojiXmlRpcApi, KojiXmlRpcApi.KojiXmlRpcApiDescriptor>getDescriptorList(KojiXmlRpcApi.class);
    }

    @Override
    public String getDisplayName() {
        return "Koji";
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        this.KojiSCMConfig_requireWorkspace = json.getBoolean("KojiSCMConfig_requireWorkspace");
        this.KojiSCMConfig_skipPoolingIfJobRuns = json.getBoolean("KojiSCMConfig_skipPoolingIfJobRuns");
        this.KojiSCMConfig_checkOnCheckout = json.getBoolean("KojiSCMConfig_checkOnCheckout");
        LOG.debug("KojiSCMConfig_requireWorkspace configured to: " + KojiSCMConfig_requireWorkspace);
        LOG.debug("KojiSCMConfig_skipPoolingIfJobRuns configured to: " + KojiSCMConfig_skipPoolingIfJobRuns);
        LOG.debug("KojiSCMConfig_checkOnCheckout configured to: " + KojiSCMConfig_checkOnCheckout);
        save();
        return true;
    }

    public boolean getKojiSCMConfig_requireWorkspace() {
        LOG.debug("KojiSCMConfig_requireWorkspace returning " + KojiSCMConfig_requireWorkspace);
        return KojiSCMConfig_requireWorkspace;
    }

    public boolean getKojiSCMConfig_skipPoolingIfJobRuns() {
        LOG.debug("KojiSCMConfig_skipPoolingIfJobRuns returning " + KojiSCMConfig_skipPoolingIfJobRuns);
        return KojiSCMConfig_skipPoolingIfJobRuns;
    }

    public boolean getKojiSCMConfig_checkOnCheckout() {
        LOG.debug("KojiSCMConfig_checkOnCheckout returning " + KojiSCMConfig_checkOnCheckout);
        return KojiSCMConfig_checkOnCheckout;
    }

    @DataBoundSetter
    public void setKojiSCMConfig_requireWorkspace(boolean kojiSCMConfig) {
        // TODO implement complex refreshing logic
        LOG.debug("KojiSCMConfig_requireWorkspace set from" + KojiSCMConfig_requireWorkspace + " to " + kojiSCMConfig);
        this.KojiSCMConfig_requireWorkspace = kojiSCMConfig;
    }

    @DataBoundSetter
    public void setKojiSCMConfig_skipPoolingIfJobRuns(boolean kojiSCMConfig) {
        // TODO implement complex refreshing logic
        LOG.debug("KojiSCMConfig_skipPoolingIfJobRuns set from" + KojiSCMConfig_skipPoolingIfJobRuns + " to " + kojiSCMConfig);
        this.KojiSCMConfig_skipPoolingIfJobRuns = kojiSCMConfig;
    }

    @DataBoundSetter
    public void setKojiSCMConfig_checkOnCheckout(boolean kojiSCMConfig) {
        // TODO implement complex refreshing logic
        LOG.debug("KojiSCMConfig_checkOnCheckout set from" + KojiSCMConfig_checkOnCheckout + " to " + kojiSCMConfig);
        this.KojiSCMConfig_checkOnCheckout = kojiSCMConfig;
    }
}
