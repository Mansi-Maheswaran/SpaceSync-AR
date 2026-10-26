package com.truefitar.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.examples.java.helloar.HelloArActivity;
import com.google.ar.core.examples.java.helloar.R;
import com.truefitar.storage.RoomStore;

import android.widget.TextView;

/** Home screen: New Room Scan or Saved Rooms. */
public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        findViewById(R.id.newScanCard).setOnClickListener((View v) ->
                startActivity(new Intent(this, HelloArActivity.class)));

        findViewById(R.id.savedRoomsCard).setOnClickListener((View v) ->
                startActivity(new Intent(this, SavedRoomsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        int count = RoomStore.loadAll(this).size();
        TextView sub = findViewById(R.id.savedRoomsSubtitle);
        sub.setText(count == 0
                ? "No saved rooms yet"
                : count + (count == 1 ? " saved room" : " saved rooms") + " - tap to open");
    }
}
