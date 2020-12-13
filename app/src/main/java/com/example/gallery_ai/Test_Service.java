package com.example.gallery_ai;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.example.gallery_ai.GalleryGrid.userLabels;
import static com.example.gallery_ai.GalleryGrid.userUrls;
import static com.example.gallery_ai.MainActivity.image_labelsMain;
import static com.example.gallery_ai.MainActivity.userLabelsMain;
import static com.example.gallery_ai.MainActivity.userUrlsMain;

public class Test_Service extends IntentService {

    public Test_Service() {
        super("Test_Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Background work in a worker thread (async)

        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference collRef = db.collection("users");
        collRef.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override public void onComplete(@NonNull Task<QuerySnapshot> task) {
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
                                                   //finish();

                                               } else {
                            Log.d("TAG", "No such document"); } }}
                );
    }

         // retrieve the data using keyName

        // You could also send a broadcast if you need to get notified
        /*Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("whatever");
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);*/

    public void uploadToFirestore(FirebaseFirestore db , String docName){
        db.collection("users").document(docName).set(image_labelsMain);
    }
}