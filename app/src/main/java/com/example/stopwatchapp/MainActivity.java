package com.example.stopwatchapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView timerTextView;
    private Button primaryActionButton, pauseButton, resetButton; // Renamed buttons for clarity
    private ListView lapTimesListView;
    private CircularTimerView circularTimerView; // Custom view for the circle

    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L; // This will now store the total elapsed time when paused
    private long updatedTime = 0L; // Current time to display

    private Handler handler = new Handler();
    private boolean isRunning = false; // To track if the stopwatch is running

    private ArrayList<String> lapTimesList;
    private ArrayAdapter<String> lapTimesAdapter;
    private int lapCounter = 1;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "StopwatchPrefs";
    private static final String KEY_TIME_SWAP_BUFF = "timeSwapBuff"; // Total time elapsed (paused/running)
    private static final String KEY_IS_RUNNING = "isRunning";
    private static final String KEY_LAP_TIMES = "lapTimes";
    private static final String KEY_LAP_COUNTER = "lapCounter";
    private static final String KEY_LAST_SAVE_TIMESTAMP = "lastSaveTimestamp"; // Wall clock time when saved

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize UI elements
        timerTextView = findViewById(R.id.timerTextView);
        primaryActionButton = findViewById(R.id.primaryActionButton);
        pauseButton = findViewById(R.id.pauseButton);
        resetButton = findViewById(R.id.resetButton);
        lapTimesListView = findViewById(R.id.lapTimesListView);
        circularTimerView = findViewById(R.id.circularTimerView);

        // Initialize lap times list and adapter
        lapTimesList = new ArrayList<>();
        lapTimesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, lapTimesList);
        lapTimesListView.setAdapter(lapTimesAdapter);

        // Load saved state
        loadStopwatchState();

        // Set initial state of buttons based on loaded data
        updateButtonStates();


        // Set OnClickListener for the Primary Action button (Start/Lap/Resume)
        primaryActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) { // If stopwatch is not running (Start or Resume state)
                    // When resuming, startTime is the current uptime minus the buffered time
                    // This makes `SystemClock.uptimeMillis() - startTime` yield `timeSwapBuff`
                    startTime = SystemClock.uptimeMillis() - timeSwapBuff;
                    handler.postDelayed(updateTimeTask, 0); // Start updating the timer
                    isRunning = true;
                } else { // If stopwatch is running (Lap state)
                    recordLap();
                }
                updateButtonStates(); // Update button states after action
            }
        });

        // Set OnClickListener for the Pause button
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) { // If stopwatch is running (Pause state)
                    // Store the current updatedTime into timeSwapBuff
                    // This is the total elapsed time until the pause moment
                    timeSwapBuff = updatedTime;
                    handler.removeCallbacks(updateTimeTask); // Stop updating the timer
                    isRunning = false;
                }
                updateButtonStates(); // Update button states after action
            }
        });

        // Set OnClickListener for the Reset button
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset all time variables
                startTime = 0L;
                timeInMilliseconds = 0L;
                timeSwapBuff = 0L; // Reset buffered time
                updatedTime = 0L; // Reset displayed time

                handler.removeCallbacks(updateTimeTask); // Stop updating if running
                isRunning = false;

                // Reset TextView to initial state
                timerTextView.setText("00:00:00");
                circularTimerView.updateTime(0); // Reset custom view

                // Clear lap times
                lapTimesList.clear();
                lapTimesAdapter.notifyDataSetChanged();
                lapCounter = 1;

                // Clear saved data
                clearSavedState();

                updateButtonStates(); // Update button states after action
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save state when the app goes to background
        saveStopwatchState();
        // If the stopwatch was running, stop the handler to prevent background updates
        if (isRunning) {
            handler.removeCallbacks(updateTimeTask);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If the stopwatch was running when paused/closed, resume it
        if (isRunning) {
            // Recalculate startTime based on the current uptimeMillis and the total elapsed time (timeSwapBuff)
            // This ensures continuity from where it left off, accounting for background time.
            startTime = SystemClock.uptimeMillis() - timeSwapBuff;
            handler.postDelayed(updateTimeTask, 0); // Resume updating
        }
        updateButtonStates(); // Update button states after resuming
    }

    // Runnable to update the timer TextView and custom view
    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            // Calculate elapsed time since startTime (either initial start or resume)
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            // The `updatedTime` is the `timeInMilliseconds` (current run) + `timeSwapBuff` (previous runs/pauses)
            updatedTime = timeInMilliseconds; // updatedTime is now the current run's elapsed time + any previous buffered time from timeSwapBuff

            // To ensure the timer continues from `timeSwapBuff` when `startTime` is set for resume:
            // updatedTime = timeSwapBuff + (SystemClock.uptimeMillis() - startTime);

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

    // Helper method to update button states based on 'isRunning'
    private void updateButtonStates() {
        if (isRunning) {
            primaryActionButton.setText("Lap");
            primaryActionButton.setEnabled(true); // Always enabled when running to allow laps
            pauseButton.setText("Pause");
            pauseButton.setEnabled(true);
            resetButton.setEnabled(true);
        } else {
            // If timer is not running, check if it's 00:00:00 (initial/reset state) or paused
            if (updatedTime == 0L) { // Initial/Reset state
                primaryActionButton.setText("Start");
                primaryActionButton.setEnabled(true);
                pauseButton.setEnabled(false);
                resetButton.setEnabled(false);
            } else { // Paused state
                primaryActionButton.setText("Resume");
                primaryActionButton.setEnabled(true);
                pauseButton.setEnabled(false); // Pause button is disabled when paused
                resetButton.setEnabled(true); // Reset button enabled when paused
            }
            // The pause button text should always show "Pause" even if it's disabled.
            // It will be enabled and functional when the timer is running.
            // When paused, the primary button becomes "Resume".
            // So, this line is not needed here as 'Pause' is its default label.
            // pauseButton.setText("Pause");
        }
    }

    // Method to save stopwatch state to SharedPreferences
    private void saveStopwatchState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_TIME_SWAP_BUFF, updatedTime); // Save the total time that has passed
        editor.putBoolean(KEY_IS_RUNNING, isRunning);
        editor.putInt(KEY_LAP_COUNTER, lapCounter);
        editor.putLong(KEY_LAST_SAVE_TIMESTAMP, System.currentTimeMillis()); // Save wall clock time

        // Convert lapTimesList to JSON string
        Gson gson = new Gson();
        String jsonLapTimes = gson.toJson(lapTimesList);
        editor.putString(KEY_LAP_TIMES, jsonLapTimes);

        editor.apply(); // Apply changes asynchronously
    }

    // Method to load stopwatch state from SharedPreferences
    private void loadStopwatchState() {
        timeSwapBuff = sharedPreferences.getLong(KEY_TIME_SWAP_BUFF, 0L);
        isRunning = sharedPreferences.getBoolean(KEY_IS_RUNNING, false);
        lapCounter = sharedPreferences.getInt(KEY_LAP_COUNTER, 1);
        long lastSaveTimestamp = sharedPreferences.getLong(KEY_LAST_SAVE_TIMESTAMP, 0L);

        // Load lapTimesList from JSON string
        Gson gson = new Gson();
        String jsonLapTimes = sharedPreferences.getString(KEY_LAP_TIMES, null);
        if (jsonLapTimes != null) {
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            lapTimesList.addAll(gson.fromJson(jsonLapTimes, type));
            lapTimesAdapter.notifyDataSetChanged();
        }

        // If the stopwatch was running when last closed, calculate the elapsed time during closure
        if (isRunning && lastSaveTimestamp > 0) {
            long timeInBackground = System.currentTimeMillis() - lastSaveTimestamp;
            timeSwapBuff += timeInBackground; // Add background elapsed time to the buffered time
        }

        // Update display with loaded or adjusted time
        updatedTime = timeSwapBuff; // Set the current displayed time to the loaded/adjusted buffered time
        int minutes = (int) (updatedTime / 60000);
        int seconds = (int) ((updatedTime / 1000) % 60);
        int milliseconds = (int) ((updatedTime % 1000) / 10);
        timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
                minutes, seconds, milliseconds));
        circularTimerView.updateTime(updatedTime);
    }

    // Method to clear all saved state from SharedPreferences
    private void clearSavedState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Clears all data in this SharedPreferences file
        editor.apply();
    }
}