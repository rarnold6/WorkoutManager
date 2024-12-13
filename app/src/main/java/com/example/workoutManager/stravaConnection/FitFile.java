package com.example.workoutManager.stravaConnection;
import com.example.workoutManager.data.Workout;
import com.garmin.fit.*;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

public class FitFile {

    public static void generateFitFile(String filename, Workout workout, LinkedHashMap<Long, Integer> heartRateHashMap){
        try {
            FileEncoder encoder = new FileEncoder(new java.io.File(filename), Fit.ProtocolVersion.V2_0);

            // Initialize the file ID message (mandatory)
            FileIdMesg fileIdMesg = new FileIdMesg();
            fileIdMesg.setType(File.ACTIVITY);
            fileIdMesg.setManufacturer(Manufacturer.DEVELOPMENT);
            fileIdMesg.setProduct(1);
            fileIdMesg.setTimeCreated(new DateTime(System.currentTimeMillis() / 1000));
            encoder.write(fileIdMesg);

            // Activity start time (find the minimum timestamp)
            //long startTime = heartRateHashMap.keySet().stream().min(Long::compareTo).orElse(System.currentTimeMillis());
            long startTime = workout.getStartTimestamp();
            long endTime = workout.getEndTimestamp();
            DateTime startDateTime = new DateTime(startTime / 1000);

            for (Long timestamp : heartRateHashMap.keySet()) {
                int heartRate = heartRateHashMap.get(timestamp);

                long relativeTimeInSeconds = (timestamp - startTime) / 1000;

                RecordMesg recordMesg = new RecordMesg();
                recordMesg.setTimestamp(new DateTime(startDateTime.getTimestamp() + relativeTimeInSeconds));
                recordMesg.setHeartRate((short) heartRate);

                encoder.write(recordMesg);

            }
            // Close the encoder
            encoder.close();
            System.out.println("FIT file successfully generated: " + filename);

        } catch (Exception e) {
            System.err.println("Error generating FIT file: " + e.getMessage());
        }


    }


}
