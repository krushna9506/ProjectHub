package com.projecthub.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class SafeUrlUtils {

    private SafeUrlUtils() {}

    /**
     * Validates that a string is a well-formed http/https URI with a non-empty host.
     * Blocks javascript:, file:, content:, intent:, data:, and malformed URIs.
     */
    public static boolean isSafeHttpUrl(String rawUrl) {
        if (rawUrl == null) return false;
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) return false;
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return scheme != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && host != null
                    && !host.trim().isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Normalizes user-entered web links to https:// when no scheme is supplied,
     * rejecting dangerous explicit schemes such as javascript:, file:, content:, or intent:.
     */
    public static String normalizeSafeHttpUrl(String rawInput) {
        if (rawInput == null) return "";
        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) return "";
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return isSafeHttpUrl(trimmed) ? trimmed : "";
        }
        if (trimmed.contains(":") || trimmed.startsWith("//")) {
            return "";
        }
        String candidate = "https://" + trimmed;
        return isSafeHttpUrl(candidate) ? candidate : "";
    }

    public static void openSafeHttpUrl(Context context, String rawUrl) {
        if (context == null) return;
        if (!isSafeHttpUrl(rawUrl)) {
            Toast.makeText(context, "Blocked unsafe or invalid link.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl.trim()));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Cannot open URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
