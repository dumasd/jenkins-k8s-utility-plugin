package io.jenkins.plugins.dumasd.k8s.config;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.dumasd.k8s.utils.Utils;
import java.io.Serializable;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author Bruce.Wu
 * @date 2024-11-14
 */
@Setter
@Getter
@ToString
public class K8sResourceNameConfig extends AbstractDescribableImpl<K8sResourceNameConfig> implements Serializable {

    private static final long serialVersionUID = 7964149685482666931L;
    /**
     * namespace
     */
    private String namespace;
    /**
     * 资源apiVersion networking.istio.io/v1beta1
     */
    private String apiVersion;
    /**
     * 资源kind VirtualService
     */
    private String kind;
    /**
     * 资源名称 account-destination-rule
     */
    private String name;

    @DataBoundConstructor
    public K8sResourceNameConfig(@NonNull String apiVersion, @NonNull String kind, @NonNull String name) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.name = name;
    }

    public String getResourceId() {
        String str = String.format("%s|%s|%s|%s", apiVersion, kind, name, Util.fixNull(namespace));
        return Utils.getSimpleUUID(str);
    }

    @DataBoundSetter
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<K8sResourceNameConfig> {

        public FormValidation doCheckResource(@QueryParameter("resource") String resource) {
            if (Utils.isNullOrEmpty(resource)) {
                return FormValidation.error("Resource required");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckName(@QueryParameter("name") String name) {
            if (Utils.isNullOrEmpty(name)) {
                return FormValidation.error("Name required");
            }
            return FormValidation.ok();
        }
    }
}
