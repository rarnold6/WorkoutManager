package com.example.workoutManager.stravaConnection;
import android.content.Context;

import com.example.workoutManager.data.Workout;
import com.garmin.fit.*;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

public class FitFile {

    public static void generateFitFile(Context context, Workout workout, LinkedHashMap<Long, Integer> heartRateHashMap){
        try {

            java.io.File externalDir = context.getExternalFilesDir(null);
            java.io.File file = new java.io.File(externalDir, "file.fit");
            String filePath = file.getAbsolutePath();
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    System.out.println("File deleted successfully: " + filePath);
                } else {
                    System.out.println("Failed to delete file: " + filePath);
                }
            } else {
                System.out.println("File not found: " + filePath);
            }

            externalDir = context.getExternalFilesDir(null);
            file  = new java.io.File(externalDir, "file.fit");


            //file = new java.io.File(context.getFilesDir(), "file.fit");
            //java.io.File file = new java.io.File("/Download/", "file.fit");
            //java.io.File file = new java.io.File("C:\\Users\\Arnol\\AndroidStudioProjects", "file.fit");
            FileEncoder encoder = new FileEncoder(file, Fit.ProtocolVersion.V2_0);

            DateTime startDateTime = new DateTime(new Date(workout.getStartTimestamp()));

            // Initialize the file ID message (mandatory)
            FileIdMesg fileIdMesg = new FileIdMesg();
            fileIdMesg.setType(File.ACTIVITY);
            fileIdMesg.setManufacturer(Manufacturer.GARMIN);
            fileIdMesg.setProduct(1);
            fileIdMesg.setTimeCreated(new DateTime(new Date(workout.getStartTimestamp())));
            encoder.write(fileIdMesg);

            // adding session message
            SessionMesg sessionMesg = new SessionMesg();
            sessionMesg.setSport(Sport.HIIT);
            sessionMesg.setSubSport(SubSport.HIIT);
            sessionMesg.setTimestamp(startDateTime);
            sessionMesg.setStartTime(startDateTime);
            sessionMesg.setTotalElapsedTime((float) ((workout.getEndTimestamp() - workout.getStartTimestamp()) / 1000.0)); // Total time in seconds
            sessionMesg.setTotalTimerTime((float) ((workout.getEndTimestamp() - workout.getStartTimestamp()) / 1000.0));
            sessionMesg.setAvgHeartRate((short) calculateAverageHeartRate(heartRateHashMap));
            sessionMesg.setTotalDistance(0.0f); // Optional: Set total distance if applicable
            sessionMesg.setTotalCalories(0);    // Optional: Total calories burned
            encoder.write(sessionMesg);



            // Activity start time (find the minimum timestamp)
            //long startTime = heartRateHashMap.keySet().stream().min(Long::compareTo).orElse(System.currentTimeMillis());
            long startDateTimeTimestamp = startDateTime.getTimestamp();
            System.out.println("STARTTIMESTAMP = "+ workout.getStartTimestamp());
            System.out.println("ENDTIMESTAMP = "+ workout.getEndTimestamp());

            long shouldRelativeSecond = 0;
            int lastHeartRate = 60;
            int index = 0;
            for (Long timestamp : heartRateHashMap.keySet()) {

                if(timestamp / 1000  <  workout.getStartTimestamp() / 1000){
                    index++;
                    continue;
                }

                int heartRate = heartRateHashMap.get(timestamp);

                long timeStampDateTime = new DateTime(new Date(timestamp)).getTimestamp();
                long relativeTimeInSeconds = timeStampDateTime - startDateTimeTimestamp;

                if(shouldRelativeSecond > relativeTimeInSeconds){
                    relativeTimeInSeconds = shouldRelativeSecond;
                }

                if(shouldRelativeSecond < relativeTimeInSeconds){
                    interpolateHeartRate(lastHeartRate, heartRate, startDateTime, shouldRelativeSecond, relativeTimeInSeconds, encoder);
                    shouldRelativeSecond = relativeTimeInSeconds;
                }

                if(index == heartRateHashMap.size()-1){
                    long restTimeInSeconds = workout.getEndTimestamp() / 1000 - workout.getStartTimestamp() - relativeTimeInSeconds -1;
                    for(int i = 1; i <= restTimeInSeconds; i++){
                        RecordMesg recordMesg = new RecordMesg();
                        recordMesg.setTimestamp(new DateTime(startDateTime.getTimestamp() + relativeTimeInSeconds + i));
                        recordMesg.setHeartRate((short) heartRate);
                        recordMesg.setSpeed(0.0f); // Placeholder speed
                        recordMesg.setDistance(0.0f); // Placeholder distance

                        encoder.write(recordMesg);
                    }
                }

                RecordMesg recordMesg = new RecordMesg();
                recordMesg.setTimestamp(new DateTime(startDateTime.getTimestamp() + relativeTimeInSeconds));
                recordMesg.setHeartRate((short) heartRate);
                recordMesg.setSpeed(0.0f); // Placeholder speed
                recordMesg.setDistance(0.0f); // Placeholder distance

                encoder.write(recordMesg);
                shouldRelativeSecond++;
                lastHeartRate = heartRate;
                index++;
            }
            // Close the encoder
            encoder.close();
            System.out.println("FIT file successfully generated: " + file.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error generating FIT file: " + e.getMessage());
        }
    }

    private static void interpolateHeartRate(int lastHeartRate, int heartRate, DateTime startDateTime, long shouldRelativeSecond, long relativeTimeInSeconds, FileEncoder encoder) {
        double incrementalIncreaseHeartRate = ((heartRate-lastHeartRate)/(double)(relativeTimeInSeconds-shouldRelativeSecond+1));
        for(int i = 0; shouldRelativeSecond + i < relativeTimeInSeconds; i++){
            System.out.println("Interpolation shouldseconds: "+ shouldRelativeSecond);
            RecordMesg recordMesg = new RecordMesg();
            recordMesg.setTimestamp(new DateTime(startDateTime.getTimestamp() + shouldRelativeSecond + i));
            recordMesg.setHeartRate((short) (lastHeartRate + (i+1) * incrementalIncreaseHeartRate));
            recordMesg.setSpeed(0.0f); // Placeholder speed
            recordMesg.setDistance(0.0f); // Placeholder distance

            encoder.write(recordMesg);
        }
    }

    private static double calculateAverageHeartRate(LinkedHashMap<Long, Integer> heartRateHashMap) {
        double avgHeartRate = 0;

        for(long timestamp : heartRateHashMap.keySet()){
            avgHeartRate += heartRateHashMap.get(timestamp);
        }
        return avgHeartRate = (double) avgHeartRate / heartRateHashMap.size();
    }


}
