package io.jenkins.plugins.dumasd.k8s.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.FINE;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.util.Base64;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthConfig;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;

/**
 * @author Bruce.Wu
 * @date 2024-11-15
 */
public class KubernetesClientManager {
    private static final java.util.logging.Logger LOGGER = Logger.getLogger(KubernetesClientManager.class.getName());

    private static final KubernetesClientManager INSTANCE = new KubernetesClientManager();

    public static KubernetesClientManager getInstance() {
        return INSTANCE;
    }

    public KubernetesClient getOrCreate(String serverUrl, String caCertificate, KubernetesAuth auth)
            throws KubernetesAuthException {
        boolean skipTlsVerify = StringUtils.isBlank(caCertificate);
        ConfigBuilder builder;

        if (StringUtils.isBlank(serverUrl)) {
            LOGGER.log(FINE, "Autoconfiguring Kubernetes client");
            builder = new ConfigBuilder(Config.autoConfigure(null));
        } else {
            // Using Config.empty() disables autoconfiguration when both serviceAddress and auth are set
            builder = auth == null ? new ConfigBuilder() : new ConfigBuilder(Config.empty());
            builder = builder.withMasterUrl(serverUrl);
        }

        if (auth != null) {
            builder = auth.decorate(
                    builder, new KubernetesAuthConfig(builder.getMasterUrl(), caCertificate, skipTlsVerify));
            // If authentication is provided, disable autoconfigure flag to deactivate auto refresh
            builder = builder.withAutoConfigure(false);
        }

        if (skipTlsVerify) {
            builder.withTrustCerts(true);
        }

        if (StringUtils.isNotBlank(caCertificate)) {
            builder.withCaCertData(Base64.getEncoder().encodeToString(caCertificate.getBytes(UTF_8)));
        }

        return new KubernetesClientBuilder().withConfig(builder.build()).build();
    }

    private static class Client {
        private final KubernetesClient client;
        private final int validity;

        public Client(int validity, KubernetesClient client) {
            this.client = client;
            this.validity = validity;
        }

        public KubernetesClient getClient() {
            return client;
        }

        public int getValidity() {
            return validity;
        }
    }
}
