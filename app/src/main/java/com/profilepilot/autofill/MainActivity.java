package com.profilepilot.autofill;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import java.util.Map;

public final class MainActivity extends Activity {
    private static final String[] PLATFORMS = {"Freelancer", "Upwork", "Fiverr", "LinkedIn"};
    private final LinkedHashMap<String, EditText> fields = new LinkedHashMap<>();
    private SecureProfileStore store;
    private Spinner platform;
    private TextView status;
    private TextView connectionStatus;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new SecureProfileStore(this);
        setContentView(buildUi());
        loadIntoForm(store.load());
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
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(30));
        scroll.addView(root);

        TextView title = text("ProfilePilot Android Autofill", 25, true, Color.rgb(6, 78, 59));
        root.addView(title);
        TextView intro = text("Freelance Autopilot can now send a prepared job, proposal and bid directly into this app. ProfilePilot then opens the correct listing and fills recognised application fields.", 15, false, Color.DKGRAY);
        intro.setPadding(0, dp(8), 0, dp(12));
        root.addView(intro);

        status = text("", 14, true, Color.rgb(146, 64, 14));
        status.setPadding(dp(12), dp(10), dp(12), dp(10));
        status.setBackgroundColor(Color.rgb(255, 247, 237));
        root.addView(status, full());

        root.addView(section("Connected application"));
        connectionStatus = text("No job has been sent from Freelance Autopilot.", 14, true, Color.rgb(75, 85, 99));
        connectionStatus.setPadding(dp(12), dp(10), dp(12), dp(10));
        connectionStatus.setBackgroundColor(Color.rgb(248, 250, 252));
        root.addView(connectionStatus, full());
        root.addView(button("Open connected job and fill", Color.rgb(6, 78, 59), v -> openConnectedJob()));
        root.addView(button("Clear connected job", Color.rgb(100, 116, 139), v -> clearConnectedJob()));

        root.addView(section("1. Enable phone autofill"));
        root.addView(button("Open Android Autofill settings", Color.rgb(6, 78, 59), v -> openAutofillSettings()));
        root.addView(button("Open Accessibility fallback settings", Color.rgb(30, 64, 175), v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));

        root.addView(section("2. Choose the profile version"));
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

        root.addView(section("3. Save, review and fill"));
        root.addView(button("Save encrypted profile", Color.rgb(6, 78, 59), v -> saveForm()));
        root.addView(button("Review selected platform fields", Color.rgb(180, 83, 9), v -> review()));
        root.addView(button("Arm one-time browser fallback", Color.rgb(30, 64, 175), v -> armFallback()));
        root.addView(button("Open selected marketplace profile", Color.rgb(55, 65, 81), v -> openMarketplace()));

        root.addView(section("Backup"));
        root.addView(button("Copy profile JSON", Color.rgb(75, 85, 99), v -> exportJson()));
        root.addView(button("Import profile JSON from clipboard", Color.rgb(75, 85, 99), v -> importJson()));

        TextView guard = text("ProfilePilot never presses Save, Submit, Apply or Publish. Passwords, PINs, verification codes, payment details, bank details, identity documents and dates of birth are blocked.", 13, true, Color.rgb(153, 27, 27));
        guard.setPadding(0, dp(14), 0, 0);
        root.addView(guard);
        return scroll;
    }

    private void addField(LinearLayout root, String label, boolean multiline) {
        TextView name = text(label, 14, true, Color.DKGRAY);
        name.setPadding(0, dp(11), 0, dp(3));
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

    private void loadIntoForm(ProfileData p) {
        fields.get("Full name").setText(p.fullName);
        fields.get("Location").setText(p.location);
        fields.get("Primary service").setText(p.primaryService);
        fields.get("Experience").setText(p.experience);
        fields.get("Skills").setText(p.skills);
        fields.get("Tools").setText(p.tools);
        fields.get("Languages").setText(p.languages);
        fields.get("Achievements").setText(p.achievements);
        fields.get("Preferred clients").setText(p.preferredClients);
        for (int i = 0; i < PLATFORMS.length; i++) {
            if (PLATFORMS[i].equalsIgnoreCase(p.selectedPlatform)) platform.setSelection(i);
        }
        updateConnectionStatus(p);
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
            updateConnectionStatus(p);
            toast("Profile and connected application saved securely.");
        } catch (Exception e) {
            toast("Profile could not be encrypted and saved.");
        }
    }

    private void review() {
        ProfileData p = formData();
        Map<FieldMatcher.FieldKey, String> values = p.valuesFor(p.selectedPlatform);
        StringBuilder message = new StringBuilder("Platform: ").append(p.selectedPlatform).append("\n\n");
        if (p.hasConnectedJob()) {
            message.append("Connected job: ").append(p.currentJobTitle).append("\n")
                    .append("Bid: ").append(p.currentCurrency).append(' ').append(p.currentBidAmount).append("\n")
                    .append("Delivery: ").append(p.currentDeliveryDays).append(" days\n\n");
        }
        for (Map.Entry<FieldMatcher.FieldKey, String> item : values.entrySet()) {
            if (item.getValue() == null || item.getValue().trim().isEmpty()) continue;
            message.append(pretty(item.getKey())).append(":\n").append(item.getValue()).append("\n\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Review before filling")
                .setMessage(message.toString())
                .setPositiveButton("Save", (dialog, which) -> saveForm())
                .setNegativeButton("Close", null)
                .show();
    }

    private void handleIntent(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data == null || !"profilepilot".equalsIgnoreCase(data.getScheme()) || !"handoff".equalsIgnoreCase(data.getHost())) {
            updateConnectionStatus(store.load());
            return;
        }
        String payload = data.getQueryParameter("payload");
        if (payload == null || payload.trim().isEmpty()) {
            toast("The Freelance Autopilot handoff was empty.");
            return;
        }
        try {
            byte[] decoded = Base64.decode(payload, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            JSONObject job = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
            ProfileData p = store.load();
            p.selectedPlatform = supportedPlatform(job.optString("marketplace", p.selectedPlatform));
            p.currentJobTitle = job.optString("title", "Prepared freelance application");
            p.currentJobUrl = job.optString("sourceUrl", "");
            p.currentProposal = job.optString("proposal", "");
            p.currentBidAmount = valueFrom(job, "offeredPrice");
            p.currentCurrency = job.optString("currency", "");
            p.currentClientNeed = job.optString("clientNeed", job.optString("summary", ""));
            p.currentDeliveryDays = job.optString("deliveryDays", "");
            if (p.currentDeliveryDays.trim().isEmpty()) {
                int hours = Math.max(1, job.optInt("estimatedHours", 8));
                p.currentDeliveryDays = String.valueOf(Math.max(1, (int) Math.ceil(hours / 8.0)));
            }
            p.currentHandoffId = job.optString("id", String.valueOf(System.currentTimeMillis()));
            if (p.currentJobUrl.trim().isEmpty() || p.currentProposal.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing listing or proposal");
            }
            store.save(p);
            loadIntoForm(p);
            new AlertDialog.Builder(this)
                    .setTitle("Application received")
                    .setMessage(p.currentJobTitle + "\n\nThe proposal, bid and delivery time are now connected to ProfilePilot. Review them, then open the listing and fill the application.")
                    .setPositiveButton("Open job and fill", (dialog, which) -> openConnectedJob())
                    .setNegativeButton("Review first", null)
                    .show();
        } catch (Exception e) {
            toast("This Freelance Autopilot application package could not be opened.");
        }
    }

    private String valueFrom(JSONObject object, String key) {
        Object value = object.opt(key);
        if (value == null || value == JSONObject.NULL) return "";
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            if (number == Math.rint(number)) return String.valueOf((long) number);
        }
        return String.valueOf(value);
    }

    private String supportedPlatform(String value) {
        for (String supported : PLATFORMS) if (supported.equalsIgnoreCase(value)) return supported;
        return "Freelancer";
    }

    private void openConnectedJob() {
        ProfileData p = formData();
        if (!p.hasConnectedJob()) {
            toast("Send a prepared job from Freelance Autopilot first.");
            return;
        }
        try {
            store.save(p);
            store.setAccessibilityArmed(true);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(p.currentJobUrl)));
        } catch (Exception e) {
            toast("The connected job could not be opened.");
        }
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
            store.setAccessibilityArmed(false);
            store.save(p);
            updateConnectionStatus(p);
            toast("Connected application cleared.");
        } catch (Exception e) {
            toast("Connected application could not be cleared.");
        }
    }

    private void updateConnectionStatus(ProfileData p) {
        if (connectionStatus == null) return;
        if (p.hasConnectedJob()) {
            String bid = p.currentBidAmount.trim().isEmpty() ? "price not stated" : (p.currentCurrency + " " + p.currentBidAmount).trim();
            connectionStatus.setText("✓ Connected from Freelance Autopilot\n" + p.currentJobTitle + "\n" + p.selectedPlatform + " • " + bid + " • " + p.currentDeliveryDays + " day delivery");
            connectionStatus.setTextColor(Color.rgb(6, 78, 59));
            connectionStatus.setBackgroundColor(Color.rgb(236, 253, 245));
        } else {
            connectionStatus.setText("No job has been sent from Freelance Autopilot.");
            connectionStatus.setTextColor(Color.rgb(75, 85, 99));
            connectionStatus.setBackgroundColor(Color.rgb(248, 250, 252));
        }
    }

    private void armFallback() {
        saveForm();
        store.setAccessibilityArmed(true);
        new AlertDialog.Builder(this)
                .setTitle("One-time fallback armed")
                .setMessage("Open a marketplace form. ProfilePilot will scan the current screen once, show the recognised fields and wait for you to press Fill. It will not submit the form.")
                .setPositiveButton("Open marketplace", (d, w) -> openMarketplace())
                .setNegativeButton("Cancel", (d, w) -> store.setAccessibilityArmed(false))
                .show();
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

    private void openMarketplace() {
        String selected = String.valueOf(platform.getSelectedItem());
        String url = "https://www.freelancer.com/u/me";
        if ("Upwork".equals(selected)) url = "https://www.upwork.com/freelancers/settings/profile";
        if ("Fiverr".equals(selected)) url = "https://www.fiverr.com/users/me/edit";
        if ("LinkedIn".equals(selected)) url = "https://www.linkedin.com/in/me/edit/intro/";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void exportJson() {
        try {
            String json = formData().toJson().toString(2);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("ProfilePilot profile", json));
            toast("Profile JSON copied.");
        } catch (Exception e) {
            toast("Profile could not be exported.");
        }
    }

    private void importJson() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (!clipboard.hasPrimaryClip()) throw new IllegalArgumentException();
            CharSequence text = clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
            ProfileData imported = ProfileData.fromJson(text.toString());
            loadIntoForm(imported);
            store.save(imported);
            toast("Profile imported and encrypted.");
        } catch (Exception e) {
            toast("Clipboard does not contain valid ProfilePilot JSON.");
        }
    }

    private void updateStatus() {
        AutofillManager manager = getSystemService(AutofillManager.class);
        boolean autofill = manager != null && manager.hasEnabledAutofillServices();
        status.setText((autofill ? "✓ Android Autofill is enabled" : "Android Autofill is not enabled") +
                "\nAccessibility fallback: " + (store.isAccessibilityArmed() ? "armed for the next screen" : "not armed"));
        status.setTextColor(autofill ? Color.rgb(6, 78, 59) : Color.rgb(146, 64, 14));
        updateConnectionStatus(store.load());
    }

    private String value(String label) { return fields.get(label).getText().toString().trim(); }
    private String pretty(FieldMatcher.FieldKey key) {
        String s = key.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    private TextView section(String value) {
        TextView view = text(value, 18, true, Color.rgb(28, 25, 23));
        view.setPadding(0, dp(20), 0, dp(7));
        return view;
    }
    private TextView text(String value, int size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }
    private Button button(String value, int color, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(value); button.setAllCaps(false); button.setTextColor(Color.WHITE); button.setBackgroundColor(color);
        button.setOnClickListener(action);
        LinearLayout.LayoutParams params = full(); params.setMargins(0, dp(5), 0, dp(5)); button.setLayoutParams(params);
        return button;
    }
    private LinearLayout.LayoutParams full() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
}
