package com.example.workoutManager.stravaConnection;

import android.content.Context;
import androidx.security.crypto.MasterKeys;
import androidx.security.crypto.EncryptedSharedPreferences;
import android.content.SharedPreferences;

public class SecureStorageHelper {
    private static final String PREFS_FILE = "secure_prefs";
    private SharedPreferences encryptedPrefs;

    public SecureStorageHelper(Context context) {
        try {
            // Create a master key if it doesn't already exist
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            // Initialize EncryptedSharedPreferences
            encryptedPrefs = EncryptedSharedPreferences.create(
                    PREFS_FILE,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveStravaData(String accessToken, String refreshToken, int athleteId, long expirationDate) {
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putString("access_token", accessToken);
        editor.putString("refresh_token", refreshToken);
        editor.putInt("athlete_id", athleteId);
        editor.putLong("expiration_date", expirationDate);
        editor.apply();  // Apply changes asynchronously
    }

    public void saveStandardSettings(int durationExercise, int recoveryTime, int breakDuration, int numberOfSets, int numberOfExercisesPerSet){
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putInt("duration_exercise", durationExercise);
        editor.putInt("recovery_time", recoveryTime);
        editor.putInt("break_duration", breakDuration);
        editor.putInt("number_of_exercises_per_set", numberOfExercisesPerSet);
        editor.putInt("number_of_sets", numberOfSets);
        editor.apply();  // Apply changes asynchronously
    }

    public String getAccessToken() {
        return encryptedPrefs.getString("access_token", "");
    }

    public String getRefreshToken() {
        return encryptedPrefs.getString("refresh_token", "");
    }

    public int getAthleteId() {
        return encryptedPrefs.getInt("athlete_id",-1);
    }

    public long getExpirationDate() {
        return encryptedPrefs.getLong("expiration_date", 0);
    }

    public int getDurationExercise() {
        return encryptedPrefs.getInt("duration_exercise", 40);
    }

    public int getRecoveryTime() {
        return encryptedPrefs.getInt("recovery_time", 20);
    }

    public int getBreakDuration() {
        return encryptedPrefs.getInt("break_duration", 120);
    }

    public int getNumberOfExercisesPerSet() {
        return encryptedPrefs.getInt("number_of_exercises_per_set", 8);
    }

    public int getNumberOfSets() {
        return encryptedPrefs.getInt("number_of_sets", 2);
    }

}
