package com.example.workoutManager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.workoutManager.data.Workout;
import com.example.workoutManager.stravaConnection.SecureStorageHelper;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class StravaConnectActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "xxx"; // Replace with your Strava Client ID
    private static final String CLIENT_SECRET = "xxx"; // Replace with your Strava Client Secret
    private static final String REDIRECT_URI = "workoutmanager://workoutmanager"; // Replace with your redirect URI

    private static final String TAG = "StravaConnectActivity";

    private String accessToken = "";

    private String refreshToken = "";

    private int athleteId;

    private long expirationDate;

    private boolean authenticated;

    private String username = "";

    private final ExecutorService authenticateAndUpload = Executors.newSingleThreadExecutor();

    private final ExecutorService executorUpload = Executors.newSingleThreadExecutor();

    private CountDownLatch tokenLatch = new CountDownLatch(1);

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private Workout workout;

    private LinkedHashMap<Long,Integer> heartRateHashMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.strava_connect);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.strava_connect), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Get existing padding values from the view
            int existingPaddingLeft = v.getPaddingLeft();
            int existingPaddingTop = v.getPaddingTop();
            int existingPaddingRight = v.getPaddingRight();
            int existingPaddingBottom = v.getPaddingBottom();

            // Add the system bars insets to the existing padding
            v.setPadding(
                    existingPaddingLeft + systemBars.left,
                    existingPaddingTop + systemBars.top,
                    existingPaddingRight + systemBars.right,
                    existingPaddingBottom + systemBars.bottom
            );

            return insets;
        });

        // Handle back button press using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Explicitly finish the activity when back is pressed
                finish();
            }
        });


        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null){
            System.out.println("EXtras");
            this.workout = (Workout) extras.getSerializable("workout");
            HashMap<Long, Integer> hashMap = (HashMap<Long, Integer>) intent.getSerializableExtra("heartRateData");
            if(hashMap != null) {
                List<Long> sortedKeys = new ArrayList<>(hashMap.keySet());
                Collections.sort(sortedKeys); // Sort keys in ascending order
                for (Long timestamp : sortedKeys) {
                    heartRateHashMap.put(timestamp, hashMap.get(timestamp));
                }
            }
        }

        ImageButton btnBackStrava = (ImageButton) findViewById(R.id.btnBackStrava);
        if(workout != null){
            btnBackStrava.setVisibility(View.GONE);
        }
        btnBackStrava.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StravaConnectActivity.this, Settings.class);
                startActivity(intent);
                finish();
            }
        });

        retrieveStoredInformation();

        TextView tvStravaConnected = (TextView) findViewById(R.id.tvStravaConnected);
        TextView tvStravaConnectedDescription = (TextView) findViewById(R.id.tvStravaConnectedDescription);

        ImageView ivConnectWithStrava = (ImageView) findViewById(R.id.ivConnectWithStrava);
        ivConnectWithStrava.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Authenticated: " + authenticated);
                if(authenticated){
                    authenticated = false;

                    accessToken = "";
                    refreshToken = "";
                    athleteId = -1;
                    expirationDate = -1;
                    username = "";

                    SecureStorageHelper secureStorageHelper = new SecureStorageHelper(getApplicationContext());
                    secureStorageHelper.resetStravaData();

                    tvStravaConnected.setText("Your are not connected to Strava yet!");
                    tvStravaConnectedDescription.setText("If you are connected to Strava you can automatically upload your HIIT.");
                    ivConnectWithStrava.setImageResource(R.drawable.btn_strava_connectwith_orange);

                } else {
                    authenticated = true;

                    authenticateUser();

                    System.out.println("USER AUTHENTICATED " + username);

                    if(workout != null){
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                tokenLatch.await();
                                uploadWorkout();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        });

                        ExecutorService executorEnd = Executors.newSingleThreadExecutor();
                        executorEnd.submit(() -> {
                            try {
                                countDownLatch.await();
                                runOnUiThread(() -> {
                                    runOnUiThread(() -> {
                                        Toast.makeText(getApplicationContext(), "Activity uploaded!",Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(StravaConnectActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();});
                                });
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } else {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                tokenLatch.await();
                                runOnUiThread(() -> {
                                    Toast.makeText(getApplicationContext(), "Successfully authenticated!",Toast.LENGTH_SHORT).show();
                                    tvStravaConnected.setText("Your are connected to Strava as:\n" + username);
                                    tvStravaConnectedDescription.setText("After completing your HIIT, you can automatically upload it.");
                                    ivConnectWithStrava.setImageResource(R.drawable.disconnect_btn);
                                    });
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        tokenLatch = new CountDownLatch(1);
                    }
                }
            }
        });


        if(accessToken.isEmpty()) {
            authenticated = false;
        } else {
            authenticated = true;
            tvStravaConnected.setText("You are connected to Strava as:\n" + username);
            tvStravaConnectedDescription.setText("If you are connected to Strava you can automatically upload your HIIT.");
            ivConnectWithStrava.setImageResource(R.drawable.disconnect_btn);
        }

    }

    private void retrieveStoredInformation() {
        SecureStorageHelper secureStorageHelper = new SecureStorageHelper(getApplicationContext());
        accessToken = secureStorageHelper.getAccessToken();
        refreshToken = secureStorageHelper.getRefreshToken();
        athleteId = secureStorageHelper.getAthleteId();
        expirationDate = secureStorageHelper.getExpirationDate();
        username = secureStorageHelper.getUsername();
    }


    private void authenticateUser() {
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
        getApplicationContext().startActivity(intent);
    }

    public void handleRedirect(Intent intent) {
        System.out.println("HANDLING REDIRECT!");
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
        SecureStorageHelper secureStorageHelper = new SecureStorageHelper(getApplicationContext());
        accessToken = jsonResponse.getString("access_token");
        refreshToken = jsonResponse.getString("refresh_token");
        expirationDate = jsonResponse.getLong("expires_at");

        JSONObject athlete = jsonResponse.optJSONObject("athlete");
        if(athlete != null){
            athleteId = athlete.optInt("id");
            username = athlete.optString("username");
            if(username.isEmpty() || username.equals("null")){
                String firstName = athlete.optString("firstname");
                String lastName = athlete.optString("lastname");
                username = firstName + lastName;
            }
        }

        secureStorageHelper.saveStravaData(accessToken,refreshToken,athleteId,expirationDate,username);
        tokenLatch.countDown();

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

    private void uploadWorkout(){

        executorUpload.submit(() -> {
            double duration = (double) workout.getNumberOfSets() *
                    (workout.getNumberOfExercisesPerSet() * workout.getExerciseDuration() + (workout.getNumberOfExercisesPerSet() - 1) * workout.getRecoveryTime()) +
                    (workout.getNumberOfSets() - 1) * workout.getBreakTime();
            uploadActivity(accessToken, "HIIT", (int)duration);

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

                    java.io.File externalDir = getApplicationContext().getExternalFilesDir(null);
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



    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleRedirect(intent);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Calls the default behavior
        finish(); // Explicitly finish the activity
    }

}
