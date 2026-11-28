package com.projecthub.utils;

import com.projecthub.models.User;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MentionParser {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9._-]+)");

    private MentionParser() {}

    public static List<String> extractMentionTokens(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (token != null && !token.trim().isEmpty()) {
                tokens.add(token.trim().toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(tokens);
    }

    public static List<User> resolveMentionedUsers(String text, List<User> projectMembers) {
        List<User> matched = new ArrayList<>();
        if (text == null || text.isEmpty() || projectMembers == null || projectMembers.isEmpty()) {
            return matched;
        }
        List<String> tokens = extractMentionTokens(text);
        if (tokens.isEmpty()) {
            return matched;
        }

        Set<String> seenUserIds = new LinkedHashSet<>();
        for (User user : projectMembers) {
            if (user == null || user.getUserId() == null) continue;
            if (seenUserIds.contains(user.getUserId())) continue;

            if (matchesAnyToken(user, tokens)) {
                seenUserIds.add(user.getUserId());
                matched.add(user);
            }
        }
        return matched;
    }

    private static boolean matchesAnyToken(User user, List<String> tokens) {
        String name = user.getName() != null ? user.getName().trim().toLowerCase(Locale.ROOT) : "";
        String firstName = "";
        if (!name.isEmpty()) {
            String[] parts = name.split("\\s+");
            if (parts.length > 0) firstName = parts[0];
        }
        String compactName = name.replaceAll("\\s+", "");
        String underscoreName = name.replaceAll("\\s+", "_");
        String dotName = name.replaceAll("\\s+", ".");
        String emailPrefix = "";
        if (user.getEmail() != null && user.getEmail().contains("@")) {
            emailPrefix = user.getEmail().substring(0, user.getEmail().indexOf('@')).toLowerCase(Locale.ROOT);
        }

        for (String token : tokens) {
            if (token.equals(firstName) || token.equals(compactName)
                    || token.equals(underscoreName) || token.equals(dotName)
                    || (!emailPrefix.isEmpty() && token.equals(emailPrefix))) {
                return true;
            }
        }
        return false;
    }

    public static String formatMentionHandle(User user) {
        if (user == null) return "";
        if (user.getName() != null && !user.getName().trim().isEmpty()) {
            return "@" + user.getName().trim().replaceAll("\\s+", "");
        }
        if (user.getEmail() != null && user.getEmail().contains("@")) {
            return "@" + user.getEmail().substring(0, user.getEmail().indexOf('@'));
        }
        return "";
    }
}
