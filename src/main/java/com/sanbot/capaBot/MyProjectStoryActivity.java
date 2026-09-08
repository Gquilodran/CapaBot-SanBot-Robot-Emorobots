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

    private void copyAssetToStorage(String assetPath, File destinationFile) {
        try {
            File parentDir = destinationFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            InputStream in = getAssets().open(assetPath);
            OutputStream out = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            in.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "Error al copiar asset a almacenamiento: " + e.getMessage());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        List<String> carpetas = Arrays.asList("clase 0", "clase I", "clase II");
        List<String> letras = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p");

        register(MyProjectStoryActivity.class);

        // Screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projector_story);
        ButterKnife.bind(this);

        // Init managers
        projectorManager = (ProjectorManager) getUnitManager(FuncConstant.PROJECTOR_MANAGER);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);

        // Handler to open projector
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "handler called to open projector");
                projectorManager.setMode(MySettings.getProjectorMode());
                projectorManager.switchProjector(true);
            }
        }, 500);

        // Reproducir video aleatorio
        videoView = findViewById(R.id.myvideoview);

        // Obtener la emoción desde el Intent con valor por defecto
        String actionDuringVideo = getIntent() != null ? getIntent().getStringExtra("ACTION_DURING_VIDEO") : "";
        String carpeta_elegida = "0"; // Valor por defecto si no coincide

        if ("sad".equals(actionDuringVideo)) {
            carpeta_elegida = "clase 0";
        } else if ("neutral".equals(actionDuringVideo)) {
            carpeta_elegida = "clase I";
        } else if ("happy".equals(actionDuringVideo)) {
            carpeta_elegida = "clase II";
        }

        // Seleccionar letra aleatoria y armar el nombre del archivo
        int vid = new Random().nextInt(letras.size());
        String letra = letras.get(vid);
        String Celegida = "";
        if(carpeta_elegida == "clase 0"){
            Celegida = "0";
        }
        else if(carpeta_elegida == "clase I"){
            Celegida = "1";
        }
        else if(carpeta_elegida == "clase II"){
            Celegida = "2";
        }
        String nombreVideo = Celegida + letra + ".mp4"; // Ejemplo: 0a.mp4

        // Ruta hacia la subcarpeta dentro de /sdcard/CAPABOT/
        String rutaAssets = "video/" + carpeta_elegida + "/" + nombreVideo;
        // Cambia esta línea:
        // File videoFile = new File(Environment.getExternalStorageDirectory(), "CAPABOT/" + carpeta_elegida + "/" + nombreVideo);

        // Por esta (agregando el modificador final):
        final File videoFile = new File(Environment.getExternalStorageDirectory(), "CAPABOT/" + carpeta_elegida + "/" + nombreVideo);
        Log.i(TAG, "Buscando archivo en: " + videoFile.getAbsolutePath());

        // Si no existe localmente, intentar copiar desde assets
        if (!videoFile.exists()) {
            copyAssetToStorage(rutaAssets, videoFile);
        }

        // Cargar y preparar reproductor usando la ruta absoluta directa
        if (videoFile.exists()) {
            videoView.setVideoPath(videoFile.getAbsolutePath());
            videoView.setMediaController(new MediaController(this));
            videoView.requestFocus();
            videoView.start();
            videoView.pause();
            Log.i(TAG, "Video cargado y pausado, esperando el proyector");
        } else {
            Log.e(TAG, "ERROR: No se encontró el video en " + videoFile.getAbsolutePath());
        }

        // Handler para iniciar video cuando el proyector esté listo
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "start video called");
                if (videoFile.exists()) {
                    videoView.start();
                }

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
