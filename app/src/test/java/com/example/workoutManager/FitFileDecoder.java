package com.example.workoutManager;

import com.garmin.fit.Decode;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.FileIdMesgListener;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.MesgListener;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;

import java.io.FileInputStream;
import java.io.InputStream;

public class FitFileDecoder {
    public static void decodeFitFile(String filePath) {
        try {
            InputStream inputStream = new FileInputStream(filePath);
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

            System.out.println("Starting FIT file decoding...");

            // Debug listener for all messages
            broadcaster.addListener(new MesgListener() {
                @Override
                public void onMesg(Mesg mesg) {
                    System.out.println("General Message: " + mesg.getName());
                }
            });

            broadcaster.addListener(new FileIdMesgListener() {
                @Override
                public void onMesg(FileIdMesg mesg) {
                    System.out.println("File ID Message:");
                    System.out.println("Type: " + mesg.getType());
                    System.out.println("Manufacturer: " + mesg.getManufacturer());
                    System.out.println("Time Created: " + mesg.getTimeCreated());
                }
            });

            broadcaster.addListener(new RecordMesgListener() {
                @Override
                public void onMesg(RecordMesg mesg) {
                    System.out.println("Record Message:");
                    System.out.println("Timestamp: " + mesg.getTimestamp());
                    System.out.println("Heart Rate: " + mesg.getHeartRate());
                }
            });

            if (!decode.checkFileIntegrity(inputStream)) {
                System.err.println("FIT file integrity check failed.");
                return;
            }

            decode.read(inputStream, broadcaster, broadcaster);

            System.out.println("FIT file successfully decoded.");
        } catch (Exception e) {
            System.err.println("Error decoding FIT file: " + e.getMessage());
        }
    }

}
