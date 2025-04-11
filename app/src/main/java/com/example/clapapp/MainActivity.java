package com.example.clapapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensorProximity;
    private MediaPlayer mediaPlayer;
    private TextView displayTextView;
    private TextView instructionTextView;
    private Button closeButton;
    private boolean canClap = true;
    private boolean isNear = false;
    private static final int COOLDOWN_TIME = 500; // Cooldown in milliseconds
    private Vibrator vibrator;
    private int clapCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        displayTextView = findViewById(R.id.displayTextView);
        instructionTextView = findViewById(R.id.instructionTextView);
        closeButton = findViewById(R.id.closeButton);

        // Close button click listener
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeApp(v);
            }
        });

        // Initialize sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Check if proximity sensor exists
        if (sensorProximity == null) {
            displayTextView.setText("Proximity sensor not available");
            instructionTextView.setText("This device doesn't support the clap app");
            return;
        }

        // Initialize MediaPlayer with clap sound
        mediaPlayer = MediaPlayer.create(this,  R.raw.clap_sound);

        // Initialize vibrator for haptic feedback
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        displayTextView.setText("Ready to clap");
        instructionTextView.setText("Move your hand near and away from the sensor to clap");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorProximity != null) {
            sensorManager.registerListener(this, sensorProximity, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];

        // Near state detected (hand close to sensor)
        if (distance < event.sensor.getMaximumRange() / 2) {
            if (!isNear) {
                isNear = true;
                // We don't play the sound when hand comes near, only when it moves away
                displayTextView.setText("Hand Near");
            }
        }
        // Far state detected (hand moved away)
        else {
            if (isNear && canClap) {
                // Play clap sound when hand moved away after being near
                playClap();
                isNear = false;
                clapCount++;
                displayTextView.setText("CLAP! Count: " + clapCount);
            } else if (!isNear) {
                displayTextView.setText("Ready to clap");
            }
        }
    }

    private void playClap() {
        if (canClap) {
            // Prevent multiple claps in quick succession
            canClap = false;

            // Play the clap sound
            if (mediaPlayer != null) {
                // Reset to start of the audio file
                try {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                } catch (IllegalStateException e) {
                    // If media player is in error state, recreate it
                    mediaPlayer = MediaPlayer.create(this, R.raw.clap_sound);
                    mediaPlayer.start();
                }
            }

            // Add haptic feedback
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(50); // Short vibration
            }

            // Set cooldown timer
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    canClap = true;
                }
            }, COOLDOWN_TIME);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this app
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void closeApp(View v) {
        finish();
    }
}