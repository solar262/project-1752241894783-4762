package com.profilepilot.autofill;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SyncClient {
    private static final String PREFS = "profilepilot_sync_queue";
    private static final String QUEUE = "pending_events";
    private static final String UNIQUE_WORK = "profilepilot-two-way-sync";
    private static final Object LOCK = new Object();

    private SyncClient() {}

    public static void report(Context context, String status, String detail) {
        Context app = context.getApplicationContext();
        SecureProfileStore store = new SecureProfileStore(app);
        ProfileData profile = store.load();
        if (!profile.hasSync()) return;
        try {
            JSONObject event = new JSONObject();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("syncId", profile.currentSyncId);
            event.put("token", profile.currentSyncToken);
            event.put("baseUrl", profile.currentSyncBaseUrl);
            event.put("status", status);
            event.put("detail", detail == null ? "" : detail);
            event.put("occurredAt", System.currentTimeMillis());
            event.put("deviceId", Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID));
            enqueueEvent(app, event);
            profile.currentSyncStatus = status;
            profile.currentSyncDetail = detail == null ? "" : detail;
            profile.currentSyncUpdatedAt = System.currentTimeMillis();
            store.save(profile);
        } catch (Exception ignored) {}
        schedule(app);
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, request);
    }

    public static void refreshAsync(Context context, Runnable complete) {
        Context app = context.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            refreshBlocking(app);
            if (complete != null) new Handler(Looper.getMainLooper()).post(complete);
        });
    }

    public static boolean flushBlocking(Context context) {
        while (true) {
            JSONObject event = firstEvent(context);
            if (event == null) break;
            try {
                int code = postEvent(context, event);
                if (code >= 200 && code < 300) removeEvent(context, event.optString("eventId"));
                else return false;
            } catch (Exception e) {
                return false;
            }
        }
        refreshBlocking(context);
        return true;
    }

    public static int pendingCount(Context context) {
        synchronized (LOCK) {
            return readQueue(context).length();
        }
    }

    static JSONObject createEventForTest(String syncId, String token, String baseUrl, String status, String eventId) throws Exception {
        JSONObject event = new JSONObject();
        event.put("eventId", eventId);
        event.put("syncId", syncId);
        event.put("token", token);
        event.put("baseUrl", baseUrl);
        event.put("status", status);
        event.put("detail", "test");
        event.put("occurredAt", 1L);
        event.put("deviceId", "test-device");
        return event;
    }

    private static void enqueueEvent(Context context, JSONObject event) {
        synchronized (LOCK) {
            JSONArray queue = readQueue(context);
            queue.put(event);
            writeQueue(context, queue);
        }
    }

    private static JSONObject firstEvent(Context context) {
        synchronized (LOCK) {
            JSONArray queue = readQueue(context);
            return queue.length() == 0 ? null : queue.optJSONObject(0);
        }
    }

    private static void removeEvent(Context context, String eventId) {
        synchronized (LOCK) {
            JSONArray queue = readQueue(context);
            JSONArray next = new JSONArray();
            for (int i = 0; i < queue.length(); i++) {
                JSONObject item = queue.optJSONObject(i);
                if (item != null && !eventId.equals(item.optString("eventId"))) next.put(item);
            }
            writeQueue(context, next);
        }
    }

    private static JSONArray readQueue(Context context) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(QUEUE, "[]");
        try { return new JSONArray(raw); } catch (Exception ignored) { return new JSONArray(); }
    }

    private static void writeQueue(Context context, JSONArray queue) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(QUEUE, queue.toString()).apply();
    }

    private static int postEvent(Context context, JSONObject event) throws Exception {
        String base = cleanBase(event.optString("baseUrl"));
        String syncId = event.optString("syncId");
        String token = event.optString("token");
        if (!base.startsWith("https://") || syncId.isEmpty() || token.isEmpty()) return 400;
        URL url = new URL(base + "/api/profilepilot/sync/" + syncId + "/events");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        JSONObject body = new JSONObject();
        body.put("token", token);
        body.put("eventId", event.optString("eventId"));
        body.put("status", event.optString("status"));
        body.put("detail", event.optString("detail"));
        body.put("occurredAt", event.optLong("occurredAt"));
        body.put("deviceId", event.optString("deviceId"));
        try (OutputStream out = connection.getOutputStream()) {
            out.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = connection.getResponseCode();
        String response = readBody(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
        if (code >= 200 && code < 300 && !response.isEmpty()) applyRemote(context, new JSONObject(response));
        connection.disconnect();
        return code;
    }

    private static boolean refreshBlocking(Context context) {
        ProfileData profile = new SecureProfileStore(context).load();
        if (!profile.hasSync()) return true;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(cleanBase(profile.currentSyncBaseUrl) + "/api/profilepilot/sync/" + profile.currentSyncId);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + profile.currentSyncToken);
            int code = connection.getResponseCode();
            if (code >= 200 && code < 300) {
                applyRemote(context, new JSONObject(readBody(connection.getInputStream())));
                return true;
            }
            return false;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void applyRemote(Context context, JSONObject response) {
        try {
            JSONObject sync = response.optJSONObject("sync");
            JSONObject job = response.optJSONObject("job");
            SecureProfileStore store = new SecureProfileStore(context);
            ProfileData profile = store.load();
            if (sync != null) {
                profile.currentSyncStatus = sync.optString("status", profile.currentSyncStatus);
                profile.currentSyncDetail = sync.optString("detail", profile.currentSyncDetail);
                profile.currentSyncUpdatedAt = sync.optLong("updatedAt", profile.currentSyncUpdatedAt);
            }
            if (job != null) {
                profile.currentJobTitle = job.optString("title", profile.currentJobTitle);
                profile.currentJobUrl = job.optString("sourceUrl", profile.currentJobUrl);
                profile.currentProposal = job.optString("proposal", profile.currentProposal);
                profile.currentBidAmount = valueFrom(job.opt("offeredPrice"), profile.currentBidAmount);
                profile.currentCurrency = job.optString("currency", profile.currentCurrency);
                profile.currentClientNeed = job.optString("clientNeed", profile.currentClientNeed);
            }
            store.save(profile);
        } catch (Exception ignored) {}
    }

    private static String valueFrom(Object value, String fallback) {
        if (value == null || value == JSONObject.NULL) return fallback;
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            if (number == Math.rint(number)) return String.valueOf((long) number);
        }
        return String.valueOf(value);
    }

    private static String readBody(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line);
        }
        return text.toString();
    }

    private static String cleanBase(String value) {
        String base = value == null ? "" : value.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }
}
