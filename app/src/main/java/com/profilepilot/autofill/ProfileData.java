package com.profilepilot.autofill;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.EnumMap;
import java.util.Map;

public final class ProfileData {
    public String fullName;
    public String location;
    public String primaryService;
    public String experience;
    public String skills;
    public String tools;
    public String languages;
    public String achievements;
    public String preferredClients;
    public String selectedPlatform;
    public String currentJobTitle;
    public String currentJobUrl;
    public String currentProposal;
    public String currentBidAmount;
    public String currentCurrency;
    public String currentDeliveryDays;
    public String currentClientNeed;
    public String currentHandoffId;
    public String currentSyncId;
    public String currentSyncToken;
    public String currentSyncBaseUrl;
    public String currentSyncStatus;
    public String currentSyncDetail;
    public long currentSyncUpdatedAt;

    public static ProfileData defaults() {
        ProfileData p = new ProfileData();
        p.fullName = "Mark Bishop";
        p.location = "Klagenfurt am Wörthersee, Austria";
        p.primaryService = "AI-Assisted Research, Content, Documents & App Support";
        p.experience = "I create practical digital products and business assets using AI-assisted workflows. My work includes online research, content writing, presentations, spreadsheets, website and product copy, ecommerce content, app planning, workflow documentation, testing and quality review. I have taken multiple standalone web apps, online stores and digital products from initial concept through build, testing and launch.";
        p.skills = "Online Research, Content Writing, Copywriting, Presentation Design, Document Creation, Spreadsheet Preparation, AI-Assisted App Building, Workflow Automation, Product Descriptions, Website Content, Ecommerce Content, Testing and Quality Review";
        p.tools = "ChatGPT, Canva, Microsoft Word, Microsoft Excel, PowerPoint, Google Workspace, Shopify, GitHub, AI App Builders, Automation Tools";
        p.languages = "English (native), German (learning)";
        p.achievements = "Built and launched multiple AI-assisted web apps, ecommerce projects and digital workflows. Created business research systems, professional reports, presentations, product pages, buyer guides, spreadsheets and structured content for real projects. Published books under the name Mark Bishop and developed digital products from idea through testing and launch.";
        p.preferredClients = "Small businesses, founders, consultants, creators, ecommerce brands and agencies needing clear research, professional content, digital documents or practical workflow support.";
        p.selectedPlatform = "Freelancer";
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
        return p;
    }

    public Map<FieldMatcher.FieldKey, String> valuesFor(String platform) {
        String target = platform == null ? selectedPlatform : platform;
        EnumMap<FieldMatcher.FieldKey, String> values = new EnumMap<>(FieldMatcher.FieldKey.class);
        values.put(FieldMatcher.FieldKey.FULL_NAME, fullName);
        values.put(FieldMatcher.FieldKey.LOCATION, location);
        values.put(FieldMatcher.FieldKey.SKILLS, skills);
        values.put(FieldMatcher.FieldKey.TOOLS, tools);
        values.put(FieldMatcher.FieldKey.LANGUAGES, languages);
        values.put(FieldMatcher.FieldKey.EXPERIENCE, experience);
        values.put(FieldMatcher.FieldKey.ACHIEVEMENTS, achievements);
        values.put(FieldMatcher.FieldKey.PREFERRED_CLIENTS, preferredClients);
        values.put(FieldMatcher.FieldKey.HEADLINE, headline(target));
        values.put(FieldMatcher.FieldKey.SUMMARY, summary(target));
        values.put(FieldMatcher.FieldKey.SERVICE_TITLE, serviceTitle(target));
        values.put(FieldMatcher.FieldKey.SERVICE_DESCRIPTION, serviceDescription(target));
        values.put(FieldMatcher.FieldKey.PORTFOLIO_TITLE, "AI-Assisted Business Content and Document Portfolio");
        values.put(FieldMatcher.FieldKey.PORTFOLIO_DESCRIPTION, "A collection of professional research, presentations, spreadsheets, website content, product copy and workflow documents created for real digital projects. Each deliverable is structured for clarity, practical use and easy handover.");
        values.put(FieldMatcher.FieldKey.PROPOSAL, currentProposal);
        values.put(FieldMatcher.FieldKey.BID_AMOUNT, currentBidAmount);
        values.put(FieldMatcher.FieldKey.DELIVERY_DAYS, currentDeliveryDays);
        return values;
    }

    public boolean hasConnectedJob() {
        return currentJobUrl != null && !currentJobUrl.trim().isEmpty() && currentProposal != null && !currentProposal.trim().isEmpty();
    }

    public boolean hasSync() {
        return currentSyncId != null && !currentSyncId.trim().isEmpty()
                && currentSyncToken != null && !currentSyncToken.trim().isEmpty()
                && currentSyncBaseUrl != null && currentSyncBaseUrl.startsWith("https://");
    }

    private String headline(String platform) {
        if ("Fiverr".equalsIgnoreCase(platform)) return "Research, Content, Presentations and Business Documents";
        if ("LinkedIn".equalsIgnoreCase(platform)) return "AI-Assisted Digital Product Builder | Research, Content, Documents & Workflow Support";
        if ("Upwork".equalsIgnoreCase(platform)) return "Research, Content, Presentation & Document Specialist";
        return "AI Content, Research, Presentation & Document Specialist";
    }

    private String summary(String platform) {
        String base = "I create clear, professional digital deliverables for businesses, founders, creators and agencies. My work includes research reports, presentations, spreadsheets, website and product copy, ecommerce content, workflow documentation and practical app support. I use efficient AI-assisted workflows combined with careful review to produce accurate, well-structured and ready-to-use work. I follow each brief closely, communicate clearly and deliver in the required format.";
        if ("LinkedIn".equalsIgnoreCase(platform)) return base + " I also develop and test standalone web apps, ecommerce projects and digital products from initial concept through launch.";
        if ("Fiverr".equalsIgnoreCase(platform)) return base + " Services can be tailored as a focused one-off delivery or a complete content and document package.";
        return base;
    }

    private String serviceTitle(String platform) {
        if ("Fiverr".equalsIgnoreCase(platform)) return "I will create professional research, content, presentations or business documents";
        if ("Upwork".equalsIgnoreCase(platform)) return "Professional Research, Content and Business Document Package";
        return "Professional Digital Content and Document Support";
    }

    private String serviceDescription(String platform) {
        return "Receive a professional, ready-to-use deliverable built around your brief. Available work includes online research, reports, presentations, spreadsheets, website copy, product descriptions, scripts, buyer guides, workflow documentation and structured content packages. Files can be supplied in Word, PDF, PowerPoint, Excel or another agreed format. Every delivery is reviewed for clarity, consistency and practical usability.";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("fullName", fullName); j.put("location", location); j.put("primaryService", primaryService);
        j.put("experience", experience); j.put("skills", skills); j.put("tools", tools);
        j.put("languages", languages); j.put("achievements", achievements);
        j.put("preferredClients", preferredClients); j.put("selectedPlatform", selectedPlatform);
        j.put("currentJobTitle", currentJobTitle); j.put("currentJobUrl", currentJobUrl);
        j.put("currentProposal", currentProposal); j.put("currentBidAmount", currentBidAmount);
        j.put("currentCurrency", currentCurrency); j.put("currentDeliveryDays", currentDeliveryDays);
        j.put("currentClientNeed", currentClientNeed); j.put("currentHandoffId", currentHandoffId);
        j.put("currentSyncId", currentSyncId); j.put("currentSyncToken", currentSyncToken);
        j.put("currentSyncBaseUrl", currentSyncBaseUrl); j.put("currentSyncStatus", currentSyncStatus);
        j.put("currentSyncDetail", currentSyncDetail); j.put("currentSyncUpdatedAt", currentSyncUpdatedAt);
        return j;
    }

    public static ProfileData fromJson(String json) throws JSONException {
        JSONObject j = new JSONObject(json);
        ProfileData d = defaults();
        d.fullName = j.optString("fullName", d.fullName);
        d.location = j.optString("location", d.location);
        d.primaryService = j.optString("primaryService", d.primaryService);
        d.experience = j.optString("experience", d.experience);
        d.skills = j.optString("skills", d.skills);
        d.tools = j.optString("tools", d.tools);
        d.languages = j.optString("languages", d.languages);
        d.achievements = j.optString("achievements", d.achievements);
        d.preferredClients = j.optString("preferredClients", d.preferredClients);
        d.selectedPlatform = j.optString("selectedPlatform", d.selectedPlatform);
        d.currentJobTitle = j.optString("currentJobTitle", d.currentJobTitle);
        d.currentJobUrl = j.optString("currentJobUrl", d.currentJobUrl);
        d.currentProposal = j.optString("currentProposal", d.currentProposal);
        d.currentBidAmount = j.optString("currentBidAmount", d.currentBidAmount);
        d.currentCurrency = j.optString("currentCurrency", d.currentCurrency);
        d.currentDeliveryDays = j.optString("currentDeliveryDays", d.currentDeliveryDays);
        d.currentClientNeed = j.optString("currentClientNeed", d.currentClientNeed);
        d.currentHandoffId = j.optString("currentHandoffId", d.currentHandoffId);
        d.currentSyncId = j.optString("currentSyncId", d.currentSyncId);
        d.currentSyncToken = j.optString("currentSyncToken", d.currentSyncToken);
        d.currentSyncBaseUrl = j.optString("currentSyncBaseUrl", d.currentSyncBaseUrl);
        d.currentSyncStatus = j.optString("currentSyncStatus", d.currentSyncStatus);
        d.currentSyncDetail = j.optString("currentSyncDetail", d.currentSyncDetail);
        d.currentSyncUpdatedAt = j.optLong("currentSyncUpdatedAt", d.currentSyncUpdatedAt);
        return d;
    }
}
