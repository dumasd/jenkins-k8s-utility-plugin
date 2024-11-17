package io.jenkins.plugins.dumasd.k8s;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.dumasd.k8s.config.K8sResourceNameContentConfig;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author Bruce.Wu
 * @date 2024-11-14
 */
@Setter
@Getter
@ToString
public class KubeResourceApplyStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

    private String credentialsId;
    private String serverUrl;
    private String caCertificate;
    private List<K8sResourceNameContentConfig> items;

    @DataBoundConstructor
    public KubeResourceApplyStep(@NonNull String credentialsId, List<K8sResourceNameContentConfig> items) {
        this.credentialsId = credentialsId;
        this.items = items;
    }

    @DataBoundSetter
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @DataBoundSetter
    public void setCaCertificate(String caCertificate) {
        this.caCertificate = caCertificate;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new KubeResourceApplyStepExecution(context, this);
    }

    public Descriptor<K8sResourceNameContentConfig> getItemDescriptor() {
        return Jenkins.get().getDescriptorByType(K8sResourceNameContentConfig.DescriptorImpl.class);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> classes = new HashSet<>();
            classes.add(Run.class);
            classes.add(TaskListener.class);
            classes.add(FilePath.class);
            return classes;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Kubernetes Resource Apply Step";
        }

        @Override
        public String getFunctionName() {
            return "kubeResourceApply";
        }

        public ListBoxModel doFillCredentialsIdItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("Select a credential", "");
            for (StandardCredentials c : CredentialsProvider.lookupCredentialsInItemGroup(
                    StandardCredentials.class, Jenkins.get(), null, Collections.emptyList())) {
                items.add(c.getId(), c.getId());
            }
            return items;
        }
    }
}
