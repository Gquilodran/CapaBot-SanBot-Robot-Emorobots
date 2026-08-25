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
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
public class MyVideoRancomActivity {
}

/**
 *         //reproducir video aleaoorio
 *         videoView = findViewById(R.id.myvideoview);
 *
 *         int index = new Random().nextInt(carpetas.size());
 *         int vid = new Random().nextInt(letras.size());
 *         String carpetaElegida = carpetas.get(index);
 *         String numero = String.valueOf(index);
 *         String letra = letras.get(vid);
 *         String nombreVideo = numero + letra + ".mp4"; // numero 0,1 o 2 dependiendo de la carpeta y tipo de video + letra identificacion de cada video + .mp4
 *
 *         String rutaAssets = "video/" + carpetaElegida + "/" + nombreVideo;
 *         String rutaDestinoMemoria = Environment.getExternalStorageDirectory().getPath() + "/CAPABOT/" + carpetaElegida + "_" + nombreVideo;
 *
 *         File videoFile = new File(rutaDestinoMemoria);
 *         if (!videoFile.exists()) {
 *             copyAssetToStorage(rutaAssets, videoFile);
 *         }
 */