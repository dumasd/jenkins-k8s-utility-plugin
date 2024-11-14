package io.jenkins.plugins.dumasd.k8s.config;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.dumasd.k8s.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.Serializable;


/**
 * @author Bruce.Wu
 * @date 2024-11-14
 */
@Setter
@Getter
@ToString
public class K8sResourceNameContentConfig extends AbstractDescribableImpl<K8sResourceNameContentConfig> implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * namespace
     */
    private String namespace;
    /**
     * 资源类型 destinationrules.networking.istio.io
     */
    private String resource;
    /**
     * 资源名称 account-destination-rule
     */
    private String name;
    /**
     * 资源YAML内容
     */
    private String content;

    @DataBoundConstructor
    public K8sResourceNameContentConfig(String resource, String name) {
        this.resource = resource;
        this.name = name;
    }

    @DataBoundSetter
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @DataBoundSetter
    public void setContent(String content) {
        this.content = content;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<K8sResourceNameContentConfig> {

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
