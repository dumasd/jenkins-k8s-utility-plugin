package io.jenkins.plugins.dumasd.k8s;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.jenkins.plugins.dumasd.k8s.config.K8sResourceNameConfig;
import io.jenkins.plugins.dumasd.k8s.config.K8sResourceNameContentConfig;
import io.jenkins.plugins.dumasd.k8s.utils.KubernetesClientManager;
import io.jenkins.plugins.dumasd.k8s.utils.KubernetesUtilityPluginException;
import io.jenkins.plugins.dumasd.k8s.utils.Logger;
import io.jenkins.plugins.dumasd.k8s.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jenkins.MasterToSlaveFileCallable;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

/**
 * @author Bruce.Wu
 * @date 2024-11-14
 */
public class KubeResourceGetStepExecution
        extends SynchronousNonBlockingStepExecution<List<K8sResourceNameContentConfig>> {

    private static final long serialVersionUID = 5173204850494332506L;
    private final KubeResourceGetStep step;

    public KubeResourceGetStepExecution(StepContext context, KubeResourceGetStep step) {
        super(context);
        this.step = step;
    }

    @Override
    protected List<K8sResourceNameContentConfig> run() throws Exception {
        TaskListener taskListener = getContext().get(TaskListener.class);
        Logger logger = new Logger("KubeResourceGetStep", taskListener);
        FilePath workspace = getContext().get(FilePath.class);
        logger.log("=========== Get kubernetes resources start ===========");
        StandardCredentials credentials = Utils.resolveCredentials(step.getCredentialsId(), Jenkins.get());
        List<K8sResourceNameContentConfig> result = Objects.requireNonNull(workspace)
                .act(new RemoteCallable(credentials, step.getServerUrl(), step.getCaCertificate(), step.getItems()));
        logger.log("=========== Get kubernetes resources end ===========");
        return result;
    }

    private static class RemoteCallable extends MasterToSlaveFileCallable<List<K8sResourceNameContentConfig>> {
        private static final long serialVersionUID = -6580362358698705142L;
        private final String serverUrl;
        private final String caCertificate;
        private final StandardCredentials credentials;
        private final List<K8sResourceNameConfig> items;

        private RemoteCallable(
                StandardCredentials credentials,
                String serverUrl,
                String caCertificate,
                List<K8sResourceNameConfig> items) {
            this.credentials = credentials;
            this.serverUrl = serverUrl;
            this.caCertificate = caCertificate;
            this.items = items;
        }

        @Override
        public List<K8sResourceNameContentConfig> invoke(File f, VirtualChannel channel)
                throws IOException, InterruptedException {
            KubernetesAuth kubernetesAuth = AuthenticationTokens.convert(KubernetesAuth.class, credentials);
            try {
                KubernetesClient kubernetesClient = KubernetesClientManager.getInstance()
                        .getOrCreate(Util.fixEmpty(serverUrl), Util.fixEmpty(caCertificate), kubernetesAuth);
                List<K8sResourceNameContentConfig> nameContentConfigs = new ArrayList<>(items.size());
                for (K8sResourceNameConfig cfg : items) {
                    MixedOperation<
                                    GenericKubernetesResource,
                                    GenericKubernetesResourceList,
                                    Resource<GenericKubernetesResource>>
                            operation = kubernetesClient.genericKubernetesResources(cfg.getApiVersion(), cfg.getKind());
                    GenericKubernetesResourceList resourceList;
                    if (StringUtils.isNotBlank(cfg.getNamespace())) {
                        resourceList = operation
                                .inNamespace(cfg.getNamespace())
                                .withField("metadata.name", cfg.getName())
                                .list();
                    } else {
                        resourceList = operation
                                .withField("metadata.name", cfg.getName())
                                .list();
                    }

                    if (resourceList.getItems().size() > 1) {
                        throw new IllegalArgumentException("Found multi resources using: " + cfg);
                    }

                    K8sResourceNameContentConfig nameContentConfig =
                            new K8sResourceNameContentConfig(cfg.getApiVersion(), cfg.getKind(), cfg.getName());

                    if (resourceList.getItems().isEmpty()) {
                        nameContentConfig.setContent("");
                    } else {
                        try {
                            GenericKubernetesResource resource =
                                    resourceList.getItems().get(0);
                            resource.setKind(cfg.getKind());
                            resource.setApiVersion(cfg.getApiVersion());
                            resource.getMetadata().setManagedFields(null);
                            resource.getMetadata().setFinalizers(null);
                            resource.getMetadata().setCreationTimestamp(null);
                            resource.getMetadata().setGeneration(null);
                            resource.getMetadata().setResourceVersion(null);
                            resource.getMetadata().setUid(null);
                            resource.getMetadata()
                                    .getAnnotations()
                                    .remove("kubectl.kubernetes.io/last-applied-configuration");
                            resource.getMetadata().getAnnotations().remove("deployment.kubernetes.io/revision");
                            resource.getAdditionalProperties().remove("status");
                            Object spec = resource.getAdditionalProperties().get("spec");
                            if (Objects.nonNull(spec)) {
                                ((Map) spec).remove("revisionHistoryLimit");
                            }
                            String content = Utils.YAML_MAPPER.writeValueAsString(resource);
                            content = content.replace("---", "");
                            nameContentConfig.setContent(content);
                        } catch (JsonProcessingException ex) {
                            throw new KubernetesUtilityPluginException(ex);
                        }
                    }
                    nameContentConfig.setNamespace(cfg.getNamespace());
                    nameContentConfigs.add(nameContentConfig);
                }
                return nameContentConfigs;

            } catch (KubernetesAuthException e) {
                throw new KubernetesUtilityPluginException(e);
            }
        }
    }
}
