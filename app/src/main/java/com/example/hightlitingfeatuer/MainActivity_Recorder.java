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


import com.example.hightlitingfeatuer.AndroidAudioConverter;
import com.example.hightlitingfeatuer.R;
import com.example.hightlitingfeatuer.Util;
import com.example.hightlitingfeatuer.UtilSave;
import com.example.hightlitingfeatuer.callback.IConvertCallback;
import com.tyorikan.voicerecordingvisualizer.RecordingSampler;
import com.tyorikan.voicerecordingvisualizer.VisualizerView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;
import dmax.dialog.SpotsDialog;
import omrecorder.AudioChunk;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.Recorder;

public class MainActivity_Recorder extends AppCompatActivity implements PullTransport.OnAudioChunkPulledListener, RecordingSampler.CalculateVolumeListener {
    String currentDateTime;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean isStarted = false;
    private Recorder recorder;
    private boolean shouldDeleteLastFile = false;
    private TextView console;
    private Handler handler;
    int seconds;
    int startSeconds;
    int endSeconds;
    //Modify seconds you want to record before start recording
    int SECOONDSTORECORDBEFORE = 10;
    ImageView audiobar;
    View someView;
    RecordingSampler recordingSampler;
    VisualizerView visualizerView;
    int flag = 0;
    SpotsDialog dialog;

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

        recordingSampler = new RecordingSampler();
        recordingSampler.setVolumeListener(MainActivity_Recorder.this);  // for custom implements
        recordingSampler.setSamplingInterval(100); // voice sampling interval
        recordingSampler.link(visualizerView);     // link to visualizer
//        recordingSampler.startRecording();


        //
//         mRealtimeWaveformView = findViewById(R.id.waveformView);
//         mRecordingThread = new RecordingThread(new AudioDataReceivedListener() {
//             @Override
//             public void onAudioDataReceived(short[] data) {
////
//                 try {
//                     mRealtimeWaveformView.setSamples(data);
//                 } catch (Exception ee) {
//
//                 }
//             }
//         });
//
//
//         mRecordingThread.startRecording();

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


    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, 8000); // sample rate
            writeInt(output, RECORDER_SAMPLERATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis = new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }


    ArrayList<Integer> readedBytes = new ArrayList<>();
    File currentFile;
    boolean isBytesCleaning;

    private void cutTo10Seconds() {
        isBytesCleaning = true;

        if (readedBytes.size() - 1 - RECORDER_SAMPLERATE * 10 > 0) {
            readedBytes = new ArrayList<>(readedBytes
                    .subList(readedBytes.size() - 1 - RECORDER_SAMPLERATE * 10, readedBytes.size() - 1));
        }
        isBytesCleaning = false;
    }


    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            isStarted = false;
            recorder.stopRecording();
            recorder = null;
            recordingThread = null;

        }

//        mediaRecorder.stop();
//        mediaRecorder.release();
//        mediaRecorder = null;
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


//                                console.setText(console.getText() + "\n Listening started: " + timeText());
//                                ((Button) v).setText("Start Recording");
                            }
                        });
                    } else {
                        if (isStarted) {
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected void onPreExecute() {
                                    dialog = new SpotsDialog(MainActivity_Recorder.this);
                                    dialog.show();
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
                                    dialog.dismiss();

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
//                    Thread savingThread = new Thread(new Runnable() {
//                        public void run() {
//                            saveToFile();
//                        }
//                    });
//                    savingThread.start();
//
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
                        dialog = new SpotsDialog(MainActivity_Recorder.this);
                        dialog.show();
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
                        dialog.dismiss();


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

            if (shouldDeleteLastFile == false) {
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

    private String timeText() {
        return new SimpleDateFormat("hh:mm:ss").format(System.currentTimeMillis());
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
    public void onAudioChunkPulled(AudioChunk audioChunk) {

    }

    @Override
    public void onCalculateVolume(int volume) {

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

    @Override
    public void onBackPressed() {
        Intent i = new Intent(MainActivity_Recorder.this, MainActivity_Recorder.class);
        startActivity(i);
        Toast.makeText(this, "hellow", Toast.LENGTH_SHORT).show();
        finish();
    }

}
