package com.example.workoutManager.stravaConnection;

import android.content.Context;
import androidx.security.crypto.MasterKeys;
import androidx.security.crypto.EncryptedSharedPreferences;
import android.content.SharedPreferences;
import android.util.Log;

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
            Log.e("SecureStorageHelper", "Error initializing EncryptedSharedPreferences", e);
            e.printStackTrace();
        }
    }
    public void saveStravaData(String accessToken, String refreshToken, int athleteId, long expirationDate, String username) {
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putString("access_token", accessToken);
        editor.putString("refresh_token", refreshToken);
        editor.putInt("athlete_id", athleteId);
        editor.putLong("expiration_date", expirationDate);
        editor.putString("username", username);
        editor.apply();  // Apply changes asynchronously
    }

    public void saveSensor(String sensorName, String address){
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putString("sensor_name", sensorName);
        editor.putString("sensor_address", address);
        editor.apply();
    }

    public void resetSensor(){
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putString("sensor_name", "");
        editor.putString("sensor_address", "");
        editor.apply();
    }

    public void resetStravaData(){
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putString("access_token", "");
        editor.putString("refresh_token", "");
        editor.putInt("athlete_id", -1);
        editor.putLong("expiration_date", -1);
        editor.putString("username", "");
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

    public String getUsername() {
        return encryptedPrefs.getString("username", "");
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

    public String getHeartRateSensor()  {
        return encryptedPrefs.getString("sensor_name", "");
    }

    public String getHeartRateSensorAddress()  {
        return encryptedPrefs.getString("sensor_address", "");
    }
}
