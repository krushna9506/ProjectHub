package com.projecthub.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.projecthub.models.GitHubItem;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class GitHubRepository {

    private static final String PREFS_NAME = "projecthub_github";
    private static final String KEY_TOKEN = "github_pat";
    private static final Pattern SAFE_SEGMENT_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static class RepoOverview {
        public final String fullName;
        public final String description;
        public final int stars;
        public final int openIssuesCount;
        public final List<GitHubItem> items;

        public RepoOverview(String fullName, String description, int stars, int openIssuesCount, List<GitHubItem> items) {
            this.fullName = fullName;
            this.description = description;
            this.stars = stars;
            this.openIssuesCount = openIssuesCount;
            this.items = items;
        }
    }

    public interface GitHubCallback {
        void onSuccess(RepoOverview overview);
        void onError(String message);
    }

    public static void saveToken(Context context, String token) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token != null ? token.trim() : "").apply();
    }

    public static String getToken(Context context) {
        if (context == null) return "";
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, "");
    }

    public static void clearToken(Context context) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_TOKEN).apply();
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public static String parseOwnerRepo(String input) {
        if (input == null) return "";
        String cleaned = input.trim();
        if (cleaned.isEmpty()) return "";

        int qIdx = cleaned.indexOf('?');
        if (qIdx >= 0) cleaned = cleaned.substring(0, qIdx);
        int hIdx = cleaned.indexOf('#');
        if (hIdx >= 0) cleaned = cleaned.substring(0, hIdx);

        cleaned = cleaned.replaceFirst("(?i)^https?://(www\\.)?github\\.com/", "");
        cleaned = cleaned.replaceFirst("(?i)^github\\.com/", "");
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.endsWith(".git")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        String[] parts = cleaned.split("/");
        if (parts.length < 2) return "";
        String owner = parts[0].trim();
        String repo = parts[1].trim();
        if (owner.isEmpty() || repo.isEmpty() || ".".equals(owner) || "..".equals(owner)
                || ".".equals(repo) || "..".equals(repo)) {
            return "";
        }
        if (!SAFE_SEGMENT_PATTERN.matcher(owner).matches() || !SAFE_SEGMENT_PATTERN.matcher(repo).matches()) {
            return "";
        }
        return owner + "/" + repo;
    }

    private static String optString(JsonObject obj, String key, String fallback) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsString();
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    private static int optInt(JsonObject obj, String key, int fallback) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsInt();
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    public static List<GitHubItem> parseIssuesJson(String json) throws Exception {
        List<GitHubItem> result = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray()) {
            return result;
        }
        JsonArray array = root.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement elem = array.get(i);
            if (!elem.isJsonObject()) continue;
            JsonObject obj = elem.getAsJsonObject();

            int number = optInt(obj, "number", 0);
            String title = optString(obj, "title", "Untitled");
            String state = optString(obj, "state", "open");
            String htmlUrl = optString(obj, "html_url", "");
            String updatedAt = optString(obj, "updated_at", "");
            boolean isPr = obj.has("pull_request") && !obj.get("pull_request").isJsonNull();

            String author = "";
            if (obj.has("user") && obj.get("user").isJsonObject()) {
                author = optString(obj.getAsJsonObject("user"), "login", "");
            }

            List<String> labels = new ArrayList<>();
            if (obj.has("labels") && obj.get("labels").isJsonArray()) {
                JsonArray labelsArray = obj.getAsJsonArray("labels");
                for (int j = 0; j < labelsArray.size(); j++) {
                    if (labelsArray.get(j).isJsonObject()) {
                        String labelName = optString(labelsArray.get(j).getAsJsonObject(), "name", "");
                        if (!labelName.isEmpty()) labels.add(labelName);
                    }
                }
            }

            result.add(new GitHubItem(number, title, state, author, htmlUrl, isPr, updatedAt, labels));
        }
        return result;
    }

    public void fetchRepositoryDetails(String repoUrlOrSlug, String token, GitHubCallback callback) {
        String ownerRepo = parseOwnerRepo(repoUrlOrSlug);
        if (ownerRepo.isEmpty()) {
            mainHandler.post(() -> callback.onError("Invalid GitHub repo URL. Use https://github.com/owner/repo or owner/repo."));
            return;
        }

        executor.execute(() -> {
            try {
                String repoJsonStr = httpGet("https://api.github.com/repos/" + ownerRepo, token);
                JsonObject repoJson = JsonParser.parseString(repoJsonStr).getAsJsonObject();
                String fullName = optString(repoJson, "full_name", ownerRepo);
                String description = optString(repoJson, "description", "");
                int stars = optInt(repoJson, "stargazers_count", 0);
                int openIssues = optInt(repoJson, "open_issues_count", 0);

                String issuesJsonStr = httpGet("https://api.github.com/repos/" + ownerRepo + "/issues?state=all&per_page=30", token);
                List<GitHubItem> items = parseIssuesJson(issuesJsonStr);

                RepoOverview overview = new RepoOverview(fullName, description, stars, openIssues, items);
                mainHandler.post(() -> callback.onSuccess(overview));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "GitHub request failed";
                mainHandler.post(() -> callback.onError(msg));
            }
        });
    }

    private String httpGet(String urlStr, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "ProjectHub-Android");
        if (token != null && !token.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token.trim());
        }

        int status = conn.getResponseCode();
        InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
        }
        conn.disconnect();

        if (status < 200 || status >= 300) {
            String apiMsg = "GitHub HTTP " + status;
            try {
                JsonObject errObj = JsonParser.parseString(sb.toString()).getAsJsonObject();
                String detail = optString(errObj, "message", "");
                if (!detail.isEmpty()) {
                    apiMsg += ": " + detail;
                }
            } catch (Exception ignored) {}
            throw new Exception(apiMsg);
        }
        return sb.toString();
    }
}
