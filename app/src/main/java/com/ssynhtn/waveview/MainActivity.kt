package com.ssynhtn.waveview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var waveView: WaveView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        waveView = findViewById(R.id.wave_three)
        waveView.addDefaultWaves(2, 1)

        waveView.startAnimation()
    }

    override fun onResume() {
        super.onResume()

        waveView.resumeAnimation()
    }

    override fun onPause() {
        super.onPause()

        waveView.pauseAnimation()
    }
}
