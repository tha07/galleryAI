
package com.example.gallery_ai;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.ExifInterface;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.example.gallery_ai.UserLogin.userID;
import static java.lang.Math.min;

import com.google.android.gms.tasks.Task;
import com.google.common.net.MediaType;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query.Direction;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.TensorFlowLite;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class GalleryGrid extends AppCompatActivity {
    public String currentPhotoPath;
    public static Uri photoURI;
    private static final int REQUEST_CODE_CAMERA = 1;
    public static final int PICK_IMAGE = 2;
    public static List<String> userUrls = new ArrayList<>();
    public static List<String> userLabels = new ArrayList<>();
    public static List<String> userTimestamps = new ArrayList<>();
    public static List<Uri> allUris = new ArrayList<>();
    public static Map<String, Object> imageData = new HashMap<>();
    public static Map<String, Object> dummyHash = new HashMap<>();
    private List<String> labels;
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    private TensorImage inputImageBuffer;
    private  int imageSizeX;
    private  int imageSizeY;
    private  TensorProcessor probabilityProcessor;
    private static String modelLink;

    /** Output probability TensorBuffer. */
    private  TensorBuffer outputProbabilityBuffer;
    private EditText searchField;
    private Button searchButton;

    StorageReference initialReference;
    GridView androidGridView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_grid);

        theSharedPref();
        selectMLModels();
        //android.hardware.Camera.CameraInfo camInfo = new android.hardware.Camera.CameraInfo();
        //Camera.getCameraInfo(1,camInfo);
        //int cameraRotationOffset = camInfo.orientation;
        //System.out.println("CAMOffset: "+cameraRotationOffset);

        androidGridView = findViewById(R.id.gridview_android_example);
        initialReference = FirebaseStorage.getInstance().getReference();
        searchField = findViewById(R.id.editTextTextPersonName);
        searchButton = findViewById(R.id.button2);
        checkifNewUser();
        //updateImages.start();
        new updateImageViews().execute();

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchLabels(searchField.getText().toString());
                System.out.println("PATHSE");
            }

        });
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAMERA) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                File f = new File(currentPhotoPath);
                try {
                    FileOutputStream out = new FileOutputStream(f);
                    int cameraAngle = getCameraAngle();
                    Log.d("Camera", String.valueOf(cameraAngle));
                    String manufacturer = Build.MANUFACTURER;
                    if(manufacturer.equals("XIAOMI")||manufacturer.equals("Xiaomi")||manufacturer.equals("Samsung")||manufacturer.equals("SAMSUNG")) {
                        if(cameraAngle == 180){bitmap = rotateBitmap(bitmap,180);}
                        else if(cameraAngle == 270){bitmap = rotateBitmap(bitmap,90);}
                    }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 15, out); // bmp is your Bitmap instance
                    Log.d("modelLink", modelLink);

                    postRequest(modelLink,f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
                startActivity(new Intent(this, GalleryGrid.class));
            }
        }
        if (requestCode == PICK_IMAGE) {
            try {
                try{
                ClipData clipped = data.getClipData();
                for (int i = 0; i < clipped.getItemCount(); i++) {
                    ClipData.Item mItem = clipped.getItemAt(i);
                    File f = createImageFileMultiple(i);
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mItem.getUri());
                    FileOutputStream out = new FileOutputStream(f);
                    int cameraAngle = getCameraAngle();
                    if(Build.MANUFACTURER.equals("XIAOMI")||Build.MANUFACTURER.equals("Xiaomi")||Build.MANUFACTURER.equals("Samsung")||Build.MANUFACTURER.equals("SAMSUNG")) {
                        if(cameraAngle == 180){bitmap = rotateBitmap(bitmap,180);}
                        else if(cameraAngle == 270){bitmap = rotateBitmap(bitmap,90);}
                    }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 15, out);
                    postRequest(modelLink,f);
                }
                }
                catch(Exception e){
                    Uri loadURI = data.getData();
                    File f = createImageFile();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), loadURI);
                    FileOutputStream out = new FileOutputStream(f);
                    int cameraAngle = getCameraAngle();
                    if(Build.MANUFACTURER.equals("XIAOMI")||Build.MANUFACTURER.equals("Xiaomi")||Build.MANUFACTURER.equals("Samsung")||Build.MANUFACTURER.equals("SAMSUNG")) {
                        if(cameraAngle == 180){bitmap = rotateBitmap(bitmap,180);}
                        else if(cameraAngle == 270){bitmap = rotateBitmap(bitmap,90);}
                    }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 15, out);
                    postRequest(modelLink,f);

                    /*MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(this, "mobilenet_v1_1.0_224_quant.tflite");
                    Interpreter tflite = new Interpreter(tfliteModel, tfliteOptions);
                    // Loads labels out from the label file.
                    labels = FileUtil.loadLabels(this, "labels_mobilenet_quant_v1_224.txt");
                    int imageTensorIndex = 0;
                    int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
                    imageSizeY = imageShape[1];
                    imageSizeX = imageShape[2];
                    DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
                    int probabilityTensorIndex = 0;
                    int[] probabilityShape =
                            tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
                    DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

                    // Creates the input tensor.
                    inputImageBuffer = new TensorImage(imageDataType);

                    // Creates the output tensor and its processor.
                    outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
                    probabilityProcessor = new TensorProcessor.Builder().build();
                    inputImageBuffer = loadImage(bitmap,0);
                    tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());
                    Map<String, Float> labeledProbability =
                            new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                                    .getMapWithFloatValue();

                    Map.Entry<String, Float> maxEntry = null;

                    for (Map.Entry<String, Float> entry : labeledProbability.entrySet())
                    {
                        if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                        {
                            maxEntry = entry;
                        }
                    }
                    uploadToFirebase(Uri.fromFile(f), maxEntry.getKey());*/
                }
            } catch (Exception e) {
                e.printStackTrace();
                startActivity(new Intent(this, GalleryGrid.class));
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

    private File createImageFileMultiple(int label) throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_"+label;
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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
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



    private void uploadToFirebase(final Uri uri, String theKey){
        @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final StorageReference image = initialReference.child("dogs/"+timeStamp);
        final String generatedLabel = generateLabel(timeStamp);
        image.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) { final FirebaseFirestore db = FirebaseFirestore.getInstance();
                            imageData.put("url",uri.toString());
                            imageData.put("timestamp",generatedLabel);
                            imageData.put("label",theKey);
                            addToFirestore(db, generatedLabel, imageData);
                            new updateImageViews().execute();
                            //updateImages.start();
                            Toast.makeText(getApplicationContext(), "Η εικόνα ανέβηκε με επιτυχία", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) { Toast.makeText(getApplicationContext(), "Υπήρξε κάποιο σφάλμα κατά το ανέβασμα της εικόνας", Toast.LENGTH_SHORT).show();

                    }});
    }

    private void uploadToFirebaseMultiple(final Uri uri, int label){
        @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final StorageReference image = initialReference.child("dogs/"+timeStamp+label);
        final String generatedLabel = generateLabel(timeStamp+label);
        image.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) { final FirebaseFirestore db = FirebaseFirestore.getInstance();
                        imageData.put("url",uri.toString());
                        imageData.put("timestamp",timeStamp);
                        addToFirestore(db, generatedLabel, imageData);
                        new updateImageViews().execute();
                        //updateImages.start();
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
            collRef.orderBy("timestamp", Direction.DESCENDING).get().
                    addOnCompleteListener(
                            new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        userLabels.clear();
                                        userUrls.clear();
                                        userTimestamps.clear();
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            userLabels.add(document.getData().get("label").toString());
                                            userUrls.add(document.getData().get("url").toString());
                                            userTimestamps.add(document.getData().get("timestamp").toString());
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
                    myIntent.putExtra("totimestamp", userTimestamps.get(position));
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
        return dummyValue;
    }

    private void searchLabels(final String queue){
            final FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference collRef = db.collection(userID);

            collRef.orderBy("timestamp", Direction.DESCENDING).get().
                    addOnCompleteListener(
                            new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        userLabels.clear();
                                        userUrls.clear();
                                        userTimestamps.clear();
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            if (((String) document.getData().get("label")).contains(queue)) {
                                                userLabels.add((String) document.getData().get("label"));
                                                userUrls.add(document.getData().get("url").toString());
                                                userTimestamps.add(document.getData().get("timestamp").toString());
                                            }
                                        }
                                        if(userLabels.size()!=0){
                                        ImageAdapterGridView adapter = new ImageAdapterGridView(GalleryGrid.this, userUrls.size());
                                        androidGridView.setAdapter(adapter);}
                                    } else {
                                        Log.d("TAG", "No such document");
                                    }
                                }
                            });
    }

    public void signOut(){
        FirebaseAuth.getInstance().signOut();
        SharedPreferences saved_values = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = saved_values.edit();
        editor.remove("userID");
        editor.commit();
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getCameraAngle(){
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        int orientation = 0;
        try{
            String cameraID = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
            orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            System.out.println(orientation);
            return orientation;
        }
        catch(Exception e){
            return orientation;
        }
    }

    public Bitmap rotateBitmap(Bitmap original, float degrees) {
        Matrix matrix = new Matrix();
        matrix.preRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        original.recycle();
        return rotatedBitmap;
    }

    private TensorImage loadImage(final Bitmap bitmap, int sensorOrientation) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);
        // Creates processor for the TensorImage.
        int cropSize = min(bitmap.getWidth(), bitmap.getHeight());
        int numRotation = sensorOrientation / 90;
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        // TODO(b/169379396): investigate the impact of the resize algorithm on accuracy.
                        // To get the same inference results as lib_task_api, which is built on top of the Task
                        // Library, use ResizeMethod.BILINEAR.
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(new Rot90Op(numRotation))
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }

   public void postRequest(String url, File file){
        AndroidNetworking.upload(url)
               .addMultipartFile("image",file)
               .setPriority(Priority.HIGH)
               .build()
               .setUploadProgressListener(new UploadProgressListener() {
                   @Override
                   public void onProgress(long bytesUploaded, long totalBytes) {
                       // do anything with progress
                   }
               })
               .getAsJSONObject(new JSONObjectRequestListener() {
                   @Override
                   public void onResponse(JSONObject response) {
                       // do anything with response
                       try {
                           JSONObject maxResult = (JSONObject) response.getJSONArray("predictions").get(0);
                           uploadToFirebase(Uri.fromFile(file), (String) maxResult.get("label"));
                       } catch (JSONException e) {
                           e.printStackTrace();
                       }
                   }
                   @Override
                   public void onError(ANError error) {
                       // handle error
                   }
               });
   }

    Thread updateImages = new Thread() {
        @Override
        public void run() {
            final FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference collRef = db.collection(userID);

            collRef.orderBy("timestamp", Direction.DESCENDING).get().
                    addOnCompleteListener(
                            new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        userLabels.clear();
                                        userUrls.clear();
                                        userTimestamps.clear();
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            userLabels.add(document.getData().get("label").toString());
                                            userUrls.add(document.getData().get("url").toString());
                                            userTimestamps.add(document.getData().get("timestamp").toString());
                                        }
                                        ImageAdapterGridView adapter = new ImageAdapterGridView(GalleryGrid.this, userUrls.size());
                                        androidGridView.setAdapter(adapter);


                                    } else {
                                        Log.d("TAG", "No such document");
                                    }
                                }
                            });
        }
    };

    private void theSharedPref(){
        SharedPreferences sp = getSharedPreferences("lastUser", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("userID", userID);
        editor.apply();
    }

    private void selectMLModels(){
        String[] names = new String[]{"MobileNet", "InceptionV3"};
        String[] arraySpinner = new String[] {
                "https://5dcea46b9a82.ngrok.io/v1/vision/image", "https://5dcea46b9a82.ngrok.io/v1/vision/image2"};
        Spinner s = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);


        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                modelLink = arraySpinner[position];
            } // to close the onItemSelected
            public void onNothingSelected(AdapterView<?> parent)
            {
                modelLink = names[s.getSelectedItemPosition()];
            }
        });
    }


}


