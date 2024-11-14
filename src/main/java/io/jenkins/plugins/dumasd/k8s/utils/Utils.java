package io.jenkins.plugins.dumasd.k8s.utils;

import java.io.File;

/**
 * @author Bruce.Wu
 * @date 2024-11-14
 */
public final class Utils {

    private Utils() {
    }

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


    public static boolean isFile(String pathString) {
        File file = new File(pathString);
        // 检查路径是否存在
        if (file.exists()) {
            return file.isFile();
        } else {
            return !(pathString.endsWith("/") || pathString.endsWith("\\\\"));
        }
    }

}
