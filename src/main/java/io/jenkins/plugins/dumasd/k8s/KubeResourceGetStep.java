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
import io.jenkins.plugins.dumasd.k8s.config.K8sResourceNameConfig;
import io.jenkins.plugins.dumasd.k8s.utils.KubernetesUtilityPluginException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.java.Log;
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
@Log
@Getter
@ToString
public class KubeResourceGetStep extends Step implements Serializable {

    private static final long serialVersionUID = 31450466576041991L;
    private String credentialsId;
    private String serverUrl;
    private String caCertificate;
    private List<K8sResourceNameConfig> items;

    @DataBoundConstructor
    public KubeResourceGetStep(@NonNull String credentialsId, @NonNull List<K8sResourceNameConfig> items) {
        this.credentialsId = credentialsId;
        this.items = items;
        checkItems(items);
    }

    @DataBoundSetter
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @DataBoundSetter
    public void setCaCertificate(String caCertificate) {
        this.caCertificate = caCertificate;
    }

    public Descriptor<K8sResourceNameConfig> getItemDescriptor() {
        return Jenkins.get().getDescriptorByType(K8sResourceNameConfig.DescriptorImpl.class);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new KubeResourceGetStepExecution(context, this);
    }

    private void checkItems(List<K8sResourceNameConfig> items) {
        Set<String> set = new HashSet<>();
        for (K8sResourceNameConfig item : items) {
            String resourceId = item.getResourceId();
            if (set.contains(resourceId)) {
                throw new KubernetesUtilityPluginException("[KubeResourceGetStep] Duplicated resource in items.");
            }
            set.add(resourceId);
        }
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
            return "Kubernetes Resource Get Step";
        }

        @Override
        public String getFunctionName() {
            return "kubeResourceGet";
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
