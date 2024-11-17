package io.jenkins.plugins.dumasd.k8s.model;

import io.jenkins.plugins.dumasd.k8s.utils.Utils;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class K8sResourceType implements Serializable {

    private static final long serialVersionUID = 5853571360809981510L;

    private String apiVersion;

    private String kind;

    private String id;

    public K8sResourceType(String apiVersion, String kind) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.id = Utils.getSimpleUUID(apiVersion + "|" + kind);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        K8sResourceType that = (K8sResourceType) o;
        return Objects.equals(apiVersion, that.apiVersion) && Objects.equals(kind, that.kind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiVersion, kind);
    }
}
