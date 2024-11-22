package com.example.workoutManager.stravaConnection;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.workoutManager.data.Workout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StravaUpload {

    private static final String CLIENT_ID = "xxx"; // Replace with your Strava Client ID
    private static final String CLIENT_SECRET = "xxx"; // Replace with your Strava Client Secret
    private static final String REDIRECT_URI = "workoutmanager://workoutmanager"; // Replace with your redirect URI
    private static final String TAG = "StravaActivity";

    private Workout workout;

    private String accessToken = "";

    private String refreshToken = "";

    private int athleteId;

    private long expirationDate;

    private Context context;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ExecutorService executorRefreshToken = Executors.newSingleThreadExecutor();

    private final ExecutorService executorUpload = Executors.newSingleThreadExecutor();;

    private final CountDownLatch tokenLatch = new CountDownLatch(1);

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public StravaUpload(Context context, Workout workout){
        this.workout = workout;
        this.context = context;
        
        retrieveStoredInformation();

        if(accessToken.isEmpty()) {
            authenticateUser();
        } else if(new Date(new Date().getTime() + 5*60*1000).after(new Date(expirationDate*1000))){
            refreshAccessToken();
        }

        uploadWorkout();

    }


    private void retrieveStoredInformation() {
        SecureStorageHelper secureStorageHelper = new SecureStorageHelper(context);
        accessToken = secureStorageHelper.getAccessToken();
        refreshToken = secureStorageHelper.getRefreshToken();
        athleteId = secureStorageHelper.getAthleteId();
        expirationDate = secureStorageHelper.getExpirationDate();
    }
    private void refreshAccessToken() {
        executorRefreshToken.submit(() -> {
            try {
                URL url = new URL("https://www.strava.com/oauth/token");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                String postData = "client_id=" + CLIENT_ID
                        + "&client_secret=" + CLIENT_SECRET
                        + "&grant_type=refresh_token"
                        + "&refresh_token=" + refreshToken;

                connection.getOutputStream().write(postData.getBytes());

                InputStream inputStream = connection.getInputStream();
                String response = convertInputStreamToString(inputStream);

                JSONObject jsonResponse = new JSONObject(response);

                storeTokensAndExpireDates(jsonResponse);

                tokenLatch.countDown();

                // Update UI on main thread if needed
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!accessToken.isEmpty()) {
                        Log.d(TAG, "Access token obtained: " + accessToken);
                    }
                });


            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error exchanging authorization code for token", e);
            }
        });
    }


    private void authenticateUser() {
        String authUrl = "https://www.strava.com/oauth/mobile/authorize?client_id=" + CLIENT_ID
                + "&response_type=code&redirect_uri=" + REDIRECT_URI
                + "&scope=activity:write";

        Uri intentUri = Uri.parse("https://www.strava.com/oauth/mobile/authorize")
                .buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("approval_prompt", "auto")
                .appendQueryParameter("scope", "activity:write,read")
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, intentUri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void handleRedirect(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            String code = uri.getQueryParameter("code");
            if (code != null) {
                exchangeAuthorizationCodeForToken(code);
            }
        }
    }

    private void exchangeAuthorizationCodeForToken(String authorizationCode) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                URL url = new URL("https://www.strava.com/oauth/token");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                String postData = "client_id=" + CLIENT_ID
                        + "&client_secret=" + CLIENT_SECRET
                        + "&code=" + URLEncoder.encode(authorizationCode, "UTF-8")
                        + "&grant_type=authorization_code";

                connection.getOutputStream().write(postData.getBytes());

                InputStream inputStream = connection.getInputStream();
                String response = convertInputStreamToString(inputStream);

                JSONObject jsonResponse = new JSONObject(response);
                accessToken = jsonResponse.getString("access_token");

                storeTokensAndExpireDates(jsonResponse);

                tokenLatch.countDown();

                // Update UI on main thread if needed
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!accessToken.isEmpty()) {
                        Log.d(TAG, "Access token obtained: " + accessToken);
                    }
                });


            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error exchanging authorization code for token", e);
            }
        });


    }

    private void storeTokensAndExpireDates(JSONObject jsonResponse) throws JSONException {
        SecureStorageHelper secureStorageHelper = new SecureStorageHelper(context);
        accessToken = jsonResponse.getString("access_token");
        refreshToken = jsonResponse.getString("refresh_token");
        expirationDate = jsonResponse.getLong("expires_at");

        JSONObject athlete = jsonResponse.optJSONObject("athlete");
        if(athlete != null){
            athleteId = jsonResponse.optInt("id");
        }

        secureStorageHelper.saveStravaData(accessToken,refreshToken,athleteId,expirationDate);
    }

    private void uploadWorkout(){

        executorUpload.submit(() -> {
            try {
                if(accessToken.isEmpty() || tokenLatch.getCount()>0){
                    tokenLatch.await();
                }

                double duration = (double) workout.getNumberOfSets() *
                        (workout.getNumberOfExercisesPerSet() * workout.getExerciseDuration() + (workout.getNumberOfExercisesPerSet() - 1) * workout.getRecoveryTime()) +
                        (workout.getNumberOfSets() - 1) * workout.getBreakTime();
                uploadActivity(accessToken, "HIIT", (int)duration);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Upload workout task was interrupted", e);
            } // Example activity
        });
    }

    private void uploadActivity(String accessToken, String activityName, int duration) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            int responseCode = 0;
            try {
                URL url = new URL("https://www.strava.com/api/v3/activities");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");


                String description = "Autoupload:\nExercise Duration: "+ workout.getExerciseDuration() +
                        "s, Recovery Time: " + workout.getRecoveryTime() + "s\n" + workout.getNumberOfSets() +
                        " sets of each " + workout.getNumberOfExercisesPerSet() + " exercises.";

                // Get current date in the correct format for start_date_local
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
                // Get the current date and time in Germany
                Date currentDate = new Date();

                // Subtract the duration from the current time
                long updatedTimeInMillis = (long) (currentDate.getTime() - (duration * 1000));
                Date updatedDate = new Date(updatedTimeInMillis);
                String startDate = dateFormat.format(updatedDate);

                // Building the POST data
                String postData = "name=" + URLEncoder.encode(activityName, "UTF-8")
                        + "&type=Workout"
                        + "&start_date_local=" + URLEncoder.encode(startDate, "UTF-8")
                        + "&elapsed_time=" + duration
                        + "&description=" + description
                        + "&trainer=0"
                        + "&commute=0"
                        + "&access_token=" + accessToken;

                // Writing the POST data
                connection.getOutputStream().write(postData.getBytes());

                // Reading the response
                responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    // Successful upload
                    InputStream inputStream = connection.getInputStream();
                    String response1 = convertInputStreamToString(inputStream);
                    Log.d(TAG, "Activity upload successful: " + response1);

                } else {
                    // Error - log error response
                    InputStream errorStream = connection.getErrorStream();
                    String errorResponse = convertInputStreamToString(errorStream);
                    Log.e(TAG, "Error uploading activity, code: " + responseCode + ", message: " + errorResponse);
                }

            } catch (IOException e) {
                Log.e(TAG, "Error uploading activity", e);
            } finally {
                countDownLatch.countDown();
                executor.shutdown();  // Shut down the executor to avoid resource leaks


            }
        });
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }


    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }
}
