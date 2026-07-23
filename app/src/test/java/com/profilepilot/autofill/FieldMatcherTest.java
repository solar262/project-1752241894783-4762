package com.profilepilot.autofill;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FieldMatcherTest {
    @Test public void recognisesCoreProfileFields() {
        assertEquals(FieldMatcher.FieldKey.HEADLINE, FieldMatcher.match("Professional headline"));
        assertEquals(FieldMatcher.FieldKey.SUMMARY, FieldMatcher.match("Profile overview / About me"));
        assertEquals(FieldMatcher.FieldKey.SKILLS, FieldMatcher.match("skills and expertise"));
        assertEquals(FieldMatcher.FieldKey.PORTFOLIO_DESCRIPTION, FieldMatcher.match("Project description"));
    }

    @Test public void recognisesApplicationFields() {
        assertEquals(FieldMatcher.FieldKey.PROPOSAL, FieldMatcher.match("Describe your proposal"));
        assertEquals(FieldMatcher.FieldKey.BID_AMOUNT, FieldMatcher.match("Your bid amount"));
        assertEquals(FieldMatcher.FieldKey.DELIVERY_DAYS, FieldMatcher.match("Days to deliver"));
    }

    @Test public void blocksSensitiveFields() {
        assertTrue(FieldMatcher.isSensitive("Credit card number"));
        assertTrue(FieldMatcher.isSensitive("One time verification code"));
        assertTrue(FieldMatcher.isSensitive("Passport identity document"));
        assertNull(FieldMatcher.match("Password"));
    }

    @Test public void recognisesSubmissionActionsWithoutMatchingOrdinaryButtons() {
        assertTrue(FieldMatcher.isSubmissionAction("Submit proposal"));
        assertTrue(FieldMatcher.isSubmissionAction("Place Bid now"));
        assertTrue(!FieldMatcher.isSubmissionAction("Open project details"));
    }

    @Test public void detectsTargetPlatforms() {
        assertEquals("Freelancer", FieldMatcher.detectPlatform("com.android.chrome", "www.freelancer.com", "Upwork"));
        assertEquals("LinkedIn", FieldMatcher.detectPlatform("com.linkedin.android", "", "Freelancer"));
        assertEquals("Fiverr", FieldMatcher.detectPlatform("com.android.chrome", "", "Fiverr"));
    }
}
