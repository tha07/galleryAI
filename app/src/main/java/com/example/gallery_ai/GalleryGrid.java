
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
    public String currentPhotoPath; // Τρέχοντας προορισμός εικόνας στη συσκευή
    public static Uri photoURI; // URI αποθήκευσης εικόνας στη συσκευή
    private static final int REQUEST_CODE_CAMERA = 1; // Κωδικός επιλογής κάμερας
    public static final int PICK_IMAGE = 2; // Κωδικός επιλογής αποθηκευμένης εικόνας
    public static List<String> userUrls = new ArrayList<>(); // Χρήση Arraylist για φόρτωση Urls των εικόνων απο το Firebase
    public static List<String> userLabels = new ArrayList<>(); // Χρήση Arraylist για φόρτωση Label των εικόνων απο το Firebase
    public static List<String> userTimestamps = new ArrayList<>(); //Χρήση Arraylist για φόρτωση Timestamp εικόνων απο το Firebase
    public static Map<String, Object> dummyHash = new HashMap<>(); // HashMap για αποστολή σε κενό γι
    private static String modelLink; // Μεταβλητή για το τελικό url που θα αποσταλεί το αρχείο εικόνων
    private static String theAddress =  "http://192.168.2.40:8000"; // IP:PORT Server Ταξινόμησης
    private int offlineMode = 0;
    String[] arraySpinner = new String[] {
            "/v1/vision/mobileNet2", "/v1/vision/resNet","/v1/vision/vggNet","/v1/vision/efficientNet","/v1/vision/nasNet","/v1/vision/xceptionNet","/v1/vision/efficientNetB7"}; //Τα link για κάθε μοντέλο στον Server Ταξινόμησης
    String[] names = new String[]{"MobileNet", "ResNet", "VGGNet", "EfficientNet", "NasNet", "XceptionNet","EfficientB7"}; //Οι ονομασίες κάθε μοντέλου αντίστοιχα

    /** Output probability TensorBuffer. */
    //private  TensorBuffer outputProbabilityBuffer;
    private EditText searchField; // Ορισμός μεταβλητης για το πεδίο αναζήτησης
    private Button searchButton; // Ορισμός μεταβλητης για το κουμπί αναζήτησης

    StorageReference initialReference; // Μεταβλητή που αναφέρεται στο Firebase Storage
    GridView androidGridView; // Ορισμός μεταβλητής GridView
    final FirebaseFirestore db = FirebaseFirestore.getInstance(); // Τελική μεταβλητή που αναφέρεται στο FireStore


    @Override
    protected void onCreate(Bundle savedInstanceState) { // Συνάρτηση δημιουργίας του Activity GalleryGrid
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_grid); // Χρήση του activity_gallery_grid για τη δημιουργία γραφικών
        theSharedPref(); // Συνάρτηση Ελέγχου προτίτερης (εαν δεν έχει αποσυνδεθεί) σύνδεσης χρήστη μέσω της εφαρμογής
        selectMLModels(); // Επιλογή μοντέλου Machine Learning για την Ταξινόμηση Φωτογραφιών
        //offlineMode();

        dummyHash.put("url","loading..."); // Χρήση placeholder Hashmap για τις τιμές url, label μέχρι την φόρτωση των πραγματικών για τα πεδία του FireStore
        dummyHash.put("label","loading");

        androidGridView = findViewById(R.id.gridview_android_example);  // Σύνδεση μεταβλητών με το xml αρχείο γραφικών
        searchField = findViewById(R.id.editTextTextPersonName);
        searchButton = findViewById(R.id.button2);

        initialReference = FirebaseStorage.getInstance().getReference(); // Μεταβλητή αναφοράς στο Firebase Storage

        checkifNewUser(); // Έλεγχος νέου χρήστη
        new updateImageViews().execute(); // Εμφάνιση Ταξινομημένων εικόνων συνδεδεμένου χρήστη

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchLabels(searchField.getText().toString()); // Αναζήτηση στο GridView
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAMERA) { // Ενέργεια έπειτα απο δημιουργία φωτογραφίας
            try {
                // Δημιουργία δύο αρχείων
                File fileStorage = new File(currentPhotoPath); // Αρχείο για μεταφόρτωση στο Firebase Storage
                File fileFireStore = new File(currentPhotoPath+1); // Αρχείο για μεταφόρτωση στο Server Ταξινόμησης
                try {
                    Bitmap bitmapStorage = getBitmapFormUri(GalleryGrid.this, photoURI, 1000,500); // Κλιμάκωση αρχείου Firebase Storage και επιστροφή σε bitmap
                    Bitmap bitmapFireStore = getBitmapFormUri(GalleryGrid.this, photoURI, 331,331); // Κλιμάκωση αρχείου Server Ταξινόμησης και επιστροφή σε bitmap
                    FileOutputStream out = new FileOutputStream(fileStorage); // Ροή εξόδου του Bitmap στο αρχείο του Firebase Storage
                    FileOutputStream out2 = new FileOutputStream(fileFireStore); // Ροη εξόδου του Bitmao στο αρχείο του Server Ταξινόμησης
                    bitmapStorage = fixImage(bitmapStorage); // Διόρθωση σε τυχόν θέμα γωνίας αποθήκευσης φωτογραφίας
                    bitmapStorage.compress(Bitmap.CompressFormat.JPEG, 25, out); // Αποθήκευση αλλαγών στο αρχείο του Firebase Storage

                    @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // Δημιουργία timestamp για την εικόνα
                    String genLabel = generateLabel(timeStamp,0); // Δημιουργία μοναδικού timestamp για την εικόνα ώστε να αποθηκευτεί μοναδικά στο FireStore αλλά και στο Firebase Storage
                    dummyHash.put("timestamp", genLabel); // Προσθήκη στο Hashmap
                    addToFirestore(db,genLabel,dummyHash); // Αποστολή στο FireStore


                    firebaseUpload fbupload = new firebaseUpload(Uri.fromFile(fileStorage), "loading...", genLabel, fileStorage, bitmapStorage); // Thread για upload εικόνας στο Firebase Storage
                    new Thread(fbupload).start(); // Εκκίνηση του Thread upload εικόνας στο Firebase Storage

                    if(offlineMode==0){
                        bitmapFireStore = Bitmap.createScaledBitmap(bitmapFireStore,224, 224,true); // Κλιμάκωση ξανά (ίσως αφαιρεθεί)
                        bitmapFireStore.compress(Bitmap.CompressFormat.JPEG, 100, out2); // Αποθήκευση αλλαγών στο αρχείο αποστολής στο Server Ταξινόμησης

                        MyRunnable theRunnable = new MyRunnable(modelLink, genLabel,fileFireStore,1231231, 0); // Thread για upload εικόνας στο Server Ταξινόμησης
                        new Thread(theRunnable).start(); // Εκκίνηση του Thread upload εικόνας στο Server Ταξινόμησης
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
                startActivity(new Intent(this, GalleryGrid.class)); // Σε περίπτωση προβλήματος επιστροφή στη βασική οθόνη της εφαρμογής (πλέγμα εικόνων)
            }
        }
        if (requestCode == PICK_IMAGE) { // Φόρτωση αποθηκευμένης εικόνας
            try {
                try{
                ClipData clipped = data.getClipData(); // Διαχωρισμός πολλαπλών uri εικόνων
                if(clipped.getItemCount() == 1){throw new Exception();} // Εαν είναι μία εικόνα δημιουργία exeption error για διαφιρετική διαχείριση
                    @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // Δημιουργία timestamp για τις εικόνες
                    long startTimeBatch = System.nanoTime();
                    List< File > FOTO = new ArrayList<>();
                    List< File > filesOne = new ArrayList<>();
                   ArrayList<String> genLabels = new ArrayList<>();
                    ArrayList<Integer> orderNums = new ArrayList<>();
                for (int i = 0; i < clipped.getItemCount(); i++) {
                    closeitAll2 theClose = new closeitAll2(clipped,timeStamp, startTimeBatch, i, FOTO, filesOne, genLabels, orderNums); // Thread για upload εικόνων
                    new Thread(theClose).start(); // Εκκίνηση Thread

                }
                }
                catch(Exception e){

                    Uri loadURI = data.getData(); // Τοποθεσία εικόνας στη συσκευή
                    File f = createImageFile(); // Δημιουργία αρχείου για εικόνα που επιλέχθηκε για ταξινόμηση

                    FileOutputStream out = new FileOutputStream(f); // Ροή αρχείου
                    Bitmap bitmap = getBitmapFormUri(GalleryGrid.this, loadURI, 1000,500); // Κλιμάκωση εικόνας και δημιουργία bitmap
                    bitmap = fixImage(bitmap); // Διόρθωση γωνίας αποθήκευσης εικόνας
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 25, out); // Συμπίεση εικόνας που επιλέχθηκε για ταξινόμηση

                    @SuppressLint("SimpleDateFormat") final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // Δημιουργία timestamp
                    String genLabel = generateLabel(timeStamp,0); // Δημιουργία label ια το FireStore
                    dummyHash.put("timestamp", genLabel); // Προσθήκη στο Hashmap
                    addToFirestore(db, genLabel, dummyHash); // Αποστολή στο FireStore

                    firebaseUpload fbupload = new firebaseUpload(Uri.fromFile(f), "loading...", genLabel, f, bitmap); // Thread για upload εικόνας στο Firebase Storage
                    new Thread(fbupload).start(); // Εκκίνηση Thread

                    if(offlineMode==0){
                        long startTimet = System.nanoTime();
                        File f2 = new File(currentPhotoPath+1); // Δημιουργία αρχείου για αποστολή σε Server Ταξινόμησης
                        FileOutputStream out2 = new FileOutputStream(f2); // Δημιουργία ροής για το αρχείο

                        Bitmap bitmap2 = getBitmapFormUri(GalleryGrid.this, loadURI, 331,331); // Κλιμάκωση εικόνας και επιστροφή σε bitmap
                        bitmap2 = fixImage(bitmap2); // Διόρθωση γωνίας αποθήκευσης εικόνας
                        bitmap2 = Bitmap.createScaledBitmap(bitmap2,331, 331,true); // Κλιμάκωση εικόνας ξανά (διόθωση)
                        bitmap2.compress(Bitmap.CompressFormat.JPEG, 35, out2); // Συμπίεση εικόνας

                        MyRunnable theRunnable = new MyRunnable(modelLink, genLabel,f2,1231231, 0); // Thread για upload εικόνας στο Server Ταξινόμησης
                        new Thread(theRunnable).start(); // Εκκίνηση του Thread upload εικόνας στο Server Ταξινόμησης

                        //multipleMLUpload mutlipleML = new multipleMLUpload(modelLink, genLabel,f2, startTimet, 0); // Thread για upload εικόνας στον server Ταξινόμησης
                        //new Thread(mutlipleML).start(); // Εκκίνηση Thread
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
                startActivity(new Intent(this, GalleryGrid.class)); // Επιστροφή στην κύρια οθόνη σε περίπτωση λάθους
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // Intent για λειτουργία φωτογραφίας
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile(); // Δημιουργία αρχείου
            } catch (IOException ex) {
                Log.d("PHOTOTAG", "OOPS SOmEthing Happened");
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.gallery_ai.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI); // Πέρασμα uri φωτογραφίας μέσω του intent
                startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA); // Εκκίνηση του Activity result
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // Δημιουργία timestamp
        String imageFileName = "JPEG_" + timeStamp + "_"; // Όνομα αρχείου βάσει του timestamp
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // Εύρεση εξωτερικής τοποθεσίας
        File image = File.createTempFile(   // Δημιουργία προσωρινού αρχείου
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image; // Επιστροφή αρχείου εικόνας
    }

    private File createImageFileMultiple(int label) throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // Ομοίως με την createImageFile() με τη δημιουργία πολλών αρχείων με timeStamp + αριθμός αρχείου
        String imageFileName = "JPEG_" + timeStamp + "_"+label;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image; // Επιστροφή αρχείου
    }

    private void chooseImage(){ // Επιλογή εικόνας - εικόνων
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    public void checkifNewUser(){ // Έλεγχος νέου χρήστη

        try{
            db.collection(userID); // Σε περίπτωση που υπάρχει δεν γίνεαι κάτι

        }catch (Exception e){
            db.collection(userID).document("thisEMptyDOc").set(dummyHash); // Εαν δεν υπάρχει δημιουργείται κενή καταχώριση ώστε να μην δημιουργείται error
        }
    }


    @SuppressLint("StaticFieldLeak")
    public class updateImageViews extends AsyncTask<Void, Void, Void> { // Ασύγχρονη ανανέωση εικόνων του GridView με τη βοήθεια του AsyncTask
        @Override
        protected Void doInBackground(Void... voids) {
            CollectionReference collRef = db.collection(userID); // Αναφορά στο Collection του FireStore με το αναγνωριστικό χρήστη
            collRef.orderBy("timestamp", Direction.DESCENDING).get(). // Ταξινόμηση collection σύμφωνα με το timeStamp
                    addOnCompleteListener(
                            new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        userLabels.clear(); // Αδειάζουμε τα Arraylist για την εμφάνιση της νέας λίστας εικόνων
                                        userUrls.clear();
                                        userTimestamps.clear();
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            userLabels.add(document.getData().get("label").toString()); // Προσθήκη στοιχείων εικόνων χρήστη στα ArrayList
                                            userUrls.add(document.getData().get("url").toString());
                                            userTimestamps.add(document.getData().get("timestamp").toString());
                                        }
                                        ImageAdapterGridView adapter = new ImageAdapterGridView(GalleryGrid.this, userUrls.size()); // Προσθήκη των εικόνων στο GridView
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
        private final Context mContext; // Το Context της δραστηριότητας
        private final int mCount; // Ο αριθμός των στοιχείων


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
            @SuppressLint({"ViewHolder", "InflateParams"}) final View myView= li.inflate(R.layout.row_data, null); // Δημιουργία τετραγώνου εμφάνισης εικόνας
            final ImageView img =  myView.findViewById(R.id.imagelayout); // Θέση Εικόνας
            TextView txt =  myView.findViewById(R.id.textlayout); // Θέση Label Ταξινόμησης
            txt.setText(userLabels.get(position)); // Label απο τα arraylist που παίρνουμε απο το FireStore

            Picasso.with(mContext).load(userUrls.get(position)).fit().centerCrop().into(img); // Εφαρμογή εικόνας στο τετράγωνο του GridView

            androidGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // Δημιουργία listener για κάθε τετράγωνο του GridView
                public void onItemClick(AdapterView<?> parent,
                                        View v, int position, long id) {
                    Toast.makeText(getBaseContext(), "Grid Item " + (position + 1) + " Selected", Toast.LENGTH_LONG).show();
                    Intent myIntent = new Intent(GalleryGrid.this, FullScreen.class); // Με το πάτημα κάθε τετραγώνου εμφανιζεται η αντίστοιχη εικόνα
                    myIntent.putExtra("tourl", userUrls.get(position)); // Πέρασμα τιμών url, label, timestamp στο επόμενο Activity
                    myIntent.putExtra("tolabel", userLabels.get(position));
                    myIntent.putExtra("totimestamp", userTimestamps.get(position));
                    startActivity(myIntent); // Έναρξη νέας δραστηριότητας
                    overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit); // Μετάβαση - Animation οθόνης
                }
            });
            return myView;
        }
    }

    public void addToFirestore(FirebaseFirestore collection, String label, Map<String, Object>  data){
        collection.collection(userID).document(label).set(data); // Προσθήκη στοιχείων εικόνας στο FireStore
    }
    public String generateLabel(String dummyValue, int count){
        return dummyValue+count; // TimeStamp + αριθμός
    }

    private void searchLabels(final String queue){ // Αναζήτηση στο GridView βάσει label
            CollectionReference collRef = db.collection(userID); // Αναφορά στο Collection του FireStore

            collRef.orderBy("timestamp", Direction.DESCENDING).get(). // Ταξινόμηση collection
                    addOnCompleteListener(
                            new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        userLabels.clear(); // Καθαρισμός των Arraylist
                                        userUrls.clear();
                                        userTimestamps.clear();
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            if (((String) document.getData().get("label")).contains(queue)) { // Εμφάνιση αποτελεσμάτων βάσει label
                                                userLabels.add((String) document.getData().get("label")); // Προσθήκη αποτελεσμάτων σε Arraylist
                                                userUrls.add(document.getData().get("url").toString());
                                                userTimestamps.add(document.getData().get("timestamp").toString());
                                            }
                                        }
                                        if(userLabels.size()!=0){
                                        ImageAdapterGridView adapter = new ImageAdapterGridView(GalleryGrid.this, userUrls.size()); // Εμφάνιση συνόλου αποτελεσμάτων στο GridView
                                        androidGridView.setAdapter(adapter);}
                                    } else {
                                        Log.d("TAG", "No such document");
                                    }
                                }
                            });
    }

    public void signOut(){
        FirebaseAuth.getInstance().signOut(); // Αποσύνδεση Χρήστη
        SharedPreferences saved_values = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = saved_values.edit();
        editor.remove("userID"); // Αφαίρεση αποθηκευμένων τιμών απο το SharedPreferences
        editor.commit();
        startActivity(new Intent(GalleryGrid.this, UserLogin.class)); // Επιστροφή στη δραστηριότητα κύριας οθόνης

    }

    public boolean onCreateOptionsMenu(Menu menu) { // Εφαρμογή επιλογών menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_options_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(MenuItem item) { // Επιλογές menu
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
    private int getCameraAngle(){ // Γωνία αποθήκευσης εικόνας απο τη συσκευή
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

    public Bitmap rotateBitmap(Bitmap original, float degrees) { // Περιστροφή Bitmap
        Matrix matrix = new Matrix();
        matrix.preRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        original.recycle();
        return rotatedBitmap;
    }



    private void theSharedPref(){
        SharedPreferences sp = getSharedPreferences("lastUser", MODE_PRIVATE); // Αποθήκευση σύνδεσης χρήστη με το αναγνωριστικό χρήστη
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("userID", userID);
        editor.apply();
    }

    private void selectMLModels(){
        Spinner s = findViewById(R.id.spinner); // Επιλογέας για μοντέλα μηχανικής μάθησης
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, names); // Προσθήκη μοντέλων στον adapter
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);

        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                modelLink = theAddress + arraySpinner[position]; // δημιουργία τελικής διεύθυνσης στο Server Ταξινόμησης
            } // to close the onItemSelected
            public void onNothingSelected(AdapterView<?> parent)
            {
                modelLink = theAddress + names[s.getSelectedItemPosition()]; // Στην αρχή επιλογή πρώτου
            }
        });
    }

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

            AndroidNetworking.upload(url) // Δημιουργία POST Request προς το επιθυμητό URL του Server Ταξινόμησης
                    .addMultipartFile("image",file) // Το αρχείο είναι multipart καθώς αποστέονται ολόκληρα τα αρχεία εικόνων
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
                                JSONArray maxResult = response.getJSONArray("labels"); // Με το που επιστρέφει το response απο τον server με την ταξινόμηση των εικόνων
                                //JSONObject maxResult = (JSONObject) response.getJSONArray("labels").get(0);
                                db.collection(userID).document(documentID).update("label", maxResult.get(0)) // Γίνεται update των στοιχείων της φωτογραφίας στο FireStore
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d("TAG", "DocumentSnapshot successfully updated!");
                                                new updateImageViews().execute(); // Με την επιτυχία αναβάθμισης του label της εικόνας, ανανεώνεται και η λίστα εικόνων στο GridView της κύριας οθόνης
                                                //long MethodeDuration = 0;
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
            final StorageReference image = initialReference.child("dogs/"+generatedLabel); // Μεταβλητή αναφοράς στο Firebase Storage
            image.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() { // Μεταφόρτωση εικόνας στο Firebase Storage
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() { // Απόκτηση url εικόνας απο το Firebase Storage
                        @Override
                        public void onSuccess(Uri uri) {

                            new updateImageViews().execute(); // Ανανέωση GridView κύριας οθόνης με την επιτυχία upload της εικόνας
                            db.collection(userID).document(genLabel).update("url",uri.toString()) // Ενημέρωση με το url της εικόνας στο FireStore
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            new updateImageViews().execute(); // Επανάληψη ανανέωσης εικόνων
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                        }
                                    });
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Bitmap fixImage(Bitmap bitmap){
        int cameraAngle = getCameraAngle(); // Εύρεση γωνίας εικόνας
        String manufacturer = Build.MANUFACTURER;
        if(manufacturer.equals("XIAOMI")||manufacturer.equals("Xiaomi")||manufacturer.equals("Samsung")||manufacturer.equals("SAMSUNG")) { // Διαφορετικές γωνίες κάποιων κατασκευαστών
            if(cameraAngle == 180){bitmap = rotateBitmap(bitmap,180);} // Αντίστοιχη περιστροφή
            else if(cameraAngle == 270){bitmap = rotateBitmap(bitmap,90);}

    }return bitmap;
}

    private class closeitAll2 implements Runnable {
        private volatile ClipData mItem;
        private volatile int i;
        private volatile String timeStamp;
        private volatile long startTimeBatch;
        private volatile  List< File > FOTO;
        private volatile List< File > filesOne;
        private volatile ArrayList<String> genLabels;
        private volatile ArrayList<Integer> orderNums;


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

            File f = createImageFileMultiple(i); // Δημιουργία πολλαπλών αρχείων με timestamp + αύξοντα αριθμό το πρώτο αρχείο για το FireBase Storage
            File f2 = new File(currentPhotoPath+i); // Το δεύτερο για το Server Ταξινόμησης
            Bitmap bitmap2 = getBitmapFormUri(GalleryGrid.this, mItem.getItemAt(i).getUri(),224,224); // Κλιμάκωση εικόνων που αποστέλονται στον Server Ταξινόμησης
            FileOutputStream out2 = new FileOutputStream(f2); // Δημιουργία ροής αρχείο
            bitmap2 = fixImage(bitmap2); // Διόρθωση γωνίας αποθήκευσης εικόνας
            if(offlineMode==0) {
                bitmap2 = Bitmap.createScaledBitmap(bitmap2, 331, 331, true); // Κλιμάκωση ξανά
                bitmap2.compress(Bitmap.CompressFormat.JPEG, 25, out2); // Συμπίεση εικόνας Server Ταξινόμησης
                String genLabel = generateLabel(timeStamp,i); // Δημουργία timestamp label
                dummyHash.put("timestamp", genLabel);
                addToFirestore(db,genLabel,dummyHash); // Προσθήκη στο FireStore placeholder τιμής

                FOTO.add(f2); // Δημιουργία λίστα αρχείων για την αποστολή στο Server Ταξινόμησης
                filesOne.add(f);
                genLabels.add(genLabel);
                orderNums.add(i);
                bitmap2.recycle();

            }

            if(FOTO.size()== mItem.getItemCount()){ // Ταξινόμηση νέων εικόνων απο τα διαφορετικά Threads βάσει του timestamp
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
            AndroidNetworking.upload(modelLink) // Αποστολή εικόνων στο Server Ταξινόμησης
                    .setOkHttpClient(okHttpClient)
                        .addMultipartFileList("image", FOTO) // Αποστολή αρχείων εικόνων με POST Request
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

                                // do anything with response
                                try {
                                    double theAnswer = (double) (System.nanoTime()/1000000000.00);
                                    JSONArray maxResult = response.getJSONArray("labels");
                                    JSONArray times = response.getJSONArray("times");
                                    double addedTime = (double) (times.get(0)) + (double) (times.get(1));

                                    long startingUpdateTime = System.nanoTime();
                                    for(int i = 0; i< maxResult.length(); i++){ // Ανανέωση label εικόνων στο FireStore
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
                                    new updateImageViews().execute(); // Ανανέωση GridView κ΄ρυιας οθόνης

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public void onError(ANError error) {
                                // handle error
                            }
                        });

            for(int j=0;j<genLabels.size();j++){
                Bitmap bitmap = getBitmapFormUri(GalleryGrid.this, mItem.getItemAt(j).getUri(), 1000,500);
                FileOutputStream out = new FileOutputStream(filesOne.get(j)); // Κλιμάκωση εικόνας

                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, out);
                firebaseUpload fbupload = new firebaseUpload(Uri.fromFile(filesOne.get(j)), "loading...", genLabels.get(j), filesOne.get(j), bitmap); // Αποστολή εικόνων στο Firebase Storage
                new Thread(fbupload).start();
            }
                //FOTO.clear();
                //filesOne.clear();
                //genLabels.clear();
                //MyRunnable theRunnable = new MyRunnable(modelLink, genLabel, f2, startTimeBatch, i);
                //new Thread(theRunnable).start();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void run(){
            try {
                doSmth(mItem, timeStamp,startTimeBatch, i, FOTO, filesOne, genLabels, orderNums);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap getBitmapFormUri(Activity ac, Uri uri, float hh, float ww) throws FileNotFoundException, IOException { // Κλιμάκωση εικόνων
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
    public static Bitmap compressImage(Bitmap image) { // Περαιτέρω συμπίεση εικόνων
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





