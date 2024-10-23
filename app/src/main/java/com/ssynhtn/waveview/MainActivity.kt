package com.ssynhtn.waveview;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private WaveView waveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        waveView = findViewById(R.id.wave_three);
        waveView.addDefaultWaves(2, 1);

        waveView.startAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        waveView.resumeAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();

        waveView.pauseAnimation();
    }
}
