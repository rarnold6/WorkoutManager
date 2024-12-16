package com.example.workoutManager;

import static com.example.workoutManager.stravaConnection.FitFile.generateFitFile;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import android.content.Context;

import org.mockito.Mockito;

import com.example.workoutManager.data.Workout;
import com.example.workoutManager.stravaConnection.SecureStorageHelper;
import com.garmin.fit.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    private static final String CLIENT_ID = "139499"; // Replace with your Strava Client ID
    private static final String CLIENT_SECRET = "10dfdd2304497a9f8abe7c7665670c6ba98b843e"; // Replace with your Strava Client Secret

    @Test
    public void checkUploadTest() throws IOException {
        URL url = new URL("https://www.strava.com/api/v3/uploads/" + "13978809891" + "?access_token=" + "2983dba3850849242f01625f9f21e351d9cd4c61");
        HttpURLConnection statusConn = (HttpURLConnection) url.openConnection();
        statusConn.setRequestMethod("GET");

        int statusResponseCode = statusConn.getResponseCode();
        InputStream statusStream = statusConn.getInputStream();
        BufferedReader statusReader = new BufferedReader(new InputStreamReader(statusStream));
        StringBuilder statusResponse = new StringBuilder();

        String line;
        while ((line = statusReader.readLine()) != null) {
            statusResponse.append(line);
        }
        statusReader.close();

        System.out.println("Upload Status Response: " + statusResponse.toString());

        assertEquals(statusResponseCode,200);
    }

    @Test
    public void checkFitFileTest() {
        Context mockContext = Mockito.mock(Context.class);
        Date startDate = new Date();
        long startTime = startDate.getTime();
        LinkedHashMap<Long,Integer> heartRateMockingHashMap = new LinkedHashMap<>();
        for(int i = 0; i < 50; i++){
            if(i > 10 &&  i < 40) {
                long timestamp = startTime + 1000 * i;
                int heartRate = ((int) (Math.random() * 20) + 70);
                heartRateMockingHashMap.put(timestamp, heartRate);
            }
        }

        generateFitFile(mockContext,new Workout(10,3,60,1,3,startTime),heartRateMockingHashMap);
        assertEquals(1,1);
    }

    @Test
    public void decodeAndPrintFitFileTest(){
        FitFileDecoder.decodeFitFile("/data/user/0/com.example.workoutManager/files/file.fit");

    }

    @Test
    public void figureOutTimeGarmin(){
        Date startDate = new Date();
        long startTime = startDate.getTime();
        DateTime dateTime = new DateTime(startDate);
        System.out.println(dateTime);
        System.out.println((new DateTime(startTime/1000)));
    }


}