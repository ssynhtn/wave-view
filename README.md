# Android Wave Animation
<img src="device-2018-04-12-180529.png" width="400" alt="wave view preview">



# Usage
Well, I personal recommend that you clone the project and import the library module so that you can freely modify it as you wish

However if you could use the gradle dependency as well

In your root build.gradle, add these

    allprojects {
        repositories {
            maven { url 'https://dl.bintray.com/ssynhtn-org/android-wave-view' }

        }
    }

then in the app module's build.gradle, add these

    dependencies {
        implementation 'com.ssynhtn:wave-view:1.0'
    }

Then do this in your Activity

    waveView = findViewById(R.id.wave_view);
    waveView.addDefaultWaves(2, 1); // or call WaveView#addWaveData to add wave data as you like
    waveView.startAnimation();

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