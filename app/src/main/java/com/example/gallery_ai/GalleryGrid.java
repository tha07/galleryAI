package com.example.gallery_ai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.example.gallery_ai.UserLogin.userID;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GalleryGrid extends AppCompatActivity {
    public String currentPhotoPath;
    public static Uri photoURI;
    private static final int REQUEST_CODE_CAMERA = 1;
    public static final int PICK_IMAGE = 2;
    public static List<String> userUrls = new ArrayList<>();
    public static List<String> userLabels = new ArrayList<>();
    public static Map<String, Object> imageData = new HashMap<>();
    public static Map<String, Object> dummyHash = new HashMap<>();

    StorageReference initialReference;
    GridView androidGridView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_grid);

        androidGridView = findViewById(R.id.gridview_android_example);
        initialReference = FirebaseStorage.getInstance().getReference();

        checkifNewUser();
        new updateImageViews().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAMERA) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                File f = new File(currentPhotoPath);
                try {
                    FileOutputStream out = new FileOutputStream(f);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 20, out); // bmp is your Bitmap instance
                    Uri contentUri = Uri.fromFile(f);
                    uploadToFirebase(contentUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (requestCode == PICK_IMAGE) {
            //Εδω θα μπει η συνδεση με το Firebase και το Machine Learning κομμάτι που θα επιστρέφει την κατηγοριοποίηση της εικόνας
            Uri loadURI = data.getData();
            try {
                File f = createImageFile();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), loadURI);
                FileOutputStream out = new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, out);
                uploadToFirebase(Uri.fromFile(f));
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.d("PHOTOTAG", "OOPS SOmEthing Happened");
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.gallery_ai.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void chooseImage(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    public void checkifNewUser(){
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        try{
            db.collection(userID);

        }catch (Exception e){
            db.collection(userID).document("thisEMptyDOc").set(dummyHash);
        }
    }

    private void uploadToFirebase(final Uri uri){
        @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final StorageReference image = initialReference.child("dogs/dog"+timeStamp);
        final String generatedLabel = generateLabel(timeStamp);
        image.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) { final FirebaseFirestore db = FirebaseFirestore.getInstance();
                            imageData.put("url",uri.toString());
                            addToFirestore(db, generatedLabel, imageData);
                            new updateImageViews().execute();
                            Toast.makeText(getApplicationContext(), "Η εικόνα ανέβηκε με επιτυχία", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) { Toast.makeText(getApplicationContext(), "Υπήρξε κάποιο σφάλμα κατά το ανέβασμα της εικόνας", Toast.LENGTH_SHORT).show();

                    }});
    }

    @SuppressLint("StaticFieldLeak")
    private class updateImageViews extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            final FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference collRef = db.collection(userID);
            collRef.get().
                    addOnCompleteListener(
                            new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        userLabels.clear();
                                        userUrls.clear();
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            userLabels.add(document.getId());
                                            userUrls.add(document.getData().get("url").toString());
                                        }
                                        ImageAdapterGridView adapter = new ImageAdapterGridView(GalleryGrid.this, userUrls.size());
                                        androidGridView.setAdapter(adapter);


                                    } else {
                                        Log.d("TAG", "No such document");
                                    }
                                }
                            });
            return  null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }


    public class ImageAdapterGridView extends BaseAdapter {
        private final Context mContext;
        private final int mCount;


        public ImageAdapterGridView(Context c, int a) {
            mContext = c;
            mCount = a;
        }

        public int getCount() {
            /*na ginei to mhkos twn photo*/return mCount;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater li = ((Activity) mContext).getLayoutInflater();
            @SuppressLint({"ViewHolder", "InflateParams"}) final View myView= li.inflate(R.layout.row_data, null);
            final ImageView img =  myView.findViewById(R.id.imagelayout);
            TextView txt =  myView.findViewById(R.id.textlayout);
            txt.setText(userLabels.get(position));
            Log.d("TAGI",String.valueOf(position));
            Picasso.with(mContext).load(userUrls.get(position)).fit().centerCrop().into(img);

            androidGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent,
                                        View v, int position, long id) {
                    Toast.makeText(getBaseContext(), "Grid Item " + (position + 1) + " Selected", Toast.LENGTH_LONG).show();
                    Intent myIntent = new Intent(GalleryGrid.this, FullScreen.class);
                    myIntent.putExtra("tourl", userUrls.get(position));
                    myIntent.putExtra("tolabel", userLabels.get(position));
                    startActivity(myIntent);
                    overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
                }
            });
            return myView;
        }
    }

    public void addToFirestore(FirebaseFirestore collection, String label, Map<String, Object>  data){
        collection.collection(userID).document(label).set(data);
    }

    public String generateLabel(String dummyValue){
        return "dog"+dummyValue;
    }

    public void signOut(){
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(GalleryGrid.this, UserLogin.class));

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_options_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signOut:
                signOut();
                return true;
            case R.id.upload:
                dispatchTakePictureIntent();
                return true;
            case R.id.load:
                chooseImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }




}

