package com.example.workoutManager.stravaConnection;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.workoutManager.Settings;
import com.example.workoutManager.StravaConnectActivity;
import com.example.workoutManager.data.Workout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
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

    private final ExecutorService executorUpload = Executors.newSingleThreadExecutor();

    private CountDownLatch tokenLatch = new CountDownLatch(1);

    private boolean accessTokenExpired = false;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private LinkedHashMap<Long,Integer> heartRateHashMap;

    public StravaUpload(Context context, Workout workout, LinkedHashMap<Long,Integer> heartRateHashMap){
        this.workout = workout;
        this.context = context;
        this.heartRateHashMap = heartRateHashMap;
        
        retrieveStoredInformation();

        if(new Date(new Date().getTime() + 5*60*1000).after(new Date(expirationDate*1000))){
            accessTokenExpired = true;
            refreshAccessToken();
            uploadWorkout();
        } else {
            accessTokenExpired = false;
            uploadWorkout();
        }



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
        String username = "";
        if(athlete != null){
            athleteId = jsonResponse.optInt("id");
            username = jsonResponse.optString("username");
        }

        secureStorageHelper.saveStravaData(accessToken,refreshToken,athleteId,expirationDate,username);
    }

    private void uploadWorkout(){
        executorUpload.submit(() -> {
            try {
                if(tokenLatch.getCount()>0 && accessTokenExpired){
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
                if(heartRateHashMap.isEmpty() || heartRateHashMap.size() < duration*0.5) {
                    System.out.println("HEART RATE HASH MAP HAS ENTRIES!");
                    URL url = new URL("https://www.strava.com/api/v3/activities");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");


                    String description = "Autoupload:\nExercise Duration: " + workout.getExerciseDuration() +
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
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
                    //File file = new File(context.getFilesDir(), "file.fit");
                    //String filePath = file.getAbsolutePath();

                    java.io.File externalDir = context.getExternalFilesDir(null);
                    java.io.File file = new java.io.File(externalDir, "file.fit");
                    String filePath = file.getAbsolutePath();

                    String startDate = dateFormat.format(new Date(workout.getStartTimestamp()));
                    String description = "Autoupload:\nExercise Duration: " + workout.getExerciseDuration() +
                            "s, Recovery Time: " + workout.getRecoveryTime() + "s\n" + workout.getNumberOfSets() +
                            " sets of each " + workout.getNumberOfExercisesPerSet() + " exercises.";
                    uploadFitFile(filePath, accessToken, activityName, startDate, duration, description);


                }

            } catch (IOException e) {
                Log.e(TAG, "Error uploading activity", e);
            } finally {

                countDownLatch.countDown();
                tokenLatch = new CountDownLatch(1);
                executor.shutdown();  // Shut down the executor to avoid resource leaks


            }
        });
    }

    private void uploadFitFile(String filePath, String accessToken, String activityName, String startDate, int duration, String description) {
        String boundary = "===" + System.currentTimeMillis() + "==="; // Unique boundary for multipart
        String LINE_FEED = "\r\n";
        HttpURLConnection httpConn = null;

        try {
            // Step 1: Create the connection
            URL url = new URL("https://www.strava.com/api/v3/uploads");
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            httpConn.setRequestProperty("Authorization", "Bearer " + accessToken);

            OutputStream outputStream = httpConn.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

            // Step 2: Add form fields
            addFormField(writer, "name", activityName, boundary, LINE_FEED);
            addFormField(writer, "type", "Workout", boundary, LINE_FEED);
            addFormField(writer, "start_date_local", startDate, boundary, LINE_FEED);
            addFormField(writer, "elapsed_time", String.valueOf(duration), boundary, LINE_FEED);
            addFormField(writer, "description", description, boundary, LINE_FEED);
            addFormField(writer, "trainer", "0", boundary, LINE_FEED);
            addFormField(writer, "commute", "0", boundary, LINE_FEED);
            addFormField(writer,"data_type","fit",boundary,LINE_FEED);

            // Step 3: Add the file
            addFilePart(writer, outputStream, "file", new File(filePath), boundary, LINE_FEED);

            // Step 4: Finish the request
            writer.append("--").append(boundary).append("--").append(LINE_FEED);
            writer.flush();
            writer.close();

            // Step 5: Get the response

            int responseCode = httpConn.getResponseCode();
            System.out.println("Response code: " + responseCode);

            InputStream inputStream;

            if (responseCode >= 400) {
                // For error responses, use the error stream
                inputStream = httpConn.getErrorStream();
            } else {
                inputStream = httpConn.getInputStream();
            }

// Read the input stream (response body)
            BufferedReader reader1 = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response1 = new StringBuilder();
            String line1;

            while ((line1 = reader1.readLine()) != null) {
                response1.append(line1);
            }
            reader1.close();

// Print the full response body for debugging
            System.out.println("Response Code: " + responseCode);
            System.out.println("Response Body: " + response1.toString());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    // Method to add a regular form field
    private void addFormField(PrintWriter writer, String name, String value, String boundary, String LINE_FEED) {
        writer.append("--").append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
        writer.append(LINE_FEED).append(value).append(LINE_FEED);
        writer.flush();
    }

    // Method to add a file part
    private static void addFilePart(PrintWriter writer, OutputStream outputStream, String fieldName, File uploadFile, String boundary, String LINE_FEED) throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--").append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"").append(fieldName).append("\"; filename=\"").append(fileName).append("\"").append(LINE_FEED);
        writer.append("Content-Type: application/octet-stream").append(LINE_FEED); // Adjust MIME type if needed
        writer.append(LINE_FEED);
        writer.flush();

        // Write file content
        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();
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
