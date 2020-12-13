package com.example.gallery_ai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import static com.example.gallery_ai.MainActivity.image_labelsMain;

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
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import static com.example.gallery_ai.MainActivity.userUrlsMain;
import static com.example.gallery_ai.MainActivity.userLabelsMain;
import static com.example.gallery_ai.UserLogin.userID;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.core.view.Event;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Array;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GalleryGrid extends AppCompatActivity {

    private Button changeButton;
    public String currentPhotoPath;
    public static Uri photoURI;
    private static final int REQUEST_CODE_CAMERA = 1;
    public static final int PICK_IMAGE = 2;
    public static List<String> userUrls = new ArrayList<>();
    public static List<String> userLabels = new ArrayList<>();
    ImageAdapterGridView adapter = new ImageAdapterGridView(GalleryGrid.this, userUrls.size());

    StorageReference initialReference, folderRefernce;
    GridView androidGridView;


    public static Map<String, Object> image_labels = new HashMap<>();






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //readFireStore();
        setContentView(R.layout.activity_gallery_grid);

        androidGridView = findViewById(R.id.gridview_android_example);
        printfromSuccess();

        initialReference = FirebaseStorage.getInstance().getReference();
        folderRefernce = initialReference.child("dogs/");


    }

    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAMERA) {
            try {
                //Εδω θα μπει η συνδεση με το Firebase και το Machine Learning κομμάτι που θα επιστρέφει την κατηγοριοποίηση της εικόνας
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                File f = new File(currentPhotoPath);
                try {
                    FileOutputStream out = new FileOutputStream(f);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 20, out); // bmp is your Bitmap instance
                    Uri contentUri = Uri.fromFile(f);
                    uploadToFirebase(contentUri);
                    //printfromSuccess();

                    // PNG is a lossless format, the compression factor (100) is ignored
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

                //imageClassifier.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    private void uploadToFirebase(final Uri uri){
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        StorageReference folderRefernce2 = initialReference.child("dogs/");
        folderRefernce2.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(final ListResult listResult) {
                final int size = listResult.getItems().size();
                final String[] array2 = new String[listResult.getItems().size()+1];

                for(int i = 0;i<listResult.getItems().size();i++){
                    array2[i]=String.valueOf(listResult.getItems().get(i));
                }

                //userUrls.clear();
                //userLabels.clear();
                final StorageReference image = initialReference.child("dogs/dog"+timeStamp);
                image.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                array2[size] = uri.toString();
                                final FirebaseFirestore db = FirebaseFirestore.getInstance();
                                CollectionReference collRef = db.collection(userID);
                                userLabelsMain.add("dog"+timeStamp);
                                userUrlsMain.add(uri.toString());
                                image_labelsMain.put("url",uri.toString());
                                db.collection(userID).document("dog"+timeStamp).set(image_labelsMain);
                                new MyTask().execute();
                                //image_labelsMain.put("dog28",uri.toString());
                                //Intent i = new Intent(GalleryGrid.this,MyService.class);
                                //getApplicationContext().startService(i);
                                //adapter = new ImageAdapterGridView(GalleryGrid.this, userUrls.size());
                                //androidGridView.setAdapter(adapter);
                                Toast.makeText(getApplicationContext(), "Η εικόνα ανέβηκε με επιτυχία", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Υπήρξε κάποιο σφάλμα κατά το ανέβασμα της εικόνας", Toast.LENGTH_SHORT).show();

                    }
                });

            }
        });
    }

    public class ImageAdapterGridView extends BaseAdapter {
        private Context mContext;
        private int mCount;
        private ArrayList mLabels;


        public ImageAdapterGridView(Context c, int a) {
            mContext = c;
            mCount = a;
            getLabel();

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

        public ArrayList getLabel(){ return mLabels; }


        public View getView(final int position, View convertView, ViewGroup parent) {

            LayoutInflater li = ((Activity) mContext).getLayoutInflater();
            final View myView= li.inflate(R.layout.row_data, null);
            final ImageView img =  myView.findViewById(R.id.imagelayout);
            TextView txt =  myView.findViewById(R.id.textlayout);
            txt.setText(userLabels.get(position));
            mLabels = (ArrayList) userLabels;


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

    public void printfromSuccess(){
        androidGridView.setAdapter(adapter);
    }


    private class MyTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {

            final FirebaseFirestore db = FirebaseFirestore.getInstance();


            CollectionReference collRef = db.collection(userID);
            collRef.get().
                    addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) { if (task.isSuccessful()) {
                                                        userLabels.clear();
                                                        userUrls.clear();
                                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                                //List<String> keys = new ArrayList<>(document.getData().keySet());
                                                                userLabels.add(document.getId());
                                                                userUrls.add(document.getData().get("url").toString());
                                                            }

                                                        ImageAdapterGridView adapter = new ImageAdapterGridView(GalleryGrid.this, userUrls.size());
                                                            androidGridView.setAdapter(adapter);
                                                            userUrlsMain = userUrls;
                                                            //startActivity(new Intent(MainActivity.this,GalleryGrid.class));


                                                        } else {
                                                            Log.d("TAG", "No such document");
                                                        }
                                                    }
                                                }
            );
        return  null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {

            adapter.notifyDataSetChanged();
            androidGridView.setAdapter(adapter);

            super.onPostExecute(aVoid);
        }


        }



    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_options_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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


