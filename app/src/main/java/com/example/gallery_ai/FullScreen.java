package com.example.gallery_ai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import static com.example.gallery_ai.UserLogin.userID;

public class FullScreen extends AppCompatActivity {
    private ImageView mImage;
    private TextView mText;
    private Button deleteButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        final String label = intent.getStringExtra("tolabel"); // lebel, uri, και timestamp εικόνας απο το Activity του GalleryGrid
        final String uri = intent.getStringExtra("tourl");
        final String timestamp = intent.getStringExtra("totimestamp");


        hideStatusBar(); // Απόκρυψη StatusBar
        initElements(); // Αρχικοποίηση όλων των στοιχείων
        setData(label, uri); // Εισαγωγή εικόνας και Label στο ImageView
        setListeners(label, uri, timestamp); // Listener για το delete button
    }

    private void deletePhoto(String collection, String document, String imageUri, String thetimeStamp){ // Διαγραφή εικόνων με το κομπί delete
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageReference = firebaseStorage.getReferenceFromUrl(imageUri);


        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.e("Picture","#deleted");
            }
        });


        db.collection(collection).document(thetimeStamp)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() { // Διαδικασία διαγραφής
                    @Override
                    public void onSuccess(Void aVoid) {
                        startActivity(new Intent(FullScreen.this, GalleryGrid.class));
                        Log.d("TAG", "DocumentSnapshot successfully deleted!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG", "Error deleting document", e);
                    }
                });


    }

    private void setListeners(final String theLabel, final String theURi, final String whatTimeStamp){



        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deletePhoto(userID, theLabel, theURi, whatTimeStamp);
            }

        });
    }

    private void setData(String theLabel, String theURi){
        mText.setText(theLabel);
        Picasso.with(FullScreen.this).load(theURi).into(mImage);
    }

    private void initElements(){
        mImage = findViewById(R.id.fullScreen);
        mText = findViewById(R.id.thatLabel);
        deleteButton = findViewById(R.id.deleteButton);
    }

    private void hideStatusBar(){
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_full_screen);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }
}