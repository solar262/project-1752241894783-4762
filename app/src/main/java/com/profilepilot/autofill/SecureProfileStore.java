package com.profilepilot.autofill;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecureProfileStore {
    private static final String PREFS = "profilepilot_secure";
    private static final String KEY_ALIAS = "profilepilot_profile_key_v1";
    private static final String CIPHERTEXT = "profile_ciphertext";
    private static final String IV = "profile_iv";
    private static final String ARMED = "accessibility_armed";

    private final SharedPreferences preferences;

    public SecureProfileStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized ProfileData load() {
        String cipherText = preferences.getString(CIPHERTEXT, null);
        String iv = preferences.getString(IV, null);
        if (cipherText == null || iv == null) return ProfileData.defaults();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            byte[] plain = cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP));
            return ProfileData.fromJson(new String(plain, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return ProfileData.defaults();
        }
    }

    public synchronized void save(ProfileData profile) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] encrypted = cipher.doFinal(profile.toJson().toString().getBytes(StandardCharsets.UTF_8));
        preferences.edit()
                .putString(CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .apply();
    }

    public boolean isAccessibilityArmed() {
        return preferences.getBoolean(ARMED, false);
    }

    public void setAccessibilityArmed(boolean armed) {
        preferences.edit().putBoolean(ARMED, armed).apply();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
