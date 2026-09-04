package com.sanbot.capaBot;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.beans.LED;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.unit.ProjectorManager;
import com.sanbot.opensdk.function.unit.SpeechManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.sanbot.capaBot.MyUtils.concludeSpeak;

/**
 * function: projection the story of vislab
 */

public class MyProjectStoryActivity extends TopBaseActivity {

    private final static String TAG = "IGOR-PROJ";

    @BindView(R.id.exit)
    Button exitButton;

    //managers
    private ProjectorManager projectorManager;
    private SpeechManager speechManager; //voice, speechRec
    private HardWareManager hardWareManager;
    //video view for fullscreen
    VideoView videoView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Se crea un array de categorias
        List<String> carpetas = Arrays.asList("clase 0", "clase I", "clase II");
        List<String> letras = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p");
        register(MyProjectStoryActivity.class);
        //screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projector_story);
        ButterKnife.bind(this);
        //init manager
        projectorManager = (ProjectorManager) getUnitManager(FuncConstant.PROJECTOR_MANAGER);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        //other settings
        /*
        projectorManager.setTrapezoidH(0);
        projectorManager.setTrapezoidV(0);
        projectorManager.setAcuity(0);
        projectorManager.setSaturation(0);
        projectorManager.setColor(0);
        projectorManager.setBright(0);
        projectorManager.setContrast(0);
        projectorManager.setMirror(ProjectorManager.MIRROR_CLOSE);*/

        //handler to open projector
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "handler called to open projector");
                //mode from settings
                projectorManager.setMode(MySettings.getProjectorMode());
                //OPEN PROJECTOR
                projectorManager.switchProjector(true);
                //voice introduction
//                speechManager.startSpeak(getString(R.string.show_video), MySettings.getSpeakDefaultOption());
            }
        }, 500);

        //reproducir video aleaoorio
        videoView = findViewById(R.id.myvideoview);
        String actionDuringVideo = Intent.getIntent().getStringExtra("ACTION_DURING_VIDEO");
        String carpeta_elegida = "";
        if(actionDuringVideo == "sad"){
            carpeta_elegida= String.valueOf(0);
        }
        if(actionDuringVideo == "happy"){
            carpeta_elegida= String.valueOf(1);
        }
        if(actionDuringVideo == "neutral"){
            carpeta_elegida= String.valueOf(2);
        }
        int vid = new Random().nextInt(letras.size());
        //String carpetaElegida = carpetas.get(index);
        //String numero = String.valueOf(index);
        String letra = letras.get(vid);
        String nombreVideo = carpeta_elegida + letra + ".mp4"; // numero 0,1 o 2 dependiendo de la carpeta y tipo de video + letra identificacion de cada video + .mp4

        String rutaAssets = "video/" + carpeta_elegida + "/" + nombreVideo;
        String rutaDestinoMemoria = Environment.getExternalStorageDirectory().getPath() + "/CAPABOT/" + carpeta_elegida + "_" + nombreVideo;

        File videoFile = new File(rutaDestinoMemoria);
        if (!videoFile.exists()) {
            copyAssetToStorage(rutaAssets, videoFile);
        }
        videoView.setVideoURI(Uri.parse(rutaDestinoMemoria));
        videoView.setMediaController(new MediaController(this));
        videoView.requestFocus();
        videoView.start();
        videoView.pause();

        Log.i(TAG, "Video Ready, waiting the projector to be ON");

        //handler to start video when the projector is effectively started
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "start video called");
                videoView.start();

                // Apagar LEDs 8 segundos después
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Apagando LEDs");

                        LED closeLed = new LED(LED.PART_ALL, LED.MODE_CLOSE);
                        hardWareManager.setLED(closeLed);
                    }
                }, 8000);
            }
        }, 2000);

        initListeners();

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishThisActivity();
            }
        });
    }

    public void onTesis(Bundle savedInstanceState) {
        //Se crea un array de categorias
        List<String> carpetas = Arrays.asList("clase 0", "clase I", "clase II");
        List<String> letras = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p");
        register(MyProjectStoryActivity.class);
        //screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projector_story);
        ButterKnife.bind(this);
        //init manager
        projectorManager = (ProjectorManager) getUnitManager(FuncConstant.PROJECTOR_MANAGER);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        //other settings
        /*
        projectorManager.setTrapezoidH(0);
        projectorManager.setTrapezoidV(0);
        projectorManager.setAcuity(0);
        projectorManager.setSaturation(0);
        projectorManager.setColor(0);
        projectorManager.setBright(0);
        projectorManager.setContrast(0);
        projectorManager.setMirror(ProjectorManager.MIRROR_CLOSE);*/

        //handler to open projector
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "handler called to open projector");
                //mode from settings
                projectorManager.setMode(MySettings.getProjectorMode());
                //OPEN PROJECTOR
                projectorManager.switchProjector(true);
                //voice introduction
//                speechManager.startSpeak(getString(R.string.show_video), MySettings.getSpeakDefaultOption());
            }
        }, 500);

        //reproducir video aleaoorio
        videoView = findViewById(R.id.myvideoview);
        String actionDuringVideo = Intent.getIntent().getStringExtra("ACTION_DURING_VIDEO");
        int vid = new Random().nextInt(letras.size());
        String carpetaElegida = carpetas.get(index);
        String numero = String.valueOf(index);
        String letra = letras.get(vid);
        String nombreVideo = carpeta_elegida + letra + ".mp4"; // numero 0,1 o 2 dependiendo de la carpeta y tipo de video + letra identificacion de cada video + .mp4

        String rutaAssets = "video/" + carpeta_elegida + "/" + nombreVideo;
        String rutaDestinoMemoria = Environment.getExternalStorageDirectory().getPath() + "/CAPABOT/" + carpeta_elegida + "_" + nombreVideo;

        File videoFile = new File(rutaDestinoMemoria);
        if (!videoFile.exists()) {
            copyAssetToStorage(rutaAssets, videoFile);
        }
        videoView.setVideoURI(Uri.parse(rutaDestinoMemoria));
        videoView.setMediaController(new MediaController(this));
        videoView.requestFocus();
        videoView.start();
        videoView.pause();

        Log.i(TAG, "Video Ready, waiting the projector to be ON");

        //handler to start video when the projector is effectively started
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "start video called");
                videoView.start();

                // Apagar LEDs 8 segundos después
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Apagando LEDs");

                        LED closeLed = new LED(LED.PART_ALL, LED.MODE_CLOSE);
                        hardWareManager.setLED(closeLed);
                    }
                }, 8000);
            }
        }, 2000);

        initListeners();

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishThisActivity();
            }
        });
    }

    public void initListeners() {
        // Al finalizar el video
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.i(TAG, "Video finalizado - Cerrando proyector de forma segura");

                // 1. Apagar el proyector inmediatamente
                if (projectorManager != null) {
                    projectorManager.switchProjector(false);
                }
                // 3. Esperar 1.5 segundos a que el robot procese la voz y libere el proyector antes de cerrar
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finishThisActivity(); // Cierra la pantalla de forma segura hacia MyBaseActivity
                    }
                }, 5000);
            }
        });
    }


    @Override
    protected void onMainServiceConnected() {

    }

    private void finishThisActivity() {
        //starts dialog activity
        Intent myIntent = new Intent(MyProjectStoryActivity.this, MyDialogActivity.class);
        MyProjectStoryActivity.this.startActivity(myIntent);

        //calls finish activity
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Android documentation says this:
        //"do not count on this method being called"
        //so I can't use this for operation: "ending activity" in the code,
        //better call finishThisActivity()
    }

    private void copyRawResourceToStorage(int resId, File outFile) {
        try {
            File dir = outFile.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }

            InputStream in = getResources().openRawResource(resId);
            OutputStream out = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
