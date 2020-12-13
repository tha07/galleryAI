    package com.example.gallery_ai;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.work.WorkManager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import static com.example.gallery_ai.UserLogin.userID;
import static com.example.gallery_ai.UserLogin.userEmail;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.example.gallery_ai.GalleryGrid.image_labels;
import static com.example.gallery_ai.GalleryGrid.userUrls;
import static com.example.gallery_ai.GalleryGrid.userLabels;



public class MainActivity extends AppCompatActivity {

    private Button selectPhotoButton;
    private String stringClassification, currentPhotoPath;
    public static ImageView imageClassifier;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CODE_CAMERA = 1;
    public static final int PICK_IMAGE = 2;
    public static Uri photoURI;
    private WorkManager mWorkManager;
    StorageReference ref;
    public String[] myarray;

    public static Map<String, Object> image_labelsMain = new HashMap<>();
    public static Map<String, Object> dummyHash = new HashMap<>();


    public static List<String> userUrlsMain = new ArrayList<>();
    public static List<String> userLabelsMain = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //init_elements();
        dummyHash.put("ARE u","CRAZY");

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference collRef;

        try{
             db.collection(userID);

        }catch (Exception e){
            db.collection(userID).document("thisEMptyDOc").set(dummyHash);
        }

        collRef = db.collection(userID);


        collRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        //List<String> keys = new ArrayList<>(document.getData().keySet());
                        userLabels.add(document.getId());
                        userUrls.add(document.getData().get("url").toString());
                    }
                        try{
                            userLabels.add(userLabelsMain.get(userLabelsMain.size()-1));
                            userUrls.add(userUrlsMain.get(userUrlsMain.size()-1));
                            image_labelsMain.put("url",userUrlsMain.get(0));

                            uploadToFirestore(db, userLabelsMain.get(0));

                            userUrlsMain.clear();
                            userLabelsMain.clear();



                        }
                        catch(Exception e){}
                        startActivity(new Intent(MainActivity.this,GalleryGrid.class));
                        finish();


                    } else {
                        Log.d("TAG", "No such document");
                    }
                }
            }
        );

        ref = FirebaseStorage.getInstance().getReference().child("/dogs");


    }
    public void uploadToFirestore(FirebaseFirestore db , String docName){
        db.collection(userID).document(docName).set(image_labelsMain);
    }

}

