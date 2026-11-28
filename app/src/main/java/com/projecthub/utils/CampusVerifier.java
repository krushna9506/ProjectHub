package com.projecthub.utils;

import java.util.Locale;

public final class CampusVerifier {

    private CampusVerifier() {}

    public static String extractDomain(String email) {
        if (email == null) return "";
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        int atIdx = trimmed.lastIndexOf('@');
        if (atIdx < 0 || atIdx == trimmed.length() - 1) {
            return "";
        }
        return trimmed.substring(atIdx + 1);
    }

    public static boolean isAcademicDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        String d = domain.trim().toLowerCase(Locale.ROOT);
        return d.endsWith(".edu")
                || d.contains(".edu.")
                || d.contains(".ac.")
                || d.endsWith(".ernet.in")
                || d.endsWith(".res.in")
                || d.endsWith(".uni.edu")
                || d.endsWith(".college.edu");
    }

    public static boolean isCampusVerified(boolean emailVerified, String email) {
        if (!emailVerified) return false;
        return isAcademicDomain(extractDomain(email));
    }
}
