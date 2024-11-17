package io.jenkins.plugins.dumasd.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Updatable;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Bruce.Wu
 * @date 2024-11-15
 */
public class KubernetesClientTests {

    private static final Logger logger = Logger.getLogger(KubernetesClientTests.class.getName());

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    @Test
    public void test() throws Exception {
        String masterUrl = System.getenv("TEST_MASTER_URL");
        String token = System.getenv("TEST_TOKEN");
        String caCert = System.getenv("TEST_CA_CERT");

        if (StringUtils.isBlank(masterUrl) || StringUtils.isBlank(token) || StringUtils.isBlank(caCert)) {
            return;
        }

        ConfigBuilder configBuilder = new ConfigBuilder(Config.empty());
        configBuilder
                .withMasterUrl(masterUrl)
                .withOauthToken(token)
                .withTrustCerts(false)
                .withCaCertData(caCert);

        KubernetesClient kubernetesClient =
                new KubernetesClientBuilder().withConfig(configBuilder.build()).build();

        KubernetesResourceList<GenericKubernetesResource> resourceList = kubernetesClient
                .genericKubernetesResources("apps/v1", "Deployment")
                .inNamespace("membersite")
                .withField("metadata.name", "ms-site")
                .list();

        for (GenericKubernetesResource resource : resourceList.getItems()) {
            resource.setKind("Deployment");
            logger.log(Level.INFO, "apiVersion: {0}, kind: {1}, name: {2}", new Object[] {
                resource.getApiVersion(),
                resource.getKind(),
                resource.getMetadata().getName()
            });
            resource.getMetadata().setManagedFields(null);
            resource.getMetadata().setFinalizers(null);
            resource.getMetadata().setCreationTimestamp(null);
            resource.getMetadata().setGeneration(null);
            resource.getMetadata().setResourceVersion(null);
            resource.getMetadata().setUid(null);
            resource.getMetadata().getAnnotations().remove("kubectl.kubernetes.io/last-applied-configuration");
            resource.getMetadata().getAnnotations().remove("deployment.kubernetes.io/revision");
            resource.getAdditionalProperties().remove("status");
            String yaml = YAML_MAPPER.writeValueAsString(resource);
            logger.log(Level.INFO, "Resource yaml: \n {0}", yaml);

            try {
                resource.getMetadata().setNamespace("yeadfaff");
                Object createOrResult = kubernetesClient
                        .resource(new ByteArrayInputStream(
                                YAML_MAPPER.writeValueAsString(resource).getBytes(StandardCharsets.UTF_8)))
                        .createOr(Updatable::update);
                logger.log(Level.INFO, "CreateOr result: {0}", createOrResult);
            } catch (KubernetesClientException e) {
                String status = e.getStatus().getStatus();
                String reason = e.getStatus().getReason();
                String message = e.getMessage();
                String errorMessage = String.format("Status=%s, Reason=%s. %s", status, reason, message);

                String statusYaml = YAML_MAPPER.writeValueAsString(e.getStatus());
                logger.log(
                        Level.INFO,
                        "KubernetesClientException. Overall: {0}, statusYaml: {1}, errorMessage: {2}",
                        new Object[] {e, statusYaml, errorMessage});
            }
        }
    }
}
