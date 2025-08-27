package com.example.realestate.activities;

import static android.os.Build.VERSION_CODES_FULL.TIRAMISU;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityProfileEditBinding;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class ProfileEditActivity extends AppCompatActivity {

    private ActivityProfileEditBinding binding;

    private static final String TAG = "PROFILE_EDIT_TAG";

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private String myUserType = "";
    private Uri imageUri=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth = FirebaseAuth.getInstance();

        loadMyInfo();

        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        binding.profileImagePickFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imagePickDialog();
            }
        });
        binding.updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private String name="";
    private String dob="";
    private String email="";
    private String phoneCode ="";
    private String phoneNumber="";
    private void validateData(){
        //input data
        name = binding.nameEt.getText().toString().trim();
        dob = binding.dobEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        phoneCode = binding.countryCodePicker.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();

        if (imageUri == null){
            //no image to upload to storage, just update db
            updateProfileDb(null);
        } else {
            //image need to upload to storage, first upload image then update db
            uploadProfileImageStorage();
        }
    }

    private void uploadProfileImageStorage(){
        Log.d(TAG, "uploadProfileImageStorage: ");
        //show progress
        progressDialog.setMessage("Uploading user profile image...");
        progressDialog.show();

        String filePathAndName = "UserImages/" + firebaseAuth.getUid();
        StorageReference ref = FirebaseStorage.getInstance().getReference(filePathAndName);

        ref.putFile(imageUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        //Progress from 0 to 100
                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        Log.d(TAG, "onSuccess: Progress: " + progress);
                        //show progress to progress dialog
                        progressDialog.setMessage("Uploading profile image. Progress: " + (int)progress + "%");
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //Image uploaded successfully
                        Log.d(TAG, "onSuccess: Uploaded");
                        //To get url of uploaded image
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String uploadedImageUrl = uriTask.getResult().toString();
                        if (uriTask.isSuccessful()){
                            //if we get successfully then upload to db
                            updateProfileDb(uploadedImageUrl);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to upload image
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(ProfileEditActivity.this, "Failed to upload profile image due to " + e.getMessage());
                    }
                });
    }

    private void updateProfileDb(String imageUrl){
        //show progress
        progressDialog.setMessage("Updating user info...");
        progressDialog.show();
        //setup data in hashmap to update to firebase db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("name", "" + name);
        hashMap.put("dob", "" + dob);
        if (imageUrl != null){
            //update profileImageUrl in db only if uploaded image url is not null
            hashMap.put("profileImageUrl", "" + imageUrl);
        }
        //if user type is Phone then allow to update email otherwise (in case of Google or Email) allow to update Phone
        if (myUserType.equals(MyUtils.USER_TYPE_EMAIL) || myUserType.equals(MyUtils.USER_TYPE_GOOGLE)){
            //user type is google/email, allow to update phone number not email
            hashMap.put("phoneCode", "" + phoneCode);
            hashMap.put("phoneNumber", "" + phoneNumber);
        } else if (myUserType.equals(MyUtils.USER_TYPE_PHONE)){
            //user type is phone, allow to update email, not phone number
            hashMap.put("email", "" + email);
        }
        //Database reference of user to update info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid())
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //updated successfully
                        Log.d(TAG, "onSuccess: Info updated");
                        progressDialog.dismiss();
                        MyUtils.toast(ProfileEditActivity.this, "Profile updated...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to update
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(ProfileEditActivity.this, "Failed to update due to " + e.getMessage());
                    }
                });
    }

    private void imagePickDialog() {
        //init popup menu param 1 is context and param 2 is the UI View (profileImagePickFab) to above/below we need to show popup menu
        PopupMenu popupMenu = new PopupMenu(this, binding.profileImagePickFab);
        //add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID, Param#3 is OrderID, Param#4 is Menu Item Title
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");
        //Show Popup Menu
        popupMenu.show();
        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //get the id of the menu item clicked
                int itemId = menuItem.getItemId();
                //check which menu item is clicked based on itemId we got
                if (itemId == 1) {
                    Log.d(TAG,"onMenuItemClick:Camera Clicked,checking permission");
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)
                    {
                        requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA});
                    }else{
                        requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE});
                    }
                    //Camera is clicked we need to check if we have permission of Camera, Storage before launching Camera to Capture Image
                } else if (itemId == 2) {
                    //Gallery is clicked, We don't need any permission to launch Gallery
                    Log.d(TAG, "onMenuItemClick: Gallery Clicked......");
                    pickImageGallery();
                }
                return true;
            }
        });
    }

    //... (previous code)
    private final ActivityResultLauncher<String[]> requestCameraPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: " + result.toString());
                    //Check if all permissions are granted
                    boolean areAllGranted = true;
                    for (Boolean isGranted : result.values()){
                        areAllGranted = areAllGranted && isGranted;
                    }
                    if (areAllGranted){
                        //All Permissions Camera, Storage are granted, we can now launch camera to capture image
                        pickImageCamera();
                    } else {
                        //Camera or Storage or Both permissions are denied, Can't launch camera to capture image
                        Log.d(TAG, "onActivityResult: All or either one permission denied...");
                        MyUtils.toast(ProfileEditActivity.this, "Camera or Storage or both permissions denied...");
                    }
                }
            }
    );

    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "temp_image");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "temp_image_description");
        //store the camera image in imageUri variable
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        //Intent to launch camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //Check if Image is captured or not
                    if (result.getResultCode() == Activity.RESULT_OK){
                        //Image Captured, we have image in imageUri as assigned in pickImageCamera()
                        Log.d(TAG, "onActivityResult: Image Picked: " + imageUri);
                        //set to profileIv
                        try {
                            Glide.with(ProfileEditActivity.this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.person_black)
                                    .into(binding.profileIv);
                        } catch (Exception e){
                            Log.e(TAG, "onActivityResult: ", e);
                        }
                    } else {
                        //Cancelled
                        Log.d(TAG, "onActivityResult: Cancelled...");
                        MyUtils.toast(ProfileEditActivity.this, "Cancelled...");
                    }
                }
            }
    );

    private void pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ");
        //Intent to launch Image Picker e.g. Gallery
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //Check if image is picked or not
                    if (result.getResultCode() == Activity.RESULT_OK){
                        //Image Picked from Gallery, get data/intent from ActivityResult
                        Intent data = result.getData();
                        //get uri of image picked
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: Image Picked From Gallery: " + imageUri);
                        //set to profileIv
                        try {
                            Glide.with(ProfileEditActivity.this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.person_black)
                                    .into(binding.profileIv);
                        } catch (Exception e) {
                            Log.e(TAG, "onActivityResult: ", e);
                        }
                    } else {
                        //Cancelled
                        Log.d(TAG, "onActivityResult: Cancelled...");
                        MyUtils.toast(ProfileEditActivity.this, "Cancelled...");
                    }
                }
            }
    );
// ... (rest of the code)
    //... (previous code)





    private void loadMyInfo() {
        Log.d(TAG, "loadMyInfo: ");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Get user info
                        String dob = "" + snapshot.child("dob").getValue();
                        String email = "" + snapshot.child("email").getValue();
                        String name = "" + snapshot.child("name").getValue();
                        String phoneCode = "" + snapshot.child("phoneCode").getValue();
                        String phoneNumber = "" + snapshot.child("phoneNumber").getValue();
                        String profileImageUrl = "" + snapshot.child("profileImageUrl").getValue();
                        String timestamp = "" + snapshot.child("timestamp").getValue();
                        myUserType = "" + snapshot.child("userType").getValue();

                        String phone = phoneCode + phoneNumber;

                        // Check user type and disable fields accordingly
                        if (myUserType.equals(MyUtils.USER_TYPE_EMAIL) || myUserType.equals(MyUtils.USER_TYPE_GOOGLE)) {
                            // User type is Email or Google. Don't allow to edit email.
                            binding.emailTil.setEnabled(false);
                            binding.emailEt.setEnabled(false);
                        } else {
                            // User type is Phone. Don't allow to edit phone number.
                            binding.phoneNumberTil.setEnabled(false);
                            binding.phoneNumberEt.setEnabled(false);
                            binding.countryCodePicker.setEnabled(false);
                        }

                        // Set data to UI
                        binding.emailEt.setText(email);
                        binding.dobEt.setText(dob);
                        binding.nameEt.setText(name);
                        binding.phoneNumberEt.setText(phoneNumber);

                        try {
                            int phoneCodeInt = Integer.parseInt(phoneCode.replace("+", ""));
                            binding.countryCodePicker.setCountryForPhoneCode(phoneCodeInt);
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: ", e);
                        }

                        // Set profile image
                        try {
                            Glide.with(ProfileEditActivity.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.person_black)
                                    .into(binding.profileIv);
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: ", e);
                        }

                        // Set full name in toolbar
                        binding.fullNameTv.setText(name);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

}