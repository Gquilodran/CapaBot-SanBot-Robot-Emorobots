package com.sanbot.capaBot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.sanbot.opensdk.base.TopBaseActivity;

/**
 * Activity desactivada temporalmente para la prueba de concepto.
 * Redirige automáticamente de vuelta a MyBaseActivity.
 */
public class MyDialogActivity extends TopBaseActivity {

    private final static String TAG = "IGOR-DIAL";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        register(MyDialogActivity.class);
        super.onCreate(savedInstanceState);

        Log.i(TAG, "MyDialogActivity ha sido invocada pero esta desactivada. Redirigiendo a MyBaseActivity...");

        // Redirigir inmediatamente a MyBaseActivity para no mostrar menus ni escuchar voz
        Intent myIntent = new Intent(MyDialogActivity.this, MyBaseActivity.class);
        startActivity(myIntent);

        // Cerrar esta Activity al instante
        finish();
    }

    @Override
    protected void onMainServiceConnected() {

    }
}