package io.jenkins.plugins.dumasd.k8s.utils;

public class KubernetesUtilityPluginException extends RuntimeException {

    private static final long serialVersionUID = -2259585652406508160L;

    public KubernetesUtilityPluginException(String message) {
        super(message);
    }

    public KubernetesUtilityPluginException(Throwable cause) {
        super(cause);
    }
}
