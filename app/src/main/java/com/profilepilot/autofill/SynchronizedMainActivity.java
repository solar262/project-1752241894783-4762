package com.profilepilot.autofill;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

public final class SynchronizedMainActivity extends Activity {
    private static final String[] PLATFORMS = {"Freelancer", "Upwork", "Fiverr", "LinkedIn"};
    private final LinkedHashMap<String, EditText> fields = new LinkedHashMap<>();
    private SecureProfileStore store;
    private Spinner platform;
    private TextView serviceStatus;
    private TextView syncStatus;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new SecureProfileStore(this);
        setContentView(buildUi());
        loadForm(store.load());
        handleIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
        SyncClient.schedule(this);
        SyncClient.refreshAsync(this, this::updateStatus);
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(32));
        scroll.addView(root);

        root.addView(text("ProfilePilot • Synchronized", 25, true, Color.rgb(6, 78, 59)));
        TextView intro = text("Freelance Autopilot and ProfilePilot share one application record. Received, opened, filled, submitted and failed updates return automatically. Offline updates wait safely and retry when internet returns.", 15, false, Color.DKGRAY);
        intro.setPadding(0, dp(8), 0, dp(12));
        root.addView(intro);

        serviceStatus = statusBox();
        root.addView(serviceStatus, full());
        syncStatus = statusBox();
        root.addView(syncStatus, full());

        root.addView(section("Connected application"));
        root.addView(button("Open connected job and fill", Color.rgb(6, 78, 59), v -> openConnectedJob()));
        root.addView(button("Retry synchronization now", Color.rgb(30, 64, 175), v -> {
            SyncClient.schedule(this);
            SyncClient.refreshAsync(this, () -> {
                updateStatus();
                toast("Synchronization retry started.");
            });
        }));
        root.addView(button("Report application problem", Color.rgb(153, 27, 27), v -> reportFailed()));
        root.addView(button("Clear connected job", Color.rgb(100, 116, 139), v -> clearConnectedJob()));

        root.addView(section("Phone services"));
        root.addView(button("Enable Android Autofill", Color.rgb(6, 78, 59), v -> openAutofillSettings()));
        root.addView(button("Enable application filling and submit detection", Color.rgb(30, 64, 175), v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));

        root.addView(section("Profile content"));
        platform = new Spinner(this);
        platform.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, PLATFORMS));
        root.addView(platform, full());
        addField(root, "Full name", false);
        addField(root, "Location", false);
        addField(root, "Primary service", false);
        addField(root, "Experience", true);
        addField(root, "Skills", true);
        addField(root, "Tools", true);
        addField(root, "Languages", false);
        addField(root, "Achievements", true);
        addField(root, "Preferred clients", true);
        root.addView(button("Save encrypted profile", Color.rgb(6, 78, 59), v -> saveForm()));

        TextView guard = text("ProfilePilot fills blank fields but never presses the final marketplace button. After you press Submit, Apply or Place Bid, the accessibility service detects that click and reports submitted to Freelance Autopilot.", 13, true, Color.rgb(153, 27, 27));
        guard.setPadding(0, dp(16), 0, 0);
        root.addView(guard);
        return scroll;
    }

    private TextView statusBox() {
        TextView view = text("", 14, true, Color.rgb(75, 85, 99));
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        view.setBackgroundColor(Color.rgb(248, 250, 252));
        LinearLayout.LayoutParams params = full();
        params.setMargins(0, dp(5), 0, dp(5));
        view.setLayoutParams(params);
        return view;
    }

    private void addField(LinearLayout root, String label, boolean multiline) {
        TextView name = text(label, 14, true, Color.DKGRAY);
        name.setPadding(0, dp(10), 0, dp(3));
        root.addView(name);
        EditText input = new EditText(this);
        input.setTextSize(15);
        input.setSingleLine(!multiline);
        input.setMinLines(multiline ? 3 : 1);
        input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        input.setBackgroundColor(Color.rgb(250, 250, 249));
        input.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(input, full());
        fields.put(label, input);
    }

    private void loadForm(ProfileData p) {
        fields.get("Full name").setText(p.fullName);
        fields.get("Location").setText(p.location);
        fields.get("Primary service").setText(p.primaryService);
        fields.get("Experience").setText(p.experience);
        fields.get("Skills").setText(p.skills);
        fields.get("Tools").setText(p.tools);
        fields.get("Languages").setText(p.languages);
        fields.get("Achievements").setText(p.achievements);
        fields.get("Preferred clients").setText(p.preferredClients);
        for (int i = 0; i < PLATFORMS.length; i++) if (PLATFORMS[i].equalsIgnoreCase(p.selectedPlatform)) platform.setSelection(i);
        updateStatus();
    }

    private ProfileData formData() {
        ProfileData p = store.load();
        p.fullName = value("Full name");
        p.location = value("Location");
        p.primaryService = value("Primary service");
        p.experience = value("Experience");
        p.skills = value("Skills");
        p.tools = value("Tools");
        p.languages = value("Languages");
        p.achievements = value("Achievements");
        p.preferredClients = value("Preferred clients");
        p.selectedPlatform = String.valueOf(platform.getSelectedItem());
        return p;
    }

    private void saveForm() {
        ProfileData p = formData();
        if (p.fullName.trim().isEmpty() || p.skills.trim().isEmpty()) {
            toast("Full name and skills are required.");
            return;
        }
        try {
            store.save(p);
            updateStatus();
            toast("Encrypted profile saved.");
        } catch (Exception e) {
            toast("Profile could not be encrypted and saved.");
        }
    }

    private void handleIntent(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data == null || !"profilepilot".equalsIgnoreCase(data.getScheme()) || !"handoff".equalsIgnoreCase(data.getHost())) {
            updateStatus();
            return;
        }
        try {
            String payload = data.getQueryParameter("payload");
            if (payload == null || payload.trim().isEmpty()) throw new IllegalArgumentException("Empty handoff");
            byte[] decoded = Base64.decode(payload, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            JSONObject job = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
            ProfileData p = store.load();
            p.selectedPlatform = supportedPlatform(job.optString("marketplace", p.selectedPlatform));
            p.currentJobTitle = job.optString("title", "Prepared freelance application");
            p.currentJobUrl = job.optString("sourceUrl", "");
            p.currentProposal = job.optString("proposal", "");
            p.currentBidAmount = jsonValue(job.opt("offeredPrice"));
            p.currentCurrency = job.optString("currency", "");
            p.currentClientNeed = job.optString("clientNeed", job.optString("summary", ""));
            p.currentDeliveryDays = jsonValue(job.opt("deliveryDays"));
            if (p.currentDeliveryDays.isEmpty()) p.currentDeliveryDays = String.valueOf(Math.max(1, (int) Math.ceil(Math.max(1, job.optInt("estimatedHours", 8)) / 8.0)));
            p.currentHandoffId = job.optString("id", String.valueOf(System.currentTimeMillis()));
            p.currentSyncId = job.optString("syncId", "");
            p.currentSyncToken = job.optString("syncToken", "");
            p.currentSyncBaseUrl = job.optString("syncBaseUrl", "");
            p.currentSyncStatus = job.optString("syncStatus", "prepared");
            p.currentSyncDetail = "Application package prepared by Freelance Autopilot.";
            p.currentSyncUpdatedAt = System.currentTimeMillis();
            if (!p.hasConnectedJob() || !p.hasSync()) throw new IllegalArgumentException("Incomplete secure handoff");
            store.save(p);
            SyncClient.report(this, "received", "ProfilePilot received and securely stored the application package.");
            loadForm(store.load());
            new AlertDialog.Builder(this)
                    .setTitle("Application received")
                    .setMessage(p.currentJobTitle + "\n\nThe shared record is connected. Open the listing, fill the application and submit it when ready.")
                    .setPositiveButton("Open job and fill", (dialog, which) -> openConnectedJob())
                    .setNegativeButton("Review first", null)
                    .show();
        } catch (Exception e) {
            ProfileData p = store.load();
            if (p.hasSync()) SyncClient.report(this, "failed", "ProfilePilot could not open the received application package.");
            toast("The synchronized application package could not be opened.");
        }
    }

    private void openConnectedJob() {
        ProfileData p = formData();
        if (!p.hasConnectedJob() || !p.hasSync()) {
            toast("Send a prepared job from Freelance Autopilot first.");
            return;
        }
        try {
            store.save(p);
            store.setAccessibilityArmed(true);
            SyncClient.report(this, "opened", "The connected marketplace listing was opened for review and filling.");
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(p.currentJobUrl)));
        } catch (Exception e) {
            SyncClient.report(this, "failed", "Android could not open the connected marketplace listing.");
            toast("The connected job could not be opened.");
        }
    }

    private void reportFailed() {
        ProfileData p = formData();
        if (!p.hasSync()) {
            toast("There is no synchronized application to report.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Report application problem?")
                .setMessage("Freelance Autopilot will mark this phone application as failed so it can be retried.")
                .setPositiveButton("Report failed", (dialog, which) -> {
                    SyncClient.report(this, "failed", "User reported that the marketplace application could not be completed.");
                    updateStatus();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearConnectedJob() {
        try {
            ProfileData p = formData();
            p.currentJobTitle = "";
            p.currentJobUrl = "";
            p.currentProposal = "";
            p.currentBidAmount = "";
            p.currentCurrency = "";
            p.currentDeliveryDays = "";
            p.currentClientNeed = "";
            p.currentHandoffId = "";
            p.currentSyncId = "";
            p.currentSyncToken = "";
            p.currentSyncBaseUrl = "";
            p.currentSyncStatus = "";
            p.currentSyncDetail = "";
            p.currentSyncUpdatedAt = 0L;
            store.setAccessibilityArmed(false);
            store.save(p);
            updateStatus();
            toast("Connected application cleared.");
        } catch (Exception e) {
            toast("Connected application could not be cleared.");
        }
    }

    private void updateStatus() {
        if (serviceStatus == null || syncStatus == null) return;
        AutofillManager manager = getSystemService(AutofillManager.class);
        boolean autofill = manager != null && manager.hasEnabledAutofillServices();
        int pending = SyncClient.pendingCount(this);
        serviceStatus.setText((autofill ? "✓ Android Autofill is enabled" : "Android Autofill is not enabled")
                + "\nApplication filling: " + (store.isAccessibilityArmed() ? "armed for the next marketplace screen" : "not armed")
                + "\nOffline queue: " + (pending == 0 ? "clear" : pending + " update(s) waiting for internet"));
        serviceStatus.setTextColor(autofill ? Color.rgb(6, 78, 59) : Color.rgb(146, 64, 14));
        ProfileData p = store.load();
        if (!p.hasConnectedJob()) {
            syncStatus.setText("No synchronized application is currently connected.");
            syncStatus.setTextColor(Color.rgb(75, 85, 99));
            syncStatus.setBackgroundColor(Color.rgb(248, 250, 252));
            return;
        }
        String bid = p.currentBidAmount.isEmpty() ? "price not stated" : (p.currentCurrency + " " + p.currentBidAmount).trim();
        String state = p.currentSyncStatus == null || p.currentSyncStatus.isEmpty() ? "prepared" : p.currentSyncStatus;
        String detail = p.currentSyncDetail == null || p.currentSyncDetail.isEmpty() ? "" : "\n" + p.currentSyncDetail;
        syncStatus.setText("✓ Synchronized with Freelance Autopilot\n" + p.currentJobTitle + "\n" + p.selectedPlatform + " • " + bid + " • " + p.currentDeliveryDays + " day delivery\nPhone status: " + state + detail);
        syncStatus.setTextColor("failed".equals(state) ? Color.rgb(153, 27, 27) : Color.rgb(6, 78, 59));
        syncStatus.setBackgroundColor("failed".equals(state) ? Color.rgb(254, 242, 242) : Color.rgb(236, 253, 245));
    }

    private void openAutofillSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private String supportedPlatform(String value) {
        for (String supported : PLATFORMS) if (supported.equalsIgnoreCase(value)) return supported;
        return "Freelancer";
    }

    private String jsonValue(Object value) {
        if (value == null || value == JSONObject.NULL) return "";
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            if (number == Math.rint(number)) return String.valueOf((long) number);
        }
        return String.valueOf(value);
    }

    private String value(String label) { return fields.get(label).getText().toString().trim(); }
    private TextView section(String value) { TextView view = text(value, 18, true, Color.rgb(28, 25, 23)); view.setPadding(0, dp(20), 0, dp(7)); return view; }
    private TextView text(String value, int size, boolean bold, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD); return view; }
    private Button button(String value, int color, View.OnClickListener action) { Button button = new Button(this); button.setText(value); button.setAllCaps(false); button.setTextColor(Color.WHITE); button.setBackgroundColor(color); button.setOnClickListener(action); LinearLayout.LayoutParams params = full(); params.setMargins(0, dp(5), 0, dp(5)); button.setLayoutParams(params); return button; }
    private LinearLayout.LayoutParams full() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
}
