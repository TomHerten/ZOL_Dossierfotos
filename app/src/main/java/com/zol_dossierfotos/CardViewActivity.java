// Package name.
package com.zol_dossierfotos;

// Imports from official sources.
import android.app.Activity;
import android.os.Bundle;

// Activity for our cardView item.
public class CardViewActivity extends Activity {

    // Handles starting of the activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the card layout.
        setContentView(R.layout.item);

    }

}
