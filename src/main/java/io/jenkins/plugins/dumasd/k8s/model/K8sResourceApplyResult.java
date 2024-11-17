package io.jenkins.plugins.dumasd.k8s.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class K8sResourceApplyResult implements Serializable {
    private static final long serialVersionUID = -9036242295692283217L;

    private boolean suc;

    private String message = "OK";

    public void fail(String message) {
        this.suc = false;
        this.message = message;
    }

    public void ok() {
        this.suc = true;
        this.message = "OK";
    }

    public void ok(String message) {
        this.suc = true;
        this.message = message;
    }
}
