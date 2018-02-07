package com.example.asus.onlinecanteen;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.asus.onlinecanteen.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = RegisterActivity.class.getSimpleName();
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 999;

    ImageView imageView;
    Button button, submitbtn;
    private static final int PICK_IMAGE = 100;
    Uri imageUri;
    String profPicUrl;
    FirebaseAuth mAuth;
    FirebaseUser user;
    DatabaseReference userReferences;

    //EditText
    EditText usernameET, passwordET, emailET, nimET, phoneET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        imageView = findViewById(R.id.imageinput);
        button =  findViewById(R.id.browse);
        submitbtn = findViewById(R.id.registerbtn);
        usernameET = findViewById(R.id.usrnamefill);
        passwordET = findViewById(R.id.passwordfill);
        emailET = findViewById(R.id.emailfill);
        nimET = findViewById(R.id.nimfill);
        phoneET = findViewById(R.id.phonefill);


        //Browse Image in Gallery & set as Profile Picture
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             openGallery();
            }
        });

        //Submit data for Sign Up & Upload to Storage
        submitbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitData();
            }
        });
    }

    private void openGallery(){
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == PICK_IMAGE){
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
    }

    //To submit data
    private void submitData() {

        if(!validateRegisterInfo()) {
            // Field is not filled
            return;
        }

        mAuth.createUserWithEmailAndPassword(emailET.getText().toString(),passwordET.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            if(imageUri != null) {
                                if(ContextCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    requestReadStoragePermission();
                                } else {
                                    addAdditionalUserInformation();
                                }
                            } else {
                                backToLoginScreen();
                            }
                        } else {
                            emailET.setText("");
                            emailET.setError("Email is registered");
                            passwordET.setText("");
                        }
                    }
                });
    }

    private void addAdditionalUserInformation() {
        mAuth.signInWithEmailAndPassword(emailET.getText().toString(), passwordET.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            user = mAuth.getCurrentUser();

                            UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder();
                            profileBuilder.setDisplayName(usernameET.getText().toString());

                            UserProfileChangeRequest profileChangeRequest = profileBuilder.build();
                            user.updateProfile(profileChangeRequest);

                            String uid = user.getUid();
                            User userInfo = new User(nimET.getText().toString(), "USER", phoneET.getText().toString());
                            userReferences = FirebaseDatabase.getInstance().getReference("users").child(uid);
                            userReferences.setValue(userInfo);

                            if (user != null && imageUri != null &&
                                    ContextCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                uploadImage();
                            } else backToLoginScreen();

                        } else {
                            Toast.makeText(RegisterActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //To upload image
    private void uploadImage() {
        Log.d(TAG, "Uploading...");
        StorageReference profileImageRef = FirebaseStorage.getInstance().getReference("profilepics/"+System.currentTimeMillis()+".jpg");
        if (imageUri!=null){
            profileImageRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    profPicUrl = taskSnapshot.getDownloadUrl().toString();
                    Log.d(TAG, "Success in uploading");
                    UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder();
                    if(profPicUrl != null) {
                        Log.d(TAG, "Photo is taken");
                        profileBuilder.setPhotoUri(Uri.parse(profPicUrl));
                    }
                    UserProfileChangeRequest profileChangeRequest = profileBuilder.build();
                    user.updateProfile(profileChangeRequest);

                    backToLoginScreen();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(),"Image failed to upload",Toast.LENGTH_LONG).show();
                    backToLoginScreen();
                }
            });
        }
    }

    private boolean validateRegisterInfo() {
        boolean valid = true;

        String username = usernameET.getText().toString();
        if(TextUtils.isEmpty(username)) {
            usernameET.setError("Username required");
            valid = false;
        } else {
            usernameET.setError(null);
        }

        String password = passwordET.getText().toString();
        if(TextUtils.isEmpty(password)) {
            passwordET.setError("Password required");
            valid = false;
        } else {
            passwordET.setError(null);
        }

        String email = emailET.getText().toString();
        if(TextUtils.isEmpty(email)) {
            emailET.setError("Email required");
            valid = false;
        } else {
            emailET.setError(null);
        }

        String nim = nimET.getText().toString();
        if(TextUtils.isEmpty(nim)) {
            nimET.setError("Email required");
            valid = false;
        } else {
            nimET.setError(null);
        }

        String phone = phoneET.getText().toString();
        if(TextUtils.isEmpty(phone)) {
            phoneET.setError("Email required");
            valid = false;
        } else {
            phoneET.setError(null);
        }

        return valid;
    }

    //DRAFT
    //private void addUsers() {
       // User user = new User("Jessica","00000013452", "00000013452", "jessicaseaan@gmail.com", "A", null ,"081511030993" );
       // databaseUsers.push().setValue(user);
    //}

    private void requestReadStoragePermission() {
        ActivityCompat.requestPermissions(RegisterActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addAdditionalUserInformation();
            }
        }
    }

    private void backToLoginScreen() {
        if(mAuth != null) {
            mAuth.signOut();
        }
        // GO TO LOGIN PAGE - after success
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
