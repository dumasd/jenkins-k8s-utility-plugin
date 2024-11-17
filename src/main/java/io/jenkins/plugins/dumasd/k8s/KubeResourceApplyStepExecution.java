package io.jenkins.plugins.dumasd.k8s;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Updatable;
import io.jenkins.plugins.dumasd.k8s.config.K8sResourceNameContentConfig;
import io.jenkins.plugins.dumasd.k8s.model.K8sResourceApplyResult;
import io.jenkins.plugins.dumasd.k8s.utils.KubernetesClientManager;
import io.jenkins.plugins.dumasd.k8s.utils.KubernetesUtilityPluginException;
import io.jenkins.plugins.dumasd.k8s.utils.Logger;
import io.jenkins.plugins.dumasd.k8s.utils.Utils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import jenkins.MasterToSlaveFileCallable;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import lombok.extern.java.Log;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

/**
 * @author Bruce.Wu
 * @date 2024-11-14
 */
@Log
public class KubeResourceApplyStepExecution extends SynchronousNonBlockingStepExecution<List<K8sResourceApplyResult>> {

    private static final long serialVersionUID = 9126925343805160360L;

    private final KubeResourceApplyStep step;

    public KubeResourceApplyStepExecution(StepContext context, KubeResourceApplyStep step) {
        super(context);
        this.step = step;
    }

    @Override
    protected List<K8sResourceApplyResult> run() throws Exception {
        TaskListener taskListener = Objects.requireNonNull(getContext().get(TaskListener.class));
        Logger logger = new Logger("KubeResourceApplyStep", taskListener);
        FilePath workspace = getContext().get(FilePath.class);

        logger.log("=========== Apply kubernetes resources start ===========");
        StandardCredentials credentials = Utils.resolveCredentials(step.getCredentialsId(), Jenkins.get());
        List<K8sResourceApplyResult> applyResults = Objects.requireNonNull(workspace)
                .act(new RemoteCallable(credentials, step.getServerUrl(), step.getCaCertificate(), step.getItems()));

        boolean success = true;
        for (int i = 0; i < applyResults.size(); i++) {
            K8sResourceNameContentConfig config = step.getItems().get(i);
            K8sResourceApplyResult result = applyResults.get(i);
            String status = result.isSuc() ? "SUCCESS" : "FAILURE";
            if (!result.isSuc()) {
                success = false;
            }
            String log = new StringBuilder("**********************************************************")
                    .append(config.getContent())
                    .append("\n")
                    .append(String.format("Status=%s, Message=%s", status, result.getMessage()))
                    .append("\n**********************************************************")
                    .toString();
            taskListener.getLogger().println(log);
        }

        if (!success) {
            logger.log("One or more resources apply failure. Please check the upper console logs!!!!!");
        }
        logger.log("========== Apply kubernetes resources end ==========");

        if (!success) {
            throw new KubernetesUtilityPluginException("One or more resources apply failure");
        }

        return applyResults;
    }

    private static class RemoteCallable extends MasterToSlaveFileCallable<List<K8sResourceApplyResult>> {
        private static final long serialVersionUID = -8239966738051336256L;
        private final String serverUrl;
        private final String caCertificate;
        private final StandardCredentials credentials;
        private final List<K8sResourceNameContentConfig> items;

        private RemoteCallable(
                StandardCredentials credentials,
                String serverUrl,
                String caCertificate,
                List<K8sResourceNameContentConfig> items) {
            this.credentials = credentials;
            this.serverUrl = serverUrl;
            this.caCertificate = caCertificate;
            this.items = items;
        }

        @Override
        public List<K8sResourceApplyResult> invoke(File f, VirtualChannel channel)
                throws IOException, InterruptedException {
            KubernetesAuth kubernetesAuth = AuthenticationTokens.convert(KubernetesAuth.class, credentials);
            try {
                KubernetesClient kubernetesClient = KubernetesClientManager.getInstance()
                        .getOrCreate(Util.fixEmpty(serverUrl), Util.fixEmpty(caCertificate), kubernetesAuth);
                List<K8sResourceApplyResult> applyResults = new ArrayList<>(items.size());

                for (K8sResourceNameContentConfig cfg : items) {
                    K8sResourceApplyResult result = new K8sResourceApplyResult();
                    applyResults.add(result);

                    if (StringUtils.isBlank(cfg.getContent())) {
                        result.ok(String.format(
                                "Content is empty, skip this. apiVersion: %s, kind: %s, name: %s. ",
                                cfg.getApiVersion(), cfg.getKind(), cfg.getName()));
                    } else {
                        ByteArrayInputStream inputStream =
                                new ByteArrayInputStream(cfg.getContent().getBytes(StandardCharsets.UTF_8));
                        HasMetadata hasMetadataObj;
                        try {
                            hasMetadataObj =
                                    kubernetesClient.resource(inputStream).get();
                            hasMetadataObj.getMetadata().setResourceVersion(null);
                        } catch (Exception resourceEx) {
                            log.log(Level.WARNING, "Load resource item warning", resourceEx);
                            result.fail("Only one resource is allowed, or resource yaml is not illegal. message: "
                                    + resourceEx.getMessage());
                            continue;
                        }

                        if (!StringUtils.equalsIgnoreCase(cfg.getApiVersion(), hasMetadataObj.getApiVersion())) {
                            result.fail(String.format(
                                    "ApiVersion not match. %s != %s",
                                    cfg.getApiVersion(), hasMetadataObj.getApiVersion()));
                            continue;
                        }

                        if (!StringUtils.equalsIgnoreCase(cfg.getKind(), hasMetadataObj.getKind())) {
                            result.fail(
                                    String.format("Kind not match. %s != %s", cfg.getKind(), hasMetadataObj.getKind()));
                            continue;
                        }

                        if (!StringUtils.equalsIgnoreCase(
                                cfg.getName(), hasMetadataObj.getMetadata().getName())) {
                            result.fail(String.format(
                                    "Metadata name not match. %s != %s",
                                    cfg.getName(), hasMetadataObj.getMetadata().getName()));
                            continue;
                        }

                        try {
                            kubernetesClient.resource(hasMetadataObj).createOr(Updatable::update);
                            result.ok();
                        } catch (KubernetesClientException e) {
                            log.log(Level.WARNING, "Apply resource warning", e);
                            result.fail(e.getMessage());
                        }
                    }
                }
                return applyResults;
            } catch (KubernetesAuthException e) {
                throw new KubernetesUtilityPluginException(e);
            }
        }
    }
}
