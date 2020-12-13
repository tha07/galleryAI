package com.example.gallery_ai;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class FullScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_full_screen);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Intent intent = getIntent();
        String uri = intent.getStringExtra("tourl");
        String uri2 = intent.getStringExtra("tolabel");
        ImageView mImage = findViewById(R.id.fullScreen);
        TextView mText =findViewById(R.id.thatLabel);
        mText.setText(uri2);

        Picasso.with(FullScreen.this).load(uri).into(mImage);




    }
}