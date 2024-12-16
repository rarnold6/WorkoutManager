package com.example.workoutManager;

import com.garmin.fit.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class FitFileDecoderTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        // Redirect System.out to capture listener output
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        // Restore System.out
        System.setOut(originalOut);
    }

    @Test
    public void testFitFileDecoding() {
        try {
            // Path to the test FIT file (ensure it exists in your project test resources)
            String filePath = "C:\\Users\\Arnol\\AndroidStudioProjects\\file.fit";

            // Call the decode method
            FitFileDecoder.decodeFitFile(filePath);

            // Wait for a short duration to ensure the listeners process outputs
            Thread.sleep(5000);

            // Verify the output contains expected values
            String output = outContent.toString();
            assertTrue("Output should contain File ID Message", output.contains("File ID Message:"));
            assertTrue("Output should contain Record Message", output.contains("Record Message:"));
            assertTrue("Output should contain Heart Rate", output.contains("Heart Rate:"));

            System.out.println("Test Passed: Output verification completed.");
        } catch (Exception e) {
            fail("Test failed due to exception: " + e.getMessage());
        }
    }


}

