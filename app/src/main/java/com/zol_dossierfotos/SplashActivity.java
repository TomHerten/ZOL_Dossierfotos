// Package name.
package com.zol_dossierfotos;

// Imports from official sources.
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

// Activity for our SplashScreen.
public class SplashActivity extends AppCompatActivity {

    // Handles starting of the activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // As soon as this activity is finished create and launch the loginActivity.
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);

        this.finish();

    }

}
