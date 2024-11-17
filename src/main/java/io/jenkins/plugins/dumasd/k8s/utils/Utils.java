package io.jenkins.plugins.dumasd.k8s.utils;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;

/**
 * @author Bruce.Wu
 * @date 2024-11-14
 */
public final class Utils {

    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private Utils() {}

    public static boolean isNullOrEmpty(final String name) {
        return name == null || name.matches("\\s*");
    }

    public static boolean isNotEmpty(final String name) {
        return !isNullOrEmpty(name);
    }

    public static String getFileExt(File file) {
        String name = file.getName();
        int idx = name.indexOf('.');
        if (idx > 0) {
            return name.substring(idx + 1);
        } else {
            return null;
        }
    }

    public static String getSimpleUUID(String from) {
        String uuid =
                UUID.nameUUIDFromBytes(from.getBytes(StandardCharsets.UTF_8)).toString();
        return uuid.replace("-", "");
    }

    public static boolean isFile(String pathString) {
        File file = new File(pathString);
        // 检查路径是否存在
        if (file.exists()) {
            return file.isFile();
        } else {
            return !(pathString.endsWith("/") || pathString.endsWith("\\\\"));
        }
    }

    @CheckForNull
    public static StandardCredentials resolveCredentials(@CheckForNull String credentialsId, @NonNull ItemGroup owner) {
        if (credentialsId == null) {
            return null;
        }
        StandardCredentials c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItemGroup(
                        StandardCredentials.class, owner, ACL.SYSTEM2, Collections.emptyList()),
                CredentialsMatchers.allOf(
                        AuthenticationTokens.matcher(KubernetesAuth.class), CredentialsMatchers.withId(credentialsId)));
        if (c == null) {
            throw new KubernetesUtilityPluginException("No credentials found with id " + credentialsId);
        }
        return c;
    }
}
