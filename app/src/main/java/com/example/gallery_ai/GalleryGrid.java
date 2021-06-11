
package com.example.gallery_ai;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.util.TimingLogger;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ScrollView;
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
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.toMap;

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

import org.json.JSONArray;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
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
    private static int offlineMode;
    private static String theAddress =  "http://192.168.2.40:8000";


    /** Output probability TensorBuffer. */
    private  TensorBuffer outputProbabilityBuffer;
    private EditText searchField;
    private Button searchButton;

    StorageReference initialReference;
    GridView androidGridView;
    final FirebaseFirestore db = FirebaseFirestore.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_grid);
        theSharedPref();
        selectMLModels();
        //offlineMode();

        dummyHash.put("url","loading...");
        //dummyHash.put("timestamp","loading");
        dummyHash.put("label","loading");

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
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAMERA) {
            try {
                File f = new File(currentPhotoPath);
                File f2 = new File(currentPhotoPath+1);
                try {
                    Bitmap bitmap = getBitmapFormUri(GalleryGrid.this, photoURI, 1000,500);
                    Bitmap bitmap2 = getBitmapFormUri(GalleryGrid.this, photoURI, 331,331);
                    FileOutputStream out = new FileOutputStream(f);
                    FileOutputStream out2 = new FileOutputStream(f2);
                    bitmap = fixImage(bitmap);
                    //bitmap2 = fixImage(bitmap2);
                    //bitmap = Bitmap.createScaledBitmap(bitmap,224, 224,true);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 25, out); // bmp is your Bitmap instance
                    Log.d("modelLink", modelLink);
                    @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String genLabel = generateLabel(timeStamp,0);
                    dummyHash.put("timestamp", genLabel);
                    addToFirestore(db,genLabel,dummyHash);


                    firebaseUpload fbupload = new firebaseUpload(Uri.fromFile(f), "loading...", genLabel, f, bitmap);
                    new Thread(fbupload).start();

                    if(offlineMode==0){
                        bitmap2 = Bitmap.createScaledBitmap(bitmap2,224, 224,true);
                        bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, out2);

                        MyRunnable theRunnable = new MyRunnable(modelLink, genLabel,f2,1231231, 0);
                        new Thread(theRunnable).start();
                    }
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
                if(clipped.getItemCount() == 1){throw new Exception();}
                    @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    long startTimeBatch = System.nanoTime();
                    //closeitAll2 theClose = new closeitAll2(clipped,timeStamp, startTimeBatch);
                    //new Thread(theClose).start();
                    List< File > FOTO = new ArrayList<>();
                    List< File > filesOne = new ArrayList<>();
                   ArrayList<String> genLabels = new ArrayList<>();
                    ArrayList<Integer> orderNums = new ArrayList<>();
                for (int i = 0; i < clipped.getItemCount(); i++) {
                    closeitAll2 theClose = new closeitAll2(clipped,timeStamp, startTimeBatch, i, FOTO, filesOne, genLabels, orderNums);
                    new Thread(theClose).start();
                    //closeitAll theClose = new closeitAll(clipped.getItemAt(i), i, timeStamp, startTimeBatch);
                   // new Thread(theClose).start();
                }
                }
                catch(Exception e){
                    //long startTimet = System.nanoTime();
                    Uri loadURI = data.getData();
                    File f = createImageFile();

                    FileOutputStream out = new FileOutputStream(f);
                    Bitmap bitmap = getBitmapFormUri(GalleryGrid.this, loadURI, 1000,500);
                    bitmap = fixImage(bitmap);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 25, out);

                    @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String genLabel = generateLabel(timeStamp,0);
                    dummyHash.put("timestamp", genLabel);
                    addToFirestore(db, genLabel, dummyHash);

                    firebaseUpload fbupload = new firebaseUpload(Uri.fromFile(f), "loading...", genLabel, f, bitmap);
                    new Thread(fbupload).start();

                    if(offlineMode==0){
                        long startTimet = System.nanoTime();
                        File f2 = new File(currentPhotoPath+1);
                        FileOutputStream out2 = new FileOutputStream(f2);
                        //long startTimet = System.nanoTime();
                        Bitmap bitmap2 = getBitmapFormUri(GalleryGrid.this, loadURI, 331,331);
                        bitmap2 = fixImage(bitmap2);
                        bitmap2 = Bitmap.createScaledBitmap(bitmap2,331, 331,true);
                        long resizingt = System.nanoTime();
                        bitmap2.compress(Bitmap.CompressFormat.JPEG, 35, out2);
                        long compressiont = System.nanoTime();

                        //System.out.println("xronos resize:"+(resizingt-startTimet)/1000000000.00);
                        Log.d("xronos resizecomp:", String.valueOf(((compressiont-startTimet)/1000000000.00)));
                        multipleMLUpload mutlipleML = new multipleMLUpload(modelLink, genLabel,f2, startTimet, 0);
                        new Thread(mutlipleML).start();

                    }
                    //offlineTensor classifyImages = new offlineTensor(bitmap,genLabel);
                    //new Thread(classifyImages).start();
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

        try{
            db.collection(userID);

        }catch (Exception e){
            db.collection(userID).document("thisEMptyDOc").set(dummyHash);
        }
    }



    private Map.Entry<String,Float> localTensor(Bitmap bitmap) throws IOException {
        long startTimet = System.nanoTime();
        MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(this, "resnet_v2_101_29d9.tflite");
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
        long endTimet = System.nanoTime();
        long duration = endTimet - startTimet;
        System.out.println("Xronos local TEnsor: "+maxEntry.getKey());
    return maxEntry;
    }

    /*private void uploadToFirebase(final Uri uri, String theKey, File postFile, int loop, String generatedLabel){

        final StorageReference image = initialReference.child("dogs/"+generatedLabel);
        //final String generatedLabel = generateLabel(timeStamp, loop);

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
    }*/


    @SuppressLint("StaticFieldLeak")
    public class updateImageViews extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
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
    public String generateLabel(String dummyValue, int count){
        return dummyValue+count;
    }

    private void searchLabels(final String queue){
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
            //case R.id.refresh:
                //new updateImageViews().execute();
                //return true;
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


    Thread updateImages = new Thread() {
        @Override
        public void run() {
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
        //String address = "http://070fb1fe387b.ngrok.io";
        String address = "http://192.168.2.40:8000";
        //String address = "http://147.102.9.93:5000";
        //String address = "http://147.102.9.97:5000";
        String[] names = new String[]{"MobileNet", "ResNet", "VGGNet", "EfficientNet", "NasNet", "XceptionNet","EfficientB7"};
        String[] arraySpinner = new String[] {
                "/v1/vision/mobileNet2", "/v1/vision/resNet","/v1/vision/vggNet","/v1/vision/efficientNet","/v1/vision/nasNet","/v1/vision/xceptionNet","/v1/vision/efficientNetB7"};
        Spinner s = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);


        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                modelLink = theAddress + arraySpinner[position];
            } // to close the onItemSelected
            public void onNothingSelected(AdapterView<?> parent)
            {
                modelLink = theAddress + names[s.getSelectedItemPosition()];
            }
        });
    }

    /*private void offlineMode(){
        String[] status = new String[]{"On", "Off"};
        Spinner s1 = (Spinner) findViewById(R.id.spinner2);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, status);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s1.setAdapter(adapter2);

        s1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if(position==0){
                    offlineMode = 0;
                }
                else{
                    offlineMode = 1;
                }

            } // to close the onItemSelected
            public void onNothingSelected(AdapterView<?> parent)
            {
                offlineMode = 0;
            }
        });

    }*/

    private class MyRunnable implements Runnable {
        private volatile String myParam;
        private volatile String mydocID;
        private volatile File myFile;
        private volatile long startTime;
        private volatile int i;


        public MyRunnable(String myParam, String mydocID, File myFile, long startTime, int i){
            this.myParam = myParam;
            this.mydocID = mydocID;
            this.myFile = myFile;
            this.startTime = startTime;
            this.i = i;
        }

        public void postRequest(String url, String documentID, File file, long startTime, int i){
            Log.d("documentID", documentID);
            AndroidNetworking.upload(url)
                    .addMultipartFile("image",file)
                    .setPriority(Priority.HIGH)
                    .build()
                    .setUploadProgressListener(new UploadProgressListener() {
                        @Override
                        public void onProgress(long bytesUploaded, long totalBytes) {
                            //Log.d("bytesUploaded", System.nanoTime()/1000000000+"-"+(bytesUploaded)+"/"+(totalBytes));
                            // do anything with progress
                        }
                    })
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            long MethodeDuration = (System.nanoTime() -startTime);
                            Log.d("XRONOS", String.valueOf(MethodeDuration/1000000000.00));
                            // do anything with response
                            try {
                                JSONArray maxResult = response.getJSONArray("labels");
                                //JSONObject maxResult = (JSONObject) response.getJSONArray("labels").get(0);
                                db.collection(userID).document(documentID).update("label", maxResult.get(0))
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d("TAG", "DocumentSnapshot successfully updated!");
                                                new updateImageViews().execute();
                                                //long MethodeDuration = 0;
                                                if(i!=100){
                                                    Toast.makeText(getApplicationContext(),  String.valueOf(MethodeDuration/1000000000)+"s", Toast.LENGTH_SHORT).show();
                                                }
                                                //Log.d("XRONOS", String.valueOf(MethodeDuration/1000000000));
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w("TAG", "Error updating document", e);
                                            }
                                        });
                                //uploadToFirebase(Uri.fromFile(file), (String) maxResult.get("label"));
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

        public void run(){
            postRequest(myParam,mydocID,myFile, startTime, i);
        }
    }

    private class multipleMLUpload implements Runnable {
        private volatile String myParam;
        private volatile String mydocID;
        private volatile File myFile;
        private volatile long startTime;
        private volatile int i;
        ArrayList<File> photo1 = new ArrayList<>();


        public multipleMLUpload(String myParam, String mydocID, File myFile, long startTime, int i){
            this.myParam = myParam;
            this.mydocID = mydocID;
            this.myFile = myFile;
            this.startTime = startTime;
            this.i = i;

        }

        public void postRequest(String url, String documentID, File file, long startTime, int i){
            photo1.add(file);
            long second = System.nanoTime();
            final double[] delay = new double[1];
            Log.d("documentID", documentID);
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    . writeTimeout(120, TimeUnit.SECONDS)
                    .build();
            AndroidNetworking.upload(modelLink)
                    .setOkHttpClient(okHttpClient)
                    .addMultipartFileList("image", photo1)
                    .setPriority(Priority.HIGH)
                    .build()
                    .setUploadProgressListener(new UploadProgressListener() {
                        @Override
                        public void onProgress(long bytesUploaded, long totalBytes) {
                            Log.d("Uploaded...", bytesUploaded+"/"+totalBytes);
                            // do anything with progress
                            if(bytesUploaded==totalBytes){
                                Log.d("Xronos anevasmatos", String.valueOf((System.nanoTime() - second)/1000000000.00));
                                delay[0] = (double) System.nanoTime()/1000000000;
                            }
                        }
                    })
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            long third = System.nanoTime();
                            //Log.d("Xronos delay", String.valueOf((third - delay[0])/1000000000.00));

                            // do anything with response
                            try {
                                JSONArray maxResult = response.getJSONArray("labels");

                                double theAnswer = (double) System.nanoTime()/1000000000;
                                JSONArray times = response.getJSONArray("times");
                                double addedTime = (double) (times.get(0)) + (double) (times.get(1));
                                Log.d("Xronos dia server", String.valueOf((times.get(0))));
                                Log.d("Xronos inf server", String.valueOf((times.get(1))));
                                Log.d("Xronos apanthshs", String.valueOf((theAnswer - delay[0] - addedTime)));




                                third= System.nanoTime();
                                db.collection(userID).document(documentID).update("label", maxResult.get(0))
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w("TAG", "Error updating document", e);
                                            }
                                        });
                                new updateImageViews().execute();
                                Log.d("Xronos sto firestore", String.valueOf((System.nanoTime() - third)/1000000000.00));
                                Log.d("Synolikos xronos", String.valueOf((System.nanoTime() - startTime)/1000000000.00));

                                //uploadToFirebase(Uri.fromFile(file), (String) maxResult.get("label"));
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

        public void run(){
            postRequest(myParam,mydocID,myFile, startTime, i);
        }
    }



    private class firebaseUpload implements Runnable {
        private volatile Uri uri;
        private volatile String theKey;
        private volatile String genLabel;
        private volatile File myFile;
        private volatile Bitmap bitmap;


        public firebaseUpload(Uri uri, String theKey, String genLabel, File myFile, Bitmap bitmap){
            this.uri = uri;
            this.theKey = theKey;
            this.genLabel = genLabel;
            this.myFile = myFile;
            this.bitmap = bitmap;

        }

        private void uploadToFirebase(final Uri uri, String theKey, String generatedLabel, File myFile, Bitmap bitmap){
            final StorageReference image = initialReference.child("dogs/"+generatedLabel);
            image.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            //imageData.put("url",uri.toString());
                            //imageData.put("timestamp",generatedLabel);
                            //imageData.put("label","loading...");
                            //addToFirestore(db, generatedLabel, imageData);

                            new updateImageViews().execute();
                            db.collection(userID).document(genLabel).update("url",uri.toString())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            new updateImageViews().execute();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                        }
                                    });

                            if(offlineMode==1){
                                try {
                                    localTensor(bitmap).getKey();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            /*try {
                                db.collection(userID).document(genLabel).update("label",localTensor(bitmap).getKey())
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                new updateImageViews().execute();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                            }
                                        });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }*/}


                            //offlineTensor myTensor = new offlineTensor(bitmap, genLabel);
                            //new Thread(myTensor).start();
                            //updateImages.start();
                            //MyRunnable theRunnable = new MyRunnable(modelLink, genLabel,myFile);
                            //new Thread(theRunnable).start();
                            //new updateImageViews().execute();
                            Toast.makeText(getApplicationContext(), "Η εικόνα ανέβηκε με επιτυχία", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) { Toast.makeText(getApplicationContext(), "Υπήρξε κάποιο σφάλμα κατά το ανέβασμα της εικόνας", Toast.LENGTH_SHORT).show();

                }});
        }

        public void run(){
            uploadToFirebase(uri, "loading...",genLabel, myFile, bitmap);
        }
    }

    private class offlineTensor implements Runnable {
        private volatile Bitmap bitmap;
        private volatile String genLabel;


        public offlineTensor(Bitmap bitmap, String genLabel){
            this.bitmap = bitmap;
            this.genLabel = genLabel;

        }

        public void doSmth(String classification, String genLabel) throws IOException {

            db.collection(userID).document(genLabel).update("label",classification)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                        }
                    });
            new updateImageViews().execute();
        }

        public void run(){
            try {
                String classification = localTensor(bitmap).getKey();
                doSmth(classification,genLabel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Bitmap fixImage(Bitmap bitmap){
        int cameraAngle = getCameraAngle();
        String manufacturer = Build.MANUFACTURER;
        if(manufacturer.equals("XIAOMI")||manufacturer.equals("Xiaomi")||manufacturer.equals("Samsung")||manufacturer.equals("SAMSUNG")) {
            if(cameraAngle == 180){bitmap = rotateBitmap(bitmap,180);}
            else if(cameraAngle == 270){bitmap = rotateBitmap(bitmap,90);}

    }return bitmap;
}

    private class closeitAll implements Runnable {
        private volatile ClipData.Item mItem;
        private volatile int i;
        private volatile String timeStamp;
        private volatile long startTimeBatch;


        public closeitAll(ClipData.Item mItem, int i, String timeStamp, long startTimeBatch){
            this.mItem = mItem;
            this.i = i;
            this.timeStamp = timeStamp;
            this.startTimeBatch = startTimeBatch;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void doSmth(ClipData.Item mItem, int i, String timeStamp, long startTimeBatch) throws IOException {
            File f = createImageFileMultiple(i);
            //long startTimet = System.nanoTime();
            long first = System.nanoTime();
            File f2 = new File(currentPhotoPath+i);
            Bitmap bitmap2 = getBitmapFormUri(GalleryGrid.this, mItem.getUri(),224,224);
            FileOutputStream out2 = new FileOutputStream(f2);
            bitmap2 = fixImage(bitmap2);


            if(offlineMode==0) {
                bitmap2 = Bitmap.createScaledBitmap(bitmap2, 224, 224, false);
                bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, out2);
                Log.d("EKkinhsh sympieshs: ", String.valueOf(first));
                Log.d("Lhxh sympieshs: ", String.valueOf(System.nanoTime()));
                System.out.println("HElllo");
                String genLabel = generateLabel(timeStamp,i);
                dummyHash.put("timestamp", genLabel);
                //addToFirestore(db,genLabel,dummyHash);

                MyRunnable theRunnable = new MyRunnable(modelLink, genLabel, f2, startTimeBatch, i);
                new Thread(theRunnable).start();
            }
            /*Bitmap bitmap = getBitmapFormUri(GalleryGrid.this, mItem.getUri(), 1000,500);
            FileOutputStream out = new FileOutputStream(f);
            //bitmap = fixImage(bitmap);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, out);


            firebaseUpload fbupload = new firebaseUpload(Uri.fromFile(f), "loading...", genLabel, f, bitmap);
            new Thread(fbupload).start();*/

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void run(){
            try {
                doSmth(mItem, i, timeStamp,startTimeBatch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class closeitAll2 implements Runnable {
        private volatile ClipData mItem;
        private volatile int i;
        private volatile String timeStamp;
        private volatile long startTimeBatch;
        private volatile  List< File > FOTO = new ArrayList<>();
        private volatile List< File > filesOne = new ArrayList<>();
        private volatile ArrayList<String> genLabels = new ArrayList<>();
        private volatile ArrayList<Integer> orderNums = new ArrayList<>();





        public closeitAll2(ClipData mItem, String timeStamp, long startTimeBatch, int i, List<File> FOTO, List<File> filesOne, ArrayList<String> genLabels, ArrayList<Integer> orderNums){
            this.mItem = mItem;
            this.i = i;
            this.timeStamp = timeStamp;
            this.startTimeBatch = startTimeBatch;
            this.genLabels =  genLabels;
            this.filesOne = filesOne;
            this.FOTO = FOTO;
            this.orderNums = orderNums;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void doSmth(ClipData mItem, String timeStamp, long startTimeBatch, int i, List<File> FOTO, List<File> filesOne, ArrayList<String> genLabels,  ArrayList<Integer> orderNums) throws IOException {

            //long startTimet = System.nanoTime();
            long first = System.nanoTime();

            File f = createImageFileMultiple(i);
            File f2 = new File(currentPhotoPath+i);
            Bitmap bitmap2 = getBitmapFormUri(GalleryGrid.this, mItem.getItemAt(i).getUri(),224,224);
            FileOutputStream out2 = new FileOutputStream(f2);
            bitmap2 = fixImage(bitmap2);
            if(offlineMode==0) {
                bitmap2 = Bitmap.createScaledBitmap(bitmap2, 331, 331, true);
                bitmap2.compress(Bitmap.CompressFormat.JPEG, 25, out2);
                //Log.d("EKkinhsh sympieshs: ", String.valueOf(first));
                //Log.d("Lhxh sympieshs: ", String.valueOf(System.nanoTime()));
                //System.out.println("HElllo");
                String genLabel = generateLabel(timeStamp,i);
                dummyHash.put("timestamp", genLabel);
                addToFirestore(db,genLabel,dummyHash);
                //sygekrimenh thesh
                FOTO.add(f2);
                filesOne.add(f);
                genLabels.add(genLabel);
                orderNums.add(i);
                bitmap2.recycle();

            }

            if(FOTO.size()== mItem.getItemCount()){
                long sympiesh = System.nanoTime() - first;
           // Log.d("Xronos sympieshs", String.valueOf((System.nanoTime() - first)/1000000000.00));
                Map<Integer, File> map = new HashMap<Integer, File>();
                Map<Integer, File> map2 = new HashMap<Integer, File>();
                Map<Integer, String> map3 = new HashMap<Integer, String>();
                for(int k = 0; k < orderNums.size(); k++) {
                    map.put(orderNums.get(k), FOTO.get(k));
                    map2.put(orderNums.get(k), filesOne.get(k));
                    map3.put(orderNums.get(k), genLabels.get(k));
                }

                Collections.sort(orderNums);
                FOTO.clear();
                filesOne.clear();
                genLabels.clear();

                for(int l = 0; l < map.size(); l++) {
                    FOTO.add(map.get(orderNums.get(l)));
                    filesOne.add(map2.get(orderNums.get(l)));
                    genLabels.add(map3.get(orderNums.get(l)));
                }




            long second = System.nanoTime();
                final double[] delay = new double[1];
                final long[] anevasma = new long[1];
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    . writeTimeout(300, TimeUnit.SECONDS)
                    .build();
            AndroidNetworking.upload(modelLink)
                    .setOkHttpClient(okHttpClient)
                        .addMultipartFileList("image", FOTO)
                        .setPriority(Priority.HIGH)
                        .build()
                        .setUploadProgressListener(new UploadProgressListener() {
                            @Override
                            public void onProgress(long bytesUploaded, long totalBytes) {
                                Log.d("Uploaded...", bytesUploaded+"/"+totalBytes);
                                // do anything with progress
                                if(bytesUploaded==totalBytes){
                                    anevasma[0] = System.nanoTime() - second;
                                    Log.d("Xronos anevasmatos", String.valueOf((System.nanoTime() - second)/1000000000.00));
                                    delay[0] = (double) (System.nanoTime()/1000000000.00);
                                }
                            }
                        })
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                //Log.d("Xronos delay", String.valueOf(((System.nanoTime() - delay[0])/1000000000.00)));
                                // do anything with response
                                try {
                                    double theAnswer = (double) (System.nanoTime()/1000000000.00);
                                    JSONArray maxResult = response.getJSONArray("labels");
                                    JSONArray times = response.getJSONArray("times");
                                    double addedTime = (double) (times.get(0)) + (double) (times.get(1));
                                    Log.d("Xronos dia server", String.valueOf((times.get(0))));
                                    Log.d("Xronos inf server", String.valueOf((times.get(1))));
                                    //Log.d("Xronos apanthshs", String.valueOf((theAnswer - delay[0] - addedTime)));
                                    long startingUpdateTime = System.nanoTime();
                                    for(int i = 0; i< maxResult.length(); i++){
                                        db.collection(userID).document(genLabels.get(i)).update("label", maxResult.get(i))
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Log.d("TAG", "DocumentSnapshot successfully updated!");

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.w("TAG", "Error updating document", e);
                                                    }
                                                });
                                    }
                                    System.out.println(genLabels);
                                    long firebaseTime =  System.nanoTime() - startingUpdateTime;
                                    long synolikos = System.nanoTime() - startTimeBatch;
                                    Log.d("Xronos sto firebase...", String.valueOf((System.nanoTime() - startingUpdateTime)/1000000000.00));
                                    Log.d("ola", (sympiesh/1000000000.00)+","+anevasma[0]/1000000000.00+","+times.get(0)+","+times.get(1)+","+firebaseTime/1000000000.00+","+ synolikos/1000000000.00);
                                    System.out.println("ola: "+(sympiesh/1000000000.00)+","+anevasma[0]/1000000000.00+","+times.get(0)+","+times.get(1)+","+firebaseTime/1000000000.00+","+ synolikos/1000000000.00);


                                    new updateImageViews().execute();

                                    //uploadToFirebase(Uri.fromFile(file), (String) maxResult.get("label"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public void onError(ANError error) {
                                // handle error
                            }
                        });
                //new updateImageViews().execute();

            for(int j=0;j<genLabels.size();j++){
                Bitmap bitmap = getBitmapFormUri(GalleryGrid.this, mItem.getItemAt(j).getUri(), 1000,500);
                FileOutputStream out = new FileOutputStream(filesOne.get(j));
                //bitmap = fixImage(bitmap);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, out);
                firebaseUpload fbupload = new firebaseUpload(Uri.fromFile(filesOne.get(j)), "loading...", genLabels.get(j), filesOne.get(j), bitmap);
                new Thread(fbupload).start();
            }
                //FOTO.clear();
                //filesOne.clear();
                //genLabels.clear();



                //MyRunnable theRunnable = new MyRunnable(modelLink, genLabel, f2, startTimeBatch, i);
                //new Thread(theRunnable).start();
            }
        }
            /*Bitmap bitmap = getBitmapFormUri(GalleryGrid.this, mItem.getUri(), 1000,500);
            FileOutputStream out = new FileOutputStream(f);
            //bitmap = fixImage(bitmap);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, out);


            firebaseUpload fbupload = new firebaseUpload(Uri.fromFile(f), "loading...", genLabel, f, bitmap);
            new Thread(fbupload).start();*/



        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void run(){
            try {
                doSmth(mItem, timeStamp,startTimeBatch, i, FOTO, filesOne, genLabels, orderNums);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap getBitmapFormUri(Activity ac, Uri uri, float hh, float ww) throws FileNotFoundException, IOException {
        InputStream input = ac.getContentResolver().openInputStream(uri);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        int originalWidth = onlyBoundsOptions.outWidth;
        int originalHeight = onlyBoundsOptions.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1))
            return null;
        //Image resolution is based on 480x800
        //float hh = 800f;//The height is set as 800f here
        //float ww = 480f;//Set the width here to 480f

        //float hh = 1000f;//The height is set as 800f here
        //float ww = 500f;//Set the width here to 480f
        //Zoom ratio. Because it is a fixed scale, only one data of height or width is used for calculation
        int be = 1;//be=1 means no scaling

        if (originalWidth > originalHeight && originalWidth > ww) {//If the width is large, scale according to the fixed size of the width
            be = (int) (originalWidth / ww);
        } else if (originalWidth < originalHeight && originalHeight > hh) {//If the height is high, scale according to the fixed size of the width
            be = (int) (originalHeight / hh);
        }
        if (be <= 0)
            be = 1;
        System.out.println(be);
        //Proportional compression
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = be;//Set scaling
        bitmapOptions.inDither = true;//optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        input = ac.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();

        return compressImage(bitmap);//Mass compression again
    }

    /**
     * Mass compression method
     *
     * @param image
     * @return
     */
    public static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//Quality compression method, here 100 means no compression, store the compressed data in the BIOS
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {  //Cycle to determine if the compressed image is greater than 100kb, greater than continue compression
            baos.reset();//Reset the BIOS to clear it
            //First parameter: picture format, second parameter: picture quality, 100 is the highest, 0 is the worst, third parameter: save the compressed data stream
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//Here, the compression options are used to store the compressed data in the BIOS
            options -= 10;//10 less each time
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//Store the compressed data in ByteArrayInputStream
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//Generate image from ByteArrayInputStream data
        return bitmap;
    }
}





