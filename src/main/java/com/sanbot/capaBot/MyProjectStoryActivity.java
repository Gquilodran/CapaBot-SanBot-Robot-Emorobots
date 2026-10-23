package com.sanbot.capaBot;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.LED;
import com.sanbot.opensdk.function.beans.headmotion.LocateAbsoluteAngleHeadMotion;
import com.sanbot.opensdk.function.beans.wheelmotion.DistanceWheelMotion;
import com.sanbot.opensdk.function.beans.wing.AbsoluteAngleWingMotion;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.ModularMotionManager;
import com.sanbot.opensdk.function.unit.ProjectorManager;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;
import com.sanbot.opensdk.function.unit.WingMotionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.sanbot.capaBot.MyUtils.temporaryEmotion;

/**
 * function: projection the story of vislab
 * Refactored event-driven architecture for video playback, robot movements,
 * and interactive scale touch logging.
 */
public class MyProjectStoryActivity extends TopBaseActivity {

    private final static String TAG = "IGOR-PROJ";
    public static String ECG = "ECG-Signal";
    private static final String TAG_ESCALA = "Escala";

    private static final String MOVE_NONE = "NONE";
    private static final String MOVE_FORWARD = "FORWARD";
    private static final String MOVE_BACKWARD = "BACKWARD";
    private String lastMovementDirection = MOVE_NONE;

    @BindView(R.id.exit)
    Button exitButton;

    @BindView(R.id.myvideoview)
    VideoView videoView;

    @BindView(R.id.iv_static_image)
    ImageView ivStaticImage;

    @BindView(R.id.tv_scale_feedback)
    TextView tvScaleFeedback;

    // SDK Managers
    private ProjectorManager projectorManager;
    private SpeechManager speechManager;
    private HardWareManager hardWareManager;
    private HeadMotionManager headMotionManager;
    private WingMotionManager wingMotionManager;
    private SystemManager systemManager;
    private ModularMotionManager modularMotionManager;
    private WheelMotionManager wheelMotionManager;

    // Video sequence state
    private List<String> listaVideos = new ArrayList<>();
    private boolean isSequenceMode = false;
    private String ultimoVideoReproducido = "";
    private int ultimoValorEscala = -1;

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
        register(MyProjectStoryActivity.class);

        // Screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projector_story);
        ButterKnife.bind(this);

        // Initialize managers
        projectorManager = (ProjectorManager) getUnitManager(FuncConstant.PROJECTOR_MANAGER);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        wingMotionManager = (WingMotionManager) getUnitManager(FuncConstant.WINGMOTION_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);
        modularMotionManager = (ModularMotionManager) getUnitManager(FuncConstant.MODULARMOTION_MANAGER);
        wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER);

        // Silenciar avisos de voz del SDK desde el inicio
        silenciarVoz();

        // Handler to open projector
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "handler called to open projector");
                if (projectorManager != null) {
                    projectorManager.setMode(MySettings.getProjectorMode());
                    projectorManager.switchProjector(true);
                }
            }
        }, 500);

        initListeners();

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishThisActivity();
            }
        });

        // Intent extras evaluation
        String actionDuringVideo = (getIntent() != null && getIntent().getStringExtra("ACTION_DURING_VIDEO") != null)
                ? getIntent().getStringExtra("ACTION_DURING_VIDEO")
                : "";
        String actionType = (getIntent() != null && getIntent().getStringExtra("ACTION_TYPE") != null)
                ? getIntent().getStringExtra("ACTION_TYPE")
                : "";

        final boolean isAll = "ALL".equalsIgnoreCase(actionType) || "ALL".equalsIgnoreCase(actionDuringVideo);
        final String targetVideo = actionDuringVideo;

        // Post delayed start to allow projector to be fully ON before initiating movements
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                silenciarVoz();
                if (isAll || targetVideo.isEmpty()) {
                    isSequenceMode = true;
                    iniciarSecuenciaVideos();
                    executeAllAction();
                } else {
                    isSequenceMode = false;
                    switch (targetVideo.charAt(0)) {
                        case '0':
                            executeSadAction(targetVideo);
                            break;
                        case '1':
                            executeNeutralAction(targetVideo);
                            break;
                        case '2':
                            executeHappyAction(targetVideo);
                            break;
                        default:
                            executeNeutralAction(targetVideo);
                            break;
                    }
                }
            }
        }, 3500);
    }

    public void silenciarVoz() {
        if (speechManager != null) {
            try {
                speechManager.stopSpeak();
                speechManager.doSleep();
                Log.i(TAG, "SpeechManager silenciado exitosamente.");
            } catch (Exception e) {
                Log.e(TAG, "Error al silenciar SpeechManager: " + e.getMessage());
            }
        }
    }

    public void restaurarVoz() {
        if (speechManager != null) {
            try {
                speechManager.doWakeUp();
                Log.i(TAG, "SpeechManager restaurado exitosamente.");
            } catch (Exception e) {
                Log.e(TAG, "Error al restaurar SpeechManager: " + e.getMessage());
            }
        }
    }

    private void displayStaticImageFromAssets() {
        try {
            String[] files = getAssets().list("image");
            String imageName = "escala.jpeg";
            if (files != null && files.length > 0) {
                for (String f : files) {
                    if (f.toLowerCase().contains("escala")) {
                        imageName = f;
                        break;
                    }
                }
            }
            InputStream is = getAssets().open("image/" + imageName);
            final Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ivStaticImage != null) {
                        ivStaticImage.setImageBitmap(bitmap);
                        ivStaticImage.setVisibility(View.VISIBLE);
                    }
                    if (tvScaleFeedback != null) {
                        tvScaleFeedback.setVisibility(View.GONE);
                    }
                    if (videoView != null) {
                        videoView.setVisibility(View.GONE);
                    }
                }
            });
            Log.i(TAG, "Imagen cargada exitosamente desde assets/image/" + imageName);
        } catch (IOException e) {
            Log.e(TAG, "Error al cargar la imagen desde assets: " + e.getMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ivStaticImage != null) {
                        ivStaticImage.setVisibility(View.VISIBLE);
                    }
                    if (videoView != null) {
                        videoView.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    public void initListeners() {
        // Listener interactivo para registrar la pulsación en la escala
        if (ivStaticImage != null) {
            ivStaticImage.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        v.performClick();
                        handleScaleTouch(event.getX(), event.getY());
                        return true;
                    }
                    return false;
                }
            });
        }

        // Listener de finalización de video
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.i(TAG, "Video finalizado (" + ultimoVideoReproducido + ") - Callback de fin de reproducción recibido");

                if (isSequenceMode) {
                    // 1. Mostrar la escala desde assets/image/ durante el delay de 5 segundos
                    displayStaticImageFromAssets();

                    // 2. Durante los 5 segundos de espera, el robot inicia movimiento hacia postura neutra
                    executeNeutralAction(null);

                    // 3. Al cumplirse los 5 segundos de delay, registrar y avanzar al siguiente video
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (ultimoValorEscala != -1) {
                                Log.i(TAG_ESCALA, "Respuesta final registrada para video " + ultimoVideoReproducido + ": " + ultimoValorEscala);
                            } else {
                                Log.i(TAG_ESCALA, "Sin respuesta registrada del usuario para video " + ultimoVideoReproducido);
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (ivStaticImage != null) {
                                        ivStaticImage.setVisibility(View.GONE);
                                    }
                                    if (tvScaleFeedback != null) {
                                        tvScaleFeedback.setVisibility(View.GONE);
                                    }
                                    if (videoView != null) {
                                        videoView.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                            executeAllAction();
                        }
                    }, 5000);
                } else {
                    Log.i(TAG, "Secuencia completada o video único finalizado. Cerrando proyector.");
                    if (projectorManager != null) {
                        projectorManager.switchProjector(false);
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finishThisActivity();
                        }
                    }, 1500);
                }
            }
        });
    }

    /**
     * Mapea las coordenadas táctiles sobre la escala en pantalla (1 a 9) y emite los Logs al PC.
     */
    private void handleScaleTouch(float touchX, float touchY) {
        if (ivStaticImage == null) return;

        Drawable drawable = ivStaticImage.getDrawable();
        if (drawable == null) return;

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            drawableWidth = 1280;
            drawableHeight = 720;
        }

        Matrix inverse = new Matrix();
        ivStaticImage.getImageMatrix().invert(inverse);
        float[] pts = new float[]{touchX, touchY};
        inverse.mapPoints(pts);

        float bitmapX = pts[0];
        float bitmapY = pts[1];

        Log.d(TAG, "Touch coordenadas: view(" + touchX + ", " + touchY + ") -> bmp(" + bitmapX + ", " + bitmapY + ")");

        // Bounding box horizontal de la escala en escala.jpeg (referencia 1280x720):
        // La casilla 1 inicia en x=111, y la casilla 9 finaliza en x=1166 (ancho total 1055 para 9 casillas)
        float scaleLeft = 111f * ((float) drawableWidth / 1280f);
        float scaleRight = 1166f * ((float) drawableWidth / 1280f);
        float boxWidth = (scaleRight - scaleLeft) / 9f;

        // Descartar si el toque ocurre en el margen superior cercano al botón de salir
        float topMargin = 80f * ((float) drawableHeight / 720f);
        if (bitmapY < topMargin) {
            return;
        }

        int option;
        if (bitmapX < scaleLeft) {
            option = 1;
        } else if (bitmapX >= scaleRight) {
            option = 9;
        } else {
            option = (int) ((bitmapX - scaleLeft) / boxWidth) + 1;
        }

        if (option < 1) option = 1;
        if (option > 9) option = 9;

        registrarSeleccionEscala(option);
    }

    /**
     * Registra la opción elegida por el usuario, emitiendo Logs con TAG "Escala" y mostrando feedback en pantalla.
     */
    private void registrarSeleccionEscala(final int option) {
        ultimoValorEscala = option;

        Log.i(TAG_ESCALA, "Opcion seleccionada: " + option);
        Log.i(TAG_ESCALA, "VALENCIA: " + option + " | Video previo: " + (ultimoVideoReproducido.isEmpty() ? "N/A" : ultimoVideoReproducido));
        Log.i(ECG, "ESCALA VALENCIA: " + option + " | Video previo: " + (ultimoVideoReproducido.isEmpty() ? "N/A" : ultimoVideoReproducido));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvScaleFeedback != null) {
                    tvScaleFeedback.setText("Opción seleccionada: " + option);
                    tvScaleFeedback.setVisibility(View.VISIBLE);
                }
                Toast.makeText(MyProjectStoryActivity.this, "Opción " + option + " registrada", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void playVideo(final String nombreVideo) {
        if (nombreVideo == null || nombreVideo.isEmpty() || "LOL".equals(nombreVideo)) {
            Log.i(TAG, "Nombre de video no valido: " + nombreVideo);
            return;
        }

        ultimoVideoReproducido = nombreVideo;
        ultimoValorEscala = -1;

        String carpeta_elegida = "clase 0";
        if (nombreVideo.startsWith("0")) {
            carpeta_elegida = "clase 0";
        } else if (nombreVideo.startsWith("1")) {
            carpeta_elegida = "clase I";
        } else if (nombreVideo.startsWith("2")) {
            carpeta_elegida = "clase II";
        }

        String rutaAssets = "video/" + carpeta_elegida + "/" + nombreVideo;
        final File videoFile = new File(Environment.getExternalStorageDirectory(), "CAPABOT/" + carpeta_elegida + "/" + nombreVideo);
        Log.i(TAG, "Buscando archivo en: " + videoFile.getAbsolutePath());

        if (!videoFile.exists()) {
            copyAssetToStorage(rutaAssets, videoFile);
        }

        if (ivStaticImage != null) {
            ivStaticImage.setVisibility(View.GONE);
        }
        if (tvScaleFeedback != null) {
            tvScaleFeedback.setVisibility(View.GONE);
        }
        if (videoView != null) {
            videoView.setVisibility(View.VISIBLE);
        }

        if (videoFile.exists()) {
            videoView.setVideoPath(videoFile.getAbsolutePath());
            videoView.requestFocus();
            videoView.start();
            Log.i(TAG, "EVENTO: Reproduciendo video " + nombreVideo + " | Pertenece a: " + carpeta_elegida);

            // Apagar LEDs 8 segundos después
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (hardWareManager != null) {
                        Log.i(TAG, "Apagando LEDs durante reproduccion");
                        LED closeLed = new LED(LED.PART_ALL, LED.MODE_CLOSE);
                        hardWareManager.setLED(closeLed);
                    }
                }
            }, 8000);
        } else {
            Log.e(TAG, "ERROR: No se encontró el video en " + videoFile.getAbsolutePath());
        }
    }

    public void iniciarSecuenciaVideos() {
        listaVideos.clear();
        String[] carpetas = {"0", "1", "2"};

        for (String carpeta : carpetas) {
            for (int i = 0; i < 16; i++) {
                char letra = (char) ('a' + i);
                listaVideos.add(carpeta + letra + ".mp4");
            }
        }
        Collections.shuffle(listaVideos);
        Log.i(ECG, "Secuencia de 48 videos iniciada y mezclada. Total: " + listaVideos.size());
    }

    public String eliminaPrimerVideoVector() {
        if (listaVideos == null || listaVideos.isEmpty()) {
            Log.d(TAG, "Se han reproducido todos los videos.");
            return "LOL";
        }
        String videoActual = listaVideos.remove(0);
        Log.d(ECG, "Se pasa al siguiente Video: " + videoActual);
        return videoActual;
    }

    public void executeAllAction() {
        isSequenceMode = true;
        silenciarVoz();
        String elegido = eliminaPrimerVideoVector();
        if ("LOL".equals(elegido) || elegido == null) {
            Log.i(ECG, "Se han reproducido todos los videos.");
            Log.i(TAG_ESCALA, "Secuencia de videos completada.");
            if (projectorManager != null) {
                projectorManager.switchProjector(false);
            }
            restaurarVoz();
            finishThisActivity();
            return;
        }

        String nombreClase = elegido.startsWith("0") ? "Clase 0" : elegido.startsWith("1") ? "Clase I" : elegido.startsWith("2") ? "Clase II" : "Desconocida";
        Log.i(ECG, "Ejecutando acción " + nombreClase + " video: " + elegido + " | Restantes: " + listaVideos.size());

        switch (elegido.charAt(0)) {
            case '0':
                executeSadAction(elegido);
                break;
            case '1':
                executeNeutralAction(elegido);
                break;
            case '2':
                executeHappyAction(elegido);
                break;
            default:
                executeNeutralAction(elegido);
                break;
        }
    }

    public void wanderOffNow() {
        if (modularMotionManager != null) {
            modularMotionManager.switchWander(false);
            Log.i(TAG, "Wander forced off now");
        }
    }

    public void executeSadAction(final String nombreVideo) {
        Log.i(ECG, "Ejecutando accion triste secuencial");
        silenciarVoz();
        wanderOffNow();
        lastMovementDirection = MOVE_BACKWARD;

        if (systemManager != null) {
            temporaryEmotion(systemManager, EmotionsType.SLEEP, 10);
        }
        if (hardWareManager != null) {
            LED led = new LED(LED.PART_ALL, LED.MODE_RED, (byte) 255);
            hardWareManager.setLED(led);
        }
        if (wingMotionManager != null) {
            AbsoluteAngleWingMotion absoluteAngleWingMotion = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_BOTH, 8, 270);
            wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion);
        }
        if (wheelMotionManager != null) {
            DistanceWheelMotion motion = new DistanceWheelMotion(DistanceWheelMotion.ACTION_BACK_RUN, 3, 20);
            wheelMotionManager.doDistanceMotion(motion);
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (nombreVideo != null && !nombreVideo.isEmpty()) {
                    playVideo(nombreVideo);
                }
            }
        }, 2500);
    }

    public void executeHappyAction(final String nombreVideo) {
        Log.i(ECG, "Ejecutando accion feliz secuencial");
        silenciarVoz();
        wanderOffNow();
        lastMovementDirection = MOVE_FORWARD;

        if (systemManager != null) {
            temporaryEmotion(systemManager, EmotionsType.SMILE, 10);
        }
        if (hardWareManager != null) {
            LED led = new LED(LED.PART_ALL, LED.MODE_YELLOW, (byte) 255);
            hardWareManager.setLED(led);
        }
        if (wingMotionManager != null) {
            AbsoluteAngleWingMotion absoluteAngleWingMotion = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_BOTH, 8, 90);
            wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion);
        }
        if (wheelMotionManager != null) {
            DistanceWheelMotion motion = new DistanceWheelMotion(DistanceWheelMotion.ACTION_FORWARD_RUN, 3, 20);
            wheelMotionManager.doDistanceMotion(motion);
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (nombreVideo != null && !nombreVideo.isEmpty()) {
                    playVideo(nombreVideo);
                }
            }
        }, 2500);
    }

    public void executeNeutralAction(final String nombreVideo) {
        Log.i(ECG, "Ejecutando accion neutra");
        silenciarVoz();
        wanderOffNow();

        if (MOVE_FORWARD.equals(lastMovementDirection)) {
            Log.i(TAG, "Regresando desde adelante a postura neutra");
            if (wheelMotionManager != null) {
                DistanceWheelMotion motion = new DistanceWheelMotion(DistanceWheelMotion.ACTION_BACK_RUN, 3, 20);
                wheelMotionManager.doDistanceMotion(motion);
            }
        } else if (MOVE_BACKWARD.equals(lastMovementDirection)) {
            Log.i(TAG, "Regresando desde atrás a postura neutra");
            if (wheelMotionManager != null) {
                DistanceWheelMotion motion = new DistanceWheelMotion(DistanceWheelMotion.ACTION_FORWARD_RUN, 3, 20);
                wheelMotionManager.doDistanceMotion(motion);
            }
        }
        lastMovementDirection = MOVE_NONE;

        if (systemManager != null) {
            temporaryEmotion(systemManager, EmotionsType.NORMAL);
        }
        if (hardWareManager != null) {
            LED led = new LED(LED.PART_ALL, LED.MODE_BLUE, (byte) 255);
            hardWareManager.setLED(led);
        }
        if (wingMotionManager != null) {
            AbsoluteAngleWingMotion wingNeutral = new AbsoluteAngleWingMotion(AbsoluteAngleWingMotion.PART_BOTH, 8, 180);
            wingMotionManager.doAbsoluteAngleMotion(wingNeutral);
        }
        // Corrección de postura de cabeza recta al centro (horizontal 90, vertical 30)
        if (headMotionManager != null) {
            LocateAbsoluteAngleHeadMotion headNeutral = new LocateAbsoluteAngleHeadMotion(
                    LocateAbsoluteAngleHeadMotion.ACTION_VERTICAL_LOCK, 90, 30
            );
            headMotionManager.doAbsoluteLocateMotion(headNeutral);
        }

        if (nombreVideo != null && !nombreVideo.isEmpty()) {
            playVideo(nombreVideo);
        }
    }

    @Override
    protected void onMainServiceConnected() {

    }

    private void finishThisActivity() {
        restaurarVoz();
        Intent myIntent = new Intent(MyProjectStoryActivity.this, MyDialogActivity.class);
        MyProjectStoryActivity.this.startActivity(myIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
