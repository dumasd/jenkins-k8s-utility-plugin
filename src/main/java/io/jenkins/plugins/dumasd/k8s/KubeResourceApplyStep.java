package io.jenkins.plugins.dumasd.k8s;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.dumasd.k8s.config.K8sResourceNameContentConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return null;
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
    }
}
