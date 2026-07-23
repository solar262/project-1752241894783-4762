package com.profilepilot.autofill;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProfileDataTest {
    @Test public void defaultProfileIsComplete() {
        ProfileData profile = ProfileData.defaults();
        assertFalse(profile.fullName.trim().isEmpty());
        assertFalse(profile.location.trim().isEmpty());
        assertTrue(profile.skills.split(",").length >= 10);
        assertFalse(profile.experience.trim().isEmpty());
        assertFalse(profile.achievements.trim().isEmpty());
    }

    @Test public void everyPlatformHasTransferReadyContent() {
        ProfileData profile = ProfileData.defaults();
        for (String platform : new String[]{"Freelancer", "Upwork", "Fiverr", "LinkedIn"}) {
            Map<FieldMatcher.FieldKey, String> values = profile.valuesFor(platform);
            assertFalse(values.get(FieldMatcher.FieldKey.HEADLINE).trim().isEmpty());
            assertFalse(values.get(FieldMatcher.FieldKey.SUMMARY).trim().isEmpty());
            assertFalse(values.get(FieldMatcher.FieldKey.SERVICE_DESCRIPTION).trim().isEmpty());
            assertFalse(values.get(FieldMatcher.FieldKey.PORTFOLIO_DESCRIPTION).trim().isEmpty());
        }
    }
}
