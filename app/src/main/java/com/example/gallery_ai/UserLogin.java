package com.example.gallery_ai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

public class UserLogin extends AppCompatActivity {
    private EditText emailField, passField;
    private Button loginButton;
    public static Map<String, Object> userCredentials = new HashMap<>();
    final FirebaseAuth fAuth = FirebaseAuth.getInstance();
    String email,password;
    public static String userID, userEmail;

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        // Initialize Firebase Auth
        emailField = findViewById(R.id.editTextTextEmailAddress); // Πεδία email, password, login button
        passField = findViewById(R.id.editTextTextPassword);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // listener
                //Check for valid fields
                    getValues(); // Παίρνω τιμές απο πεδία
                    loginUser(); // Σύνδεση χρήστη στην εφαρμογή + μόνιμη σύνδεση
                }
        });
    }

    private void getValues() {
        email = emailField.getText().toString();
        password = passField.getText().toString();
    }


    private void loginUser() {
        fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    FirebaseUser user = fAuth.getCurrentUser(); // Authentication χρήστη με το Firebase
                    userID = fAuth.getCurrentUser().getUid(); // Μοναδικό ID χρήστη απο το Firebase
                    userEmail = fAuth.getCurrentUser().getEmail();

                    SharedPreferences saved_values = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = saved_values.edit();
                    editor.putString("userID", userID); // Αποθήκευση του UserID για ταυτοποίηση και χρήση ως αναγνωριστικό για τις εικόνες
                    editor.commit(); // Αποθήκευση

                    if(user!=null){
                        userCredentials.put(userID,userEmail); // Αποθήκευση credentials σε Hashmap
                        startActivity(new Intent(UserLogin.this,  GalleryGrid.class)); // Συνέχεια στην κύρια οθόνη
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Δεν βρέθηκε χρήστης με τα στοιχεία σας. Δοκιμάστε ξανά", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}