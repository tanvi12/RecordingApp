package com.example.hightlitingfeatuer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.hightlitingfeatuer.callback.IConvertCallback;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;
import omrecorder.AudioChunk;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.Recorder;



public class MainActivity_Recorder extends AppCompatActivity implements PullTransport.OnAudioChunkPulledListener {
    String currentDateTime;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private boolean isRecording = false;
    private boolean isStarted = false;
    private Recorder recorder;
    private boolean shouldDeleteLastFile = false;
    private Handler handler;
    int seconds;
    int startSeconds;
    int endSeconds;
    //Modify seconds you want to record before start recording
    int SECOONDSTORECORDBEFORE = 10;
    ImageView audiobar;
    View someView;
    VisualizerView visualizerView;

    private MediaRecorder tempRecorder = null;
    @Override
    protected void onStart() {
        super.onStart();
        // link to visualizer

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main__recorder);
        audiobar = findViewById(R.id.audiobar);
        if (ContextCompat.checkSelfPermission(MainActivity_Recorder.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity_Recorder.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 100);

        }
        foldercreating();
        someView = findViewById(R.id.the_id);
        visualizerView = (VisualizerView) findViewById(R.id.visualizer);



        handler = new Handler();
        setButtonHandlers();


        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

//        Analytics analytics = new Analytics.Builder(this, "faqSTNty32d7fF9pyxb4ozQ8wBy12ajC")
//                // Enable this to record certain application events automatically!
//                .trackApplicationLifecycleEvents()
//                // Enable this to record screen views automatically!
//                .recordScreenViews()
//                .build();
//
//        // Set the initialized instance as a globally accessible instance.
//        Analytics.setSingletonInstance(analytics);

//        analytics.track("I am just here");
    }

    private void setButtonHandlers() {
        ((ImageView) findViewById(R.id.button_start)).setOnClickListener(btnClick);
        ((ImageView) findViewById(R.id.button_start)).setOnLongClickListener(btnLongClick);
    }

    int bufferSize = 1024 * 2; // want to play 2048 (2K) since 2 bytes we use only 1024

    File currentFile;
    File tempFile;



    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            isStarted = false;
            recorder.stopRecording();
            recorder = null;
            visualizerView.clear();
            recorder = null;
        }
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        @SuppressLint("StaticFieldLeak")
        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.button_start: {

                    if (!isRecording) {

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        currentDateTime = dateFormat.format(new Date()); // Find todays date

                        currentFile = new File(getExternalFilesDir(null),
                                "recording" + System.currentTimeMillis() + ".wav");
                        recorder = OmRecorder.wav(
                                new PullTransport.Default(Util.getMic(AudioSource.MIC, AudioChannel.STEREO, AudioSampleRate.HZ_8000), MainActivity_Recorder.this),
                                currentFile);
                        recorder.startRecording();


                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                audiobar.setVisibility(View.VISIBLE);

                                TextView taptohighlit = findViewById(R.id.taptohighlit);


                                taptohighlit.setText("Tap to Highlight");

                                seconds++;
                                handler.postDelayed(this, 1000);
                            }
                        }, 0);
                        isRecording = true;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast toast1 = Toast.makeText(getApplicationContext(),
                                        "Listening Start  ", Toast.LENGTH_SHORT);
                                toast1.setGravity(Gravity.CENTER_HORIZONTAL, 0, 90);
                                toast1.show();

                            }
                        });
                    } else {
                        if (isStarted) {
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected void onPreExecute() {
                                    super.onPreExecute();
                                }

                                @Override
                                protected Void doInBackground(Void... voids) {
                                    endSeconds = seconds;
                                    recorder.stopRecording();
                                    shouldDeleteLastFile = isStarted;
                                    saveFile(false);
                                    isStarted = false;

                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Void aVoid) {
                                    super.onPostExecute(aVoid);


                                    TextView taptohighlit = findViewById(R.id.taptohighlit);

                                    taptohighlit.setText("Tap to Highlight");
                                }
                            }.execute();


                        } else {
                            startSeconds = seconds;
                            Toast toast1 = Toast.makeText(getApplicationContext(),
                                    "Highlighting  Started:", Toast.LENGTH_SHORT);
                            toast1.setGravity(Gravity.CENTER_HORIZONTAL, 0, 90);
                            toast1.show();
                            TextView taptohighlit = findViewById(R.id.taptohighlit);
                            taptohighlit.setText("Tap to Save Snippet");
//                            console.setText(console.getText() + "\nRecording started: " + timeText());
//                            ((Button) v).setText("Save");

                            isStarted = true;
                        }
                    }
                    break;
                }
            }
        }
    };

    private View.OnLongClickListener btnLongClick = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View view) {
            if (isRecording) {
                shouldDeleteLastFile = isStarted;
                if (isStarted) {
                    isStarted = false;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            Toast toast1 = Toast.makeText(getApplicationContext(),
                                    "Highlighting end :", Toast.LENGTH_SHORT);
                            toast1.setGravity(Gravity.CENTER_HORIZONTAL, 0, 90);
                            toast1.show();
//                            console.setText(console.getText() + "\nRecording ended: " + timeText());
                        }
                    });
                }
                handler.post(
                        new Runnable() {
                            @Override
                            public void run() {

                                Toast toast1 = Toast.makeText(getApplicationContext(),
                                        "Listening end :", Toast.LENGTH_SHORT);
                                toast1.setGravity(Gravity.CENTER_HORIZONTAL, 0, 90);
                                toast1.show();
                                Toast.makeText(MainActivity_Recorder.this, "Session completed", Toast.LENGTH_SHORT).show();
//                                console.setText(console.getText() + "\nListening ended: " + timeText());
                            }
                        }
                );

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                    }

                    @Override
                    protected Void doInBackground(Void... voids) {
                        handler.removeCallbacksAndMessages(null);
                        endSeconds = seconds;
                        stopRecording();
                        saveFile(true);

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);

                    }
                }.execute();

            }
            TextView taptohighlit = findViewById(R.id.taptohighlit);

            taptohighlit.setText("Tap to Start Listen");
            Toast toast1 = Toast.makeText(getApplicationContext(),
                    "Listening end :", Toast.LENGTH_SHORT);
            toast1.setGravity(Gravity.CENTER_HORIZONTAL, 0, 90);
            toast1.show();
            Toast.makeText(MainActivity_Recorder.this, "Session completed", Toast.LENGTH_SHORT).show();
            audiobar.setVisibility(View.GONE);

//            ((Button) view).setText("Listen");
            return true;
        }
    };

    private void saveFile(boolean isStopepd) {
        final CheapSoundFile.ProgressListener listener = new CheapSoundFile.ProgressListener() {
            public boolean reportProgress(double frac) {
                Log.d("Progress", frac + "");
                return true;
            }
        };
        CheapSoundFile cheapSoundFile = null;
        try {
            cheapSoundFile = CheapSoundFile.create(currentFile.getPath(), listener);

            int mSampleRate = cheapSoundFile.getSampleRate();

            int mSamplesPerFrame = cheapSoundFile.getSamplesPerFrame();

            int startFrame;

            if (startSeconds > SECOONDSTORECORDBEFORE)
                startFrame = UtilSave.secondsToFrames(startSeconds - SECOONDSTORECORDBEFORE, mSampleRate, mSamplesPerFrame);
            else {
                startFrame = UtilSave.secondsToFrames(0, mSampleRate, mSamplesPerFrame);
            }
            int endFrame = UtilSave.secondsToFrames(endSeconds, mSampleRate, mSamplesPerFrame);

            Log.d("Start seconds", startSeconds + " " + endSeconds);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            currentDateTime = dateFormat.format(new Date()); // Find todays date
            final File file1 = new File(Environment.getExternalStorageDirectory().toString()
                    + "/TapTap/" + "mytaptap", currentDateTime + ".wav");

            cheapSoundFile.WriteFile(file1, startFrame, endFrame - startFrame);
            currentFile.delete();



            if (!shouldDeleteLastFile) {
                file1.delete();
            }


            IConvertCallback callback = new IConvertCallback() {
                @Override
                public void onSuccess(File convertedFile) {
                    file1.delete();
                }

                @Override
                public void onFailure(Exception error) {
                }
            };

            AndroidAudioConverter.with(this)
                    .setFile(file1)
                    .setFormat(com.example.hightlitingfeatuer.model.AudioFormat.MP3)
                    .setCallback(callback)
                    .convert();
            if (isStopepd)
                handler.post(new Runnable() {
                    @Override
                    public void run() {

//                        console.setText(console.getText() + "\nFile saved: " + file1.getAbsoluteFile() +
//                                " At: " + timeText());
                    }
                });


            Log.e("path", currentFile.toString());


            if (isStarted) {
                //                console.setText(console.getText() + "\nRecording ended: " + timeText());
//                console.setText(console.getText() + "\n Listening started: " + timeText());
                startSeconds = 0;
                seconds = 0;
                currentFile = new File(getExternalFilesDir(null),
                        "recording" + System.currentTimeMillis() + ".wav");

                recorder = OmRecorder.wav(
                        new PullTransport.Default(Util.getMic(AudioSource.MIC, AudioChannel.STEREO, AudioSampleRate.HZ_48000), MainActivity_Recorder.this),
                        currentFile);
                recorder.startRecording();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {


        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }


    @Override
    protected void onResume() {

        super.onResume();


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onAudioChunkPulled(final AudioChunk audioChunk) {
        visualizerView.addAmplitude((float) Math.pow(10,audioChunk.maxAmplitude()/20)); // update the VisualizeView
        visualizerView.invalidate(); // refresh the VisualizerView

    }

    public void foldercreating() {

        String dirPathParent = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TapTap";
        File dirParent = new File(dirPathParent);
        if (dirParent.mkdirs()) {
        } else {
        }
        if (!dirParent.exists()) {
            dirParent.mkdirs();
        }

        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TapTap/" + "mytaptap";
        Log.e("path", dirPath);
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }


    }



    private void audioSetup() {
        tempFile = new File(getExternalFilesDir(null),
                "recording" + System.currentTimeMillis() + ".wav");
        tempRecorder = new MediaRecorder();
        tempRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        tempRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_2_TS);
        tempRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        tempRecorder.setOutputFile(tempFile.getPath());
    }
    @Override
    public void onBackPressed() {
        Intent i = new Intent(MainActivity_Recorder.this, MainActivity_Recorder.class);
        startActivity(i);
        Toast.makeText(this, "hellow", Toast.LENGTH_SHORT).show();
        finish();
    }

}
