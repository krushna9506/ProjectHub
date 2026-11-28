package com.projecthub.utils;

import java.util.Locale;
import java.util.regex.Pattern;

public final class CampusVerifier {

    private static final Pattern ACADEMIC_DOMAIN_PATTERN = Pattern.compile(
        "^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*\\.(edu|edu\\.[a-z]{2}|ac\\.[a-z]{2}|ernet\\.in|res\\.in)$"
    );

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
        return ACADEMIC_DOMAIN_PATTERN.matcher(d).matches();
    }

    public static boolean isCampusVerified(boolean emailVerified, String email) {
        if (!emailVerified) return false;
        return isAcademicDomain(extractDomain(email));
    }
}
