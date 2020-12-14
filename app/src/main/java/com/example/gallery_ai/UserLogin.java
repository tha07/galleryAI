package com.example.gallery_ai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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

import java.util.HashMap;
import java.util.Map;

public class UserLogin extends AppCompatActivity {
    private EditText emailField, passField;
    private Button loginButton;

    public static Map<String, Object> userCredentials = new HashMap<>();
    final FirebaseAuth fAuth = FirebaseAuth.getInstance();

    String email,password;
    public static String userID,userEmail;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if(userID!=null){
            userID = fAuth.getCurrentUser().getUid();
            startActivity(new Intent(UserLogin.this, GalleryGrid.class));
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        // Initialize Firebase Auth
        emailField = findViewById(R.id.editTextTextEmailAddress);
        passField = findViewById(R.id.editTextTextPassword);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Check for valid fields
                    getValues();
                    loginUser();
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
                    FirebaseUser user = fAuth.getCurrentUser();
                    userID = fAuth.getCurrentUser().getUid();
                    userEmail = fAuth.getCurrentUser().getEmail();

                    if(user!=null){
                        userCredentials.put(userID,userEmail);
                        startActivity(new Intent(UserLogin.this,  GalleryGrid.class));
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Δεν βρέθηκε χρήστης με τα στοιχεία σας. Δοκιμάστε ξανά", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}