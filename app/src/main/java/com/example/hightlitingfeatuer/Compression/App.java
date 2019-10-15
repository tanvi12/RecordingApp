package com.example.hightlitingfeatuer.Compression;

import android.app.Application;

import com.example.hightlitingfeatuer.AndroidAudioConverter;
import com.example.hightlitingfeatuer.callback.ILoadCallback;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidAudioConverter.load(this, new ILoadCallback() {
            @Override
            public void onSuccess() {
                // Great!
            }

            @Override
            public void onFailure(Exception error) {
                // FFmpeg is not supported by device
                error.printStackTrace();
            }
        });
    }
}