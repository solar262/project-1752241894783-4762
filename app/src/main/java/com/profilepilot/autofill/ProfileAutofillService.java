package com.profilepilot.autofill;

import android.app.assist.AssistStructure;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProfileAutofillService extends AutofillService {
    @Override
    public void onFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback) {
        try {
            List<FillContext> contexts = request.getFillContexts();
            if (contexts.isEmpty()) {
                callback.onSuccess(null);
                return;
            }
            AssistStructure structure = contexts.get(contexts.size() - 1).getStructure();
            ProfileData profile = new SecureProfileStore(this).load();
            String packageName = structure.getActivityComponent() == null ? "" : structure.getActivityComponent().getPackageName();
            String domain = findWebDomain(structure);
            String platform = FieldMatcher.detectPlatform(packageName, domain, profile.selectedPlatform);
            Map<FieldMatcher.FieldKey, String> values = profile.valuesFor(platform);
            LinkedHashMap<AutofillId, FieldMatcher.FieldKey> matches = new LinkedHashMap<>();
            for (int i = 0; i < structure.getWindowNodeCount(); i++) {
                collect(structure.getWindowNodeAt(i).getRootViewNode(), matches);
            }
            if (matches.isEmpty()) {
                callback.onSuccess(null);
                return;
            }
            RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
            presentation.setTextViewText(android.R.id.text1, "ProfilePilot • Fill " + platform + " profile");
            Dataset.Builder dataset = new Dataset.Builder(presentation);
            int count = 0;
            for (Map.Entry<AutofillId, FieldMatcher.FieldKey> match : matches.entrySet()) {
                String value = values.get(match.getValue());
                if (value != null && !value.trim().isEmpty()) {
                    dataset.setValue(match.getKey(), AutofillValue.forText(value), presentation);
                    count++;
                }
            }
            if (count == 0) {
                callback.onSuccess(null);
                return;
            }
            callback.onSuccess(new FillResponse.Builder().addDataset(dataset.build()).build());
        } catch (Exception e) {
            callback.onFailure("ProfilePilot could not prepare this form.");
        }
    }

    @Override
    public void onSaveRequest(SaveRequest request, SaveCallback callback) {
        callback.onSuccess();
    }

    private void collect(AssistStructure.ViewNode node, Map<AutofillId, FieldMatcher.FieldKey> out) {
        if (node == null) return;
        AutofillId id = node.getAutofillId();
        if (id != null && node.getAutofillType() == View.AUTOFILL_TYPE_TEXT) {
            String descriptor = descriptor(node);
            FieldMatcher.FieldKey key = FieldMatcher.match(descriptor);
            if (key != null && !FieldMatcher.isSensitive(descriptor)) out.put(id, key);
        }
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChildAt(i), out);
    }

    private String descriptor(AssistStructure.ViewNode node) {
        StringBuilder text = new StringBuilder();
        append(text, node.getIdEntry()); append(text, node.getHint()); append(text, node.getText());
        append(text, node.getContentDescription()); append(text, node.getClassName());
        String[] hints = node.getAutofillHints();
        if (hints != null) for (String hint : hints) append(text, hint);
        if (node.getHtmlInfo() != null && node.getHtmlInfo().getAttributes() != null) {
            for (android.util.Pair<String, String> attribute : node.getHtmlInfo().getAttributes()) {
                append(text, attribute.first); append(text, attribute.second);
            }
        }
        return text.toString();
    }

    private String findWebDomain(AssistStructure structure) {
        List<String> domains = new ArrayList<>();
        for (int i = 0; i < structure.getWindowNodeCount(); i++) collectDomains(structure.getWindowNodeAt(i).getRootViewNode(), domains);
        return domains.isEmpty() ? "" : domains.get(0);
    }

    private void collectDomains(AssistStructure.ViewNode node, List<String> domains) {
        if (node == null) return;
        if (node.getWebDomain() != null) domains.add(node.getWebDomain());
        for (int i = 0; i < node.getChildCount(); i++) collectDomains(node.getChildAt(i), domains);
    }

    private void append(StringBuilder target, CharSequence value) {
        if (value != null) target.append(' ').append(value);
    }
}
