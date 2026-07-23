package com.profilepilot.autofill;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProfileAccessibilityService extends AccessibilityService {
    private final List<Match> matches = new ArrayList<>();
    private LinearLayout overlay;
    private boolean processing;

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (processing || overlay != null) return;
        SecureProfileStore store = new SecureProfileStore(this);
        if (!store.isAccessibilityArmed()) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        processing = true;
        matches.clear();
        ProfileData profile = store.load();
        String packageName = event.getPackageName() == null ? "" : event.getPackageName().toString();
        String platform = FieldMatcher.detectPlatform(packageName, "", profile.selectedPlatform);
        collect(root, profile.valuesFor(platform));
        if (matches.isEmpty()) {
            processing = false;
            return;
        }
        store.setAccessibilityArmed(false);
        showReview(platform);
        processing = false;
    }

    @Override public void onInterrupt() { removeOverlay(); }
    @Override public void onDestroy() { removeOverlay(); super.onDestroy(); }

    private void collect(AccessibilityNodeInfo node, Map<FieldMatcher.FieldKey, String> values) {
        if (node == null) return;
        if (node.isEditable() && node.isEnabled()) {
            String descriptor = descriptor(node);
            FieldMatcher.FieldKey key = FieldMatcher.match(descriptor);
            CharSequence current = node.getText();
            String value = key == null ? null : values.get(key);
            if (key != null && !FieldMatcher.isSensitive(descriptor) && value != null && !value.trim().isEmpty()
                    && (current == null || current.toString().trim().isEmpty())) {
                matches.add(new Match(AccessibilityNodeInfo.obtain(node), key, current == null ? "" : current.toString(), value));
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), values);
    }

    private String descriptor(AccessibilityNodeInfo node) {
        StringBuilder out = new StringBuilder();
        append(out, node.getViewIdResourceName());
        append(out, node.getHintText());
        append(out, node.getText());
        append(out, node.getContentDescription());
        append(out, node.getClassName());
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            append(out, node.getPaneTitle());
            append(out, node.getTooltipText());
        }
        return out.toString();
    }

    private void showReview(String platform) {
        removeOverlayOnly();
        LinearLayout panel = panel();
        panel.addView(label("ProfilePilot • " + platform, 18, true, Color.rgb(6, 78, 59)));
        Set<String> names = new LinkedHashSet<>();
        for (Match match : matches) names.add(pretty(match.key));
        TextView summary = label("Recognised " + matches.size() + " blank field" + (matches.size() == 1 ? "" : "s") + ":\n" + join(names), 14, false, Color.DKGRAY);
        summary.setPadding(0, dp(7), 0, dp(7));
        panel.addView(summary);
        TextView warning = label("Review first. Fill does not save or submit the page and never enters passwords, payment details, identity documents or security codes.", 13, true, Color.rgb(153, 27, 27));
        warning.setPadding(0, 0, 0, dp(8));
        panel.addView(warning);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel = button("Cancel", Color.rgb(75, 85, 99));
        Button fill = button("Fill fields", Color.rgb(6, 78, 59));
        actions.addView(cancel, weighted()); actions.addView(fill, weighted());
        panel.addView(actions);
        cancel.setOnClickListener(v -> removeOverlay());
        fill.setOnClickListener(v -> fillFields());
        overlay = panel;
        ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(panel, overlayParams());
    }

    private void fillFields() {
        int filled = 0;
        for (Match match : matches) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, match.value);
            if (match.node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) filled++;
        }
        showUndo(filled);
    }

    private void showUndo(int filled) {
        removeOverlayOnly();
        LinearLayout panel = panel();
        panel.addView(label("ProfilePilot filled " + filled + " field" + (filled == 1 ? "" : "s") + ".", 17, true, Color.rgb(6, 78, 59)));
        TextView note = label("Review the page. No Save, Submit, Apply or Publish button was pressed.", 14, false, Color.DKGRAY);
        note.setPadding(0, dp(6), 0, dp(8));
        panel.addView(note);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button undo = button("Undo", Color.rgb(153, 27, 27));
        Button close = button("Close", Color.rgb(6, 78, 59));
        actions.addView(undo, weighted()); actions.addView(close, weighted());
        panel.addView(actions);
        undo.setOnClickListener(v -> undoFields());
        close.setOnClickListener(v -> removeOverlay());
        overlay = panel;
        ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(panel, overlayParams());
    }

    private void undoFields() {
        int restored = 0;
        for (Match match : matches) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, match.previous);
            if (match.node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) restored++;
        }
        Toast.makeText(this, "Restored " + restored + " fields.", Toast.LENGTH_LONG).show();
        removeOverlay();
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), Color.rgb(209, 213, 219));
        panel.setBackground(background);
        return panel;
    }

    private WindowManager.LayoutParams overlayParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM;
        params.y = dp(12);
        return params;
    }

    private void removeOverlayOnly() {
        if (overlay == null) return;
        try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(overlay); } catch (Exception ignored) {}
        overlay = null;
    }

    private void removeOverlay() {
        removeOverlayOnly();
        matches.clear();
    }

    private TextView label(String value, int size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private Button button(String value, int color) {
        Button button = new Button(this);
        button.setText(value); button.setAllCaps(false); button.setTextColor(Color.WHITE); button.setBackgroundColor(color);
        return button;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private String pretty(FieldMatcher.FieldKey key) {
        String value = key.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String join(Set<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) { if (out.length() > 0) out.append(", "); out.append(value); }
        return out.toString();
    }

    private void append(StringBuilder target, CharSequence value) { if (value != null) target.append(' ').append(value); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class Match {
        final AccessibilityNodeInfo node;
        final FieldMatcher.FieldKey key;
        final String previous;
        final String value;
        Match(AccessibilityNodeInfo node, FieldMatcher.FieldKey key, String previous, String value) {
            this.node = node; this.key = key; this.previous = previous; this.value = value;
        }
    }
}
