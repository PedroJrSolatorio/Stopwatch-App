package com.example.stopwatchapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView timerTextView;
    private Button primaryActionButton, pauseButton, resetButton; // Renamed buttons for clarity
    private ListView lapTimesListView;
    private CircularTimerView circularTimerView; // Custom view for the circle

    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;

    private Handler handler = new Handler();
    private boolean isRunning = false; // To track if the stopwatch is running

    private ArrayList<String> lapTimesList;
    private ArrayAdapter<String> lapTimesAdapter;
    private int lapCounter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        timerTextView = findViewById(R.id.timerTextView);
        primaryActionButton = findViewById(R.id.primaryActionButton); // Renamed ID
        pauseButton = findViewById(R.id.pauseButton); // Renamed ID
        resetButton = findViewById(R.id.resetButton);
        lapTimesListView = findViewById(R.id.lapTimesListView);
        circularTimerView = findViewById(R.id.circularTimerView);

        // Initialize lap times list and adapter
        lapTimesList = new ArrayList<>();
        lapTimesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, lapTimesList);
        lapTimesListView.setAdapter(lapTimesAdapter);

        // Set initial state of buttons
        primaryActionButton.setText("Start");
        pauseButton.setText("Pause");
        pauseButton.setEnabled(false); // Pause button disabled initially
        resetButton.setEnabled(false); // Reset button disabled initially


        // Set OnClickListener for the Primary Action button (Start/Lap/Resume)
        primaryActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) { // If stopwatch is not running (Start or Resume state)
                    startTime = SystemClock.uptimeMillis(); // Get current system uptime
                    handler.postDelayed(updateTimeTask, 0); // Start updating the timer
                    isRunning = true;

                    // Update button states and text
                    primaryActionButton.setText("Lap"); // Change to Lap
                    pauseButton.setEnabled(true); // Enable Pause button
                    resetButton.setEnabled(true); // Enable Reset button
                    // The pauseButton text should always be "Pause" when enabled
                    pauseButton.setText("Pause");
                } else { // If stopwatch is running (Lap state)
                    recordLap();
                }
            }
        });

        // Set OnClickListener for the Pause button
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) { // If stopwatch is running (Pause state)
                    timeSwapBuff += timeInMilliseconds; // Add current time to buffer
                    handler.removeCallbacks(updateTimeTask); // Stop updating the timer
                    isRunning = false;

                    // Update button states and text
                    primaryActionButton.setText("Resume"); // Change primary action to Resume
                    primaryActionButton.setEnabled(true); // Ensure Resume is clickable
                    pauseButton.setEnabled(false); // Disable Pause button
                }
            }
        });

        // Set OnClickListener for the Reset button
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset all time variables
                startTime = 0L;
                timeInMilliseconds = 0L;
                timeSwapBuff = 0L;
                updatedTime = 0L;

                handler.removeCallbacks(updateTimeTask); // Stop updating if running
                isRunning = false;

                // Reset TextView to initial state
                timerTextView.setText("00:00:00"); // Display MM:SS:ms
                circularTimerView.updateTime(0); // Reset custom view

                // Clear lap times
                lapTimesList.clear();
                lapTimesAdapter.notifyDataSetChanged();
                lapCounter = 1;

                // Update button states and text to initial state
                primaryActionButton.setEnabled(true);
                primaryActionButton.setText("Start");
                pauseButton.setEnabled(false); // Disable Pause button
                pauseButton.setText("Pause"); // Reset text to Pause
                resetButton.setEnabled(false); // Disable Reset button
            }
        });
    }

    // Runnable to update the timer TextView and custom view
    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            // Calculate elapsed time since start or resume
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            // Add buffered time (from pauses) to current elapsed time
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int minutes = (int) (updatedTime / 60000); // Total minutes
            int seconds = (int) ((updatedTime / 1000) % 60); // Seconds within the current minute
            int milliseconds = (int) ((updatedTime % 1000) / 10); // Milliseconds (two digits)

            // Format the time and update the TextView (MM:SS:ms)
            timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    minutes, seconds, milliseconds));

            // Update the custom circular timer view
            circularTimerView.updateTime(updatedTime);

            handler.postDelayed(this, 0); // Schedule itself to run again as fast as possible
        }
    };

    // Method to record lap times
    private void recordLap() {
        // Get the current time displayed on the stopwatch
        String lapTime = timerTextView.getText().toString();
        lapTimesList.add(String.format(Locale.getDefault(), "Lap %d: %s", lapCounter++, lapTime));
        lapTimesAdapter.notifyDataSetChanged();
        lapTimesListView.smoothScrollToPosition(lapTimesAdapter.getCount() - 1); // Scroll to latest lap
    }
}