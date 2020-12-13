package com.example.gallery_ai;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

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
import static com.example.gallery_ai.MainActivity.userLabelsMain;
import static com.example.gallery_ai.MainActivity.userUrlsMain;
import static com.example.gallery_ai.MainActivity.image_labelsMain;

public class MyService extends Service {
    public MyService() {

        Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try  {

                final FirebaseFirestore db = FirebaseFirestore.getInstance();

                CollectionReference collRef = db.collection("users");
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

                                                                    db.collection("users").document(userLabelsMain.get(0)).set(image_labelsMain);


                                                                    userUrlsMain.clear();
                                                                    userLabelsMain.clear();



                                                                }
                                                                catch(Exception e){}
                                                                //startActivity(new Intent(MainActivity.this,GalleryGrid.class));


                                                            } else {
                                                                Log.d("TAG", "No such document");
                                                            }
                                                        }
                                                    }
                );

        }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    });
        thread.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

        throw new UnsupportedOperationException("Not yet implemented");
    }



}
