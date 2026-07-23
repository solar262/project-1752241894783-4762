package com.profilepilot.autofill;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class FieldMatcher {
    public enum FieldKey {
        FULL_NAME, LOCATION, HEADLINE, SUMMARY, SKILLS, TOOLS, LANGUAGES,
        EXPERIENCE, ACHIEVEMENTS, PREFERRED_CLIENTS, SERVICE_TITLE,
        SERVICE_DESCRIPTION, PORTFOLIO_TITLE, PORTFOLIO_DESCRIPTION,
        PROPOSAL, BID_AMOUNT, DELIVERY_DAYS
    }

    private static final String[] SENSITIVE = {
        "password", "passcode", "pin", "one time", "otp", "verification code",
        "security code", "cvv", "cvc", "credit card", "debit card", "card number",
        "bank account", "iban", "routing number", "social security", "ssn",
        "tax id", "passport", "identity document", "national id", "driver licence",
        "driver license", "date of birth", "birth date"
    };

    private static final LinkedHashMap<FieldKey, String[]> RULES = new LinkedHashMap<>();
    static {
        RULES.put(FieldKey.PROPOSAL, new String[]{"cover letter", "your proposal", "proposal details", "proposal description", "application message", "message to client", "describe your proposal", "why are you the best", "why should we hire you", "bid description"});
        RULES.put(FieldKey.BID_AMOUNT, new String[]{"bid amount", "your bid", "proposed price", "fixed price", "project amount", "hourly rate", "your rate", "offer amount"});
        RULES.put(FieldKey.DELIVERY_DAYS, new String[]{"delivery days", "days to deliver", "delivery time", "project duration", "duration in days", "complete in", "estimated duration"});
        RULES.put(FieldKey.PORTFOLIO_DESCRIPTION, new String[]{"portfolio description", "project description", "case study description"});
        RULES.put(FieldKey.PORTFOLIO_TITLE, new String[]{"portfolio title", "project title", "case study title"});
        RULES.put(FieldKey.SERVICE_DESCRIPTION, new String[]{"service description", "gig description", "project catalog description", "what you offer"});
        RULES.put(FieldKey.SERVICE_TITLE, new String[]{"service title", "gig title", "project catalog title", "service name"});
        RULES.put(FieldKey.PREFERRED_CLIENTS, new String[]{"preferred clients", "ideal clients", "target clients", "who i work with"});
        RULES.put(FieldKey.ACHIEVEMENTS, new String[]{"achievements", "accomplishments", "awards and achievements", "proof of work"});
        RULES.put(FieldKey.EXPERIENCE, new String[]{"experience", "work experience", "professional experience", "background"});
        RULES.put(FieldKey.LANGUAGES, new String[]{"languages", "language skills", "spoken languages"});
        RULES.put(FieldKey.TOOLS, new String[]{"tools", "software", "technologies", "apps used"});
        RULES.put(FieldKey.SKILLS, new String[]{"skills", "expertise", "specialties", "competencies", "keywords"});
        RULES.put(FieldKey.HEADLINE, new String[]{"professional headline", "profile headline", "headline", "professional title", "profile title", "occupation", "title"});
        RULES.put(FieldKey.SUMMARY, new String[]{"profile summary", "professional summary", "overview", "about me", "about", "bio", "description", "introduction"});
        RULES.put(FieldKey.LOCATION, new String[]{"location", "city", "city and country", "where are you based", "address city"});
        RULES.put(FieldKey.FULL_NAME, new String[]{"full name", "display name", "your name", "name"});
    }

    private FieldMatcher() {}

    public static String normalize(CharSequence value) {
        if (value == null) return "";
        return value.toString().toLowerCase(Locale.ROOT)
                .replace('_', ' ').replace('-', ' ').replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    public static boolean isSensitive(String descriptor) {
        String normalized = normalize(descriptor);
        for (String token : SENSITIVE) {
            if (normalized.contains(token)) return true;
        }
        return false;
    }

    public static FieldKey match(String descriptor) {
        String normalized = normalize(descriptor);
        if (normalized.isEmpty() || isSensitive(normalized)) return null;
        for (Map.Entry<FieldKey, String[]> entry : RULES.entrySet()) {
            for (String token : entry.getValue()) {
                if (normalized.equals(token) || normalized.contains(token)) return entry.getKey();
            }
        }
        return null;
    }

    public static boolean isSubmissionAction(String descriptor) {
        String normalized = normalize(descriptor);
        String[] actions = {
                "submit proposal", "submit application", "send proposal", "send application",
                "place bid", "submit bid", "apply now", "send offer", "publish application"
        };
        for (String action : actions) {
            if (normalized.equals(action) || normalized.contains(action)) return true;
        }
        return false;
    }

    public static String detectPlatform(String packageName, String domain, String selected) {
        String haystack = normalize((packageName == null ? "" : packageName) + " " + (domain == null ? "" : domain));
        if (haystack.contains("freelancer")) return "Freelancer";
        if (haystack.contains("upwork")) return "Upwork";
        if (haystack.contains("fiverr")) return "Fiverr";
        if (haystack.contains("linkedin")) return "LinkedIn";
        return selected == null || selected.trim().isEmpty() ? "Freelancer" : selected;
    }
}
