package com.example.realestate.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.realestate.AdapterImagePicked;
import com.example.realestate.ModelImagePicked;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityPostAddBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PostAddActivity extends AppCompatActivity {

    //View Binding
    private ActivityPostAddBinding binding;

    //TAG for logs in logcat
    private static final String TAG = "POST_ADD_TAG";
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    //Image Uri to hold uri of the image (picked/captured using Gallery/Camera) to add in Ad Images List
    private Uri imageUri = null;



    //Array Adapter to set to AutoCompleteTextView, so user can select subcategory base on category
    private ArrayAdapter<String> adapterPropertySubcategory;

    //list of images (picked/captured using Gallery/Camera or from internet)
    private ArrayList<ModelImagePicked> imagePickedArrayList;
    //Adapter to show images picked/taken from Gallery/Camera or from Internet
    private AdapterImagePicked adapterImagePicked;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //init view binding... activity_property_add.xml = ActivityPropertyAddBinding
        binding = ActivityPostAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please wait....!");
        progressDialog.setCanceledOnTouchOutside(false);

        //Setup and set the property area size unit adapter to the Property Area Unit Filed i.e. areaSizeUnitAct
        ArrayAdapter<String> adapterAreaSize = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, MyUtils.propertyAreaSizeUnit);
        binding.areaSizeUnitAct.setAdapter(adapterAreaSize);

        imagePickedArrayList = new ArrayList<>();
        loadImages();
        propertyCategoryHomes();
        //handle propertyCategoryTabLayout change listener, Choose Category
        binding.propertyCategoryTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //get selected category
                int position = tab.getPosition();

                if (position == 0) {
                    //Homes Tab clicked: Prepare adapter with categories related to Homes
                    category = MyUtils.propertyTypes[0];
                    propertyCategoryHomes();
                } else if (position == 1) {
                    //Plots Tab clicked: Prepare adapter with categories related to Plots
                    category = MyUtils.propertyTypes[1];
                    propertyCategoryPlots();
                } else if (position == 2) {
                    //Commercial Tab clicked: Prepare adapter with categories related to Commercial
                    category = MyUtils.propertyTypes[2];
                    propertyCategoryCommercial();
                }

                Log.d(TAG, "onTabSelected: category: " + category);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // Set a listener for the RadioGroup
        binding.purposeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Find the selected RadioButton by checkedId
                RadioButton selectedRadioButton = findViewById(checkedId);
                // Get the text of the selected RadioButton e.g. Sell/Rent
                purpose = selectedRadioButton.getText().toString();
                // show in logs
                Log.d(TAG, "onCheckedChanged: purpose: " + purpose);
            }
        });

        //handle pickImageTv click, show image add options (Gallery/Camera)
        binding.pickImagesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickOptions();
            }
        });

        binding.locationAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PostAddActivity.this, LocationPickerActivity.class);
                locationPickerActivityResultLauncher.launch(intent);
            }
        });

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private ActivityResultLauncher<Intent> locationPickerActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: result:"+result);
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            latitude = data.getDoubleExtra("latitude", 0);
                            longitude = data.getDoubleExtra("longitude", 0);
                            address = data.getStringExtra("address");
                            city = data.getStringExtra("city");
                            country = data.getStringExtra("country");

                            Log.d(TAG, "onActivityResult: latitude: " + latitude);
                            Log.d(TAG, "onActivityResult: longitude: " + longitude);
                            Log.d(TAG, "onActivityResult: address: " + address);
                            Log.d(TAG, "onActivityResult: city: " + city);
                            Log.d(TAG, "onActivityResult: country: " + country);

                            binding.locationAct.setText(address);
                        }
                    }
                }
            }
    );

    private void propertyCategoryHomes() {
        //In case of category Homes we will show
        binding.floorsTil.setVisibility(View.VISIBLE);
        binding.bedroomsTil.setVisibility(View.VISIBLE);
        binding.bathRoomsTil.setVisibility(View.VISIBLE);

        //Array Adapter to set to AutoCompleteTextView, so user can select subcategory base on category
        adapterPropertySubcategory = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, MyUtils.propertyTypesHomes);
        //set adapter to propertySubcategoryAct
        binding.propertySubCategoryAct.setAdapter(adapterPropertySubcategory);
        //Category changed, reset subcategory
        binding.propertySubCategoryAct.setText("");
    }

    private void propertyCategoryPlots() {
        //In case of category Plots we will hide
        binding.floorsTil.setVisibility(View.GONE);
        binding.bedroomsTil.setVisibility(View.GONE);
        binding.bathRoomsTil.setVisibility(View.GONE);

        //Array Adapter to set to AutoCompleteTextView, so user can select subcategory base on category
        adapterPropertySubcategory = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, MyUtils.propertyTypesPlots);
        //set adapter to propertySubcategoryAct
        binding.propertySubCategoryAct.setAdapter(adapterPropertySubcategory);
        //Category changed, reset subcategory
        binding.propertySubCategoryAct.setText("");
    }

    private void propertyCategoryCommercial() {
        //In case of category Commercial we will show/hide
        binding.floorsTil.setVisibility(View.VISIBLE);
        binding.bedroomsTil.setVisibility(View.GONE);
        binding.bathRoomsTil.setVisibility(View.GONE);

        //Array Adapter to set to AutoCompleteTextView, so user can select subcategory base on category
        adapterPropertySubcategory = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, MyUtils.propertyTypesCommercial);
        //set adapter to propertySubcategoryAct
        binding.propertySubCategoryAct.setAdapter(adapterPropertySubcategory);
        //Category changed, reset subcategory
        binding.propertySubCategoryAct.setText("");
    }

    private void loadImages() {
        Log.d(TAG, "loadImages: ");
        //init setup adapterImagesPicked to set it Recyclerview i.e. imagesRv. Param 1 is Context, Param 2 is Images List to show in RecyclerView
        adapterImagePicked = new AdapterImagePicked(this, imagePickedArrayList);
        //set adapter to imagesRv
        binding.imagesRv.setAdapter(adapterImagePicked);
    }
    private void showImagePickOptions() {
        Log.d(TAG, "showImagePickOptions: ");
        //init the PopupMenu. Param 1 is context. Param 2 is Anchor view for this popup. The popup will appear below the anchor if there is room, or above it if there is not.
        PopupMenu popupMenu = new PopupMenu(this, binding.pickImagesTv);
        //add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID, Param#3 is OrderID, Param#4 is Menu Item Title
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");
        //Show Popup Menu
        popupMenu.show();
        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //get the id of the item clicked in popup menu
                int itemId = item.getItemId();
                //check which item id is clicked from popup menu. 1=Camera, 2=Gallery as we defined
                if (itemId == 1) {
                    //Camera is clicked we need to check if we have permission of Camera, Storage before launching Camera to Capture image
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                        //Device version is TIRAMISU or above. We only need Camera permission
                        String[] permissions = new String[]{Manifest.permission.CAMERA};
                        requestCameraPermissions.launch(permissions);
                    } else {
                        //Device version is below TIRAMISU. We need Camera & Storage permissions
                        String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestCameraPermissions.launch(permissions);
                    }
                } else if (itemId == 2) {
                    //Gallery is clicked we need to check if we have permission of Storage before launching Gallery to Pick image
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                        //Device version is TIRAMISU or above. We don't need Storage permission to launch Gallery
                        pickImageGallery();
                    } else {
                        //Device version is below TIRAMISU. We need Storage permission to launch Gallery
                        String storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                        requestStoragePermission.launch(storagePermission);
                    }
                }
                return false;
            }
        });
    }
    private ActivityResultLauncher<String> requestStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG, "onActivityResult: isGranted: " + isGranted);
                    //Let's check if permission is granted or not
                    if (isGranted) {
                        //Storage Permission granted, we can now launch gallery to pick image
                        pickImageGallery();
                    } else {
                        //Storage Permission denied, we can't launch gallery to pick image
                        MyUtils.toast(PostAddActivity.this, "Storage permission denied!");
                    }
                }
            }
    );

    private ActivityResultLauncher<String[]> requestCameraPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: result: " + result);
                    //let's check if permissions are granted or not
                    boolean areAllGranted = true;
                    for (Boolean isGranted : result.values()) {
                        //check if any permission is not granted
                        areAllGranted = areAllGranted && isGranted;
                    }

                    if (areAllGranted) {
                        //All Permissions Camera, Storage are granted, we can now launch camera to capture image
                        pickImageCamera();
                    } else {
                        //Camera or Storage or Both permissions are denied, Can't launch camera to capture image
                        MyUtils.toast(PostAddActivity.this, "Camera or Storage or both permissions denied!");
                    }

                }
            }
    );
    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");
        //Intent to launch Image Picker e.g. Gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        //We only want to pick images
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");
                    //Check if image is picked or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //get data from result param
                        Intent data = result.getData();
                        //get uri of image picked
                        if (data != null && data.getData() != null) {
                            imageUri = data.getData();
                            Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                            // Example: show selected image in ImageView
                            // binding.selectedImageIv.setImageURI(imageUri);
                        } else {
                            MyUtils.toast(PostAddActivity.this, "No image selected!");
                        }
                        String timestamp = "" + MyUtils.timestamp();
                        ModelImagePicked modelImagePicked = new ModelImagePicked(timestamp, imageUri, null, false);
                        imagePickedArrayList.add(modelImagePicked);
                        //reload the images
                        loadImages();

                    } else {
                        //Cancelled
                        MyUtils.toast(PostAddActivity.this, "Cancelled!");
                    }
                }
            }
    );

    private void pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ");
        //Setup Content values, MediaStore to capture high quality image using camera intent
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMP_TITLE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "TEMP_DESCRIPTION");
        //Uri of the image to be captured from camera
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        //Intent to launch camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");
                    //Check if image is captured or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (imageUri != null) {
                            Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                            // binding.selectedImageIv.setImageURI(imageUri);
                        } else {
                            MyUtils.toast(PostAddActivity.this, "Image capture failed!");
                        }
                        String timestamp = "" + MyUtils.timestamp();
                        ModelImagePicked modelImagePicked = new ModelImagePicked(timestamp, imageUri, null, false);
                        imagePickedArrayList.add(modelImagePicked);
                        //reload the images
                        loadImages();
                    } else {
                        MyUtils.toast(PostAddActivity.this, "Cancelled!");
                    }

                }
            }
    );
    private String category = MyUtils.propertyTypes[0];
    private String purpose = MyUtils.PROPERTY_PURPOSE_SELL;

    private String subcategory= "";
    private String floors = ""; // no usages
    private String bedRooms = ""; // no usages
    private String bathRooms = ""; // no usages
    private String areaSize = ""; // no usages
    private String areaSizeUnit = ""; // no usages
    private String price = ""; // no usages
    private String title = ""; // no usages
    private String description = ""; // no usages
    private String email = ""; // no usages
    private String phoneCode = ""; // no usages
    private String phoneNumber = ""; // no usages
    private String country = ""; // no usages
    private String city = ""; // no usages
    private String address = ""; // no usages
    private double latitude = 0; // no usages
    private double longitude = 0; // no usages

    private void validateData() {
        Log.d(TAG, "validateData: ");

        subcategory=binding.propertySubCategoryAct.getText().toString().trim();
        floors=binding.floorsEt.getText().toString().trim();
        bedRooms=binding.bathRoomsEt.getText().toString().trim();
        bathRooms=binding.bathRoomsEt.getText().toString().trim();
        areaSize=binding.areaSizeEt.getText().toString().trim();
        areaSizeUnit=binding.areaSizeUnitAct.getText().toString().trim();
        address=binding.locationAct.getText().toString().trim();
        price=binding.priceEt.getText().toString().trim();
        title=binding.titleEt.getText().toString().trim();
        description=binding.descriptionEt.getText().toString().trim();
        email=binding.emailEt.getText().toString().trim();
        phoneCode=binding.phoneCodeTil.getSelectedCountryCodeWithPlus();
        phoneNumber=binding.phoneNumberEt.getText().toString().trim();
        //validate data
        if (subcategory.isEmpty()){
            //no property subcategory selected
            binding.propertySubCategoryAct.setError("Choose Subcategory!");
            binding.propertySubCategoryAct.requestFocus();
        } else if (category.equals(MyUtils.propertyTypes[0]) && floors.isEmpty()){
            //Property Type is Home, No floors count entered
            binding.floorsEt.setError("Enter Floors Count...!");
            binding.floorsEt.requestFocus();
        } else if (category.equals(MyUtils.propertyTypes[0]) && bedRooms.isEmpty()){
            //Property Type is Home, No bedrooms count entered
            binding.bedroomsEt.setError("Enter Bedrooms Count...!");
            binding.bedroomsEt.requestFocus();
        } else if (category.equals(MyUtils.propertyTypes[0]) && bathRooms.isEmpty()){
            //Property Type is Home, No bathrooms count entered in bathRoomsEt
            binding.bathRoomsEt.setError("Enter Bathrooms Count...!");
            binding.bathRoomsEt.requestFocus();
        } else if (areaSize.isEmpty()){
            //no area size entered in areaSizeEt
            binding.areaSizeEt.setError("Enter Area Size...!");
            binding.areaSizeEt.requestFocus();
        }else if (areaSizeUnit.isEmpty()){
            //no area size unit entered
            binding.areaSizeUnitAct.setError("Choose Area Size Unit...!");
            binding.areaSizeUnitAct.requestFocus();
        } else if (price.isEmpty()){
            //no price entered
            binding.priceEt.setError("Enter Price...!");
            binding.priceEt.requestFocus();
        } else if (title.isEmpty()){
            //no title entered
            binding.titleEt.setError("Enter Title...!");
            binding.titleEt.requestFocus();
        } else if (description.isEmpty()){
            //no description entered
            binding.descriptionEt.setError("Enter Description...!");
            binding.descriptionEt.requestFocus();
        } else if (phoneNumber.isEmpty()){
            //no phone number entered
            binding.phoneNumberEt.setError("Enter Phone Number...!");
            binding.phoneNumberEt.requestFocus();
        } else if (imagePickedArrayList.isEmpty()){
            //no image selected/picked
            MyUtils.toast(this, "Pick at least one image...!");
        } else {
            //All data is validated, we can proceed further now
            postAd();
        }
    }

    private void postAd(){
        Log.d(TAG, "postAd: ");
        //show progress
        progressDialog.setMessage("Publishing Ad...");
        progressDialog.show();
        if (bedRooms.isEmpty()){
            bedRooms="0";
        }

        //get current timestamp
        long timestamp = MyUtils.timestamp();
        //firebase database Properties reference to store new Properties
        DatabaseReference refProperties = FirebaseDatabase.getInstance().getReference("Properties");
        //key id from the reference to use as Ad id
        String keyId = refProperties.push().getKey();

        //setup data to add in firebase database
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", ""+keyId);
        hashMap.put("uid", ""+firebaseAuth.getUid());
        hashMap.put("purpose", ""+purpose);
        hashMap.put("category", ""+category);
        hashMap.put("subcategory", ""+subcategory);
        hashMap.put("areaSizeUnit", ""+areaSizeUnit);
        hashMap.put("areaSize", Double.parseDouble(areaSize));
        hashMap.put("title", ""+title);
        hashMap.put("description", ""+description);
        hashMap.put("email", ""+email);
        hashMap.put("phoneCode", ""+phoneCode);
        hashMap.put("phoneNumber", ""+phoneNumber);
        hashMap.put("country", ""+country);
        hashMap.put("city", ""+city);
        hashMap.put("address", ""+address);
        hashMap.put("status", ""+MyUtils.AD_STATUS_AVAILABLE);
        hashMap.put("floors", Long.parseLong(floors));
        hashMap.put("bedRooms", Long.parseLong(bedRooms));
        hashMap.put("bathRooms", Long.parseLong(bathRooms));
        hashMap.put("price", Double.parseDouble(price));
        hashMap.put("timestamp", timestamp);
        hashMap.put("latitude", latitude);
        hashMap.put("longitude", longitude);

        //set data to firebase database. Properties -> PropertyId -> PropertyDataJSON
        refProperties.child(keyId)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Ad Published");
                        uploadImagesStorage(keyId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(PostAddActivity.this, "Failed to publish due to "+e.getMessage());
                    }
                });
    }
    private void uploadImagesStorage(String propertyId){
        Log.d(TAG, "uploadImagesStorage: propertyId:"+propertyId);

        for (int i=0; i< imagePickedArrayList.size(); i++) {

            ModelImagePicked modelImagePicked = imagePickedArrayList.get(i);

            if (!modelImagePicked.isFromInternet()) {

                String imageName = modelImagePicked.getId();

                String filePathAndName = "Properties/" + imageName;

                int imageIndexForProgress = i + 1;

                Uri pickedImageUri = modelImagePicked.getImageUri();

                StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
                storageReference.putFile(pickedImageUri)
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();

                                String message = "Uploading " + imageIndexForProgress + " of " + imagePickedArrayList.size() + " images...\nProgress " + (int) progress + "%";
                                Log.d(TAG, "onProgress: message:" + message);
                                progressDialog.setMessage(message);
                                progressDialog.show();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Log.d(TAG, "onSuccess: ");

                                //image uploaded get url of uploaded image
                                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                while (!uriTask.isSuccessful()) ;
                                Uri uploadedImageUrl = uriTask.getResult();

                                if (uriTask.isSuccessful()) {

                                    HashMap<String, Object> hashMap = new HashMap<>();
                                    hashMap.put("id", "" + modelImagePicked.getId());
                                    hashMap.put("imageUrl", "" + uploadedImageUrl);

                                    DatabaseReference refProperties = FirebaseDatabase.getInstance().getReference("Properties");
                                    refProperties.child(propertyId).child("Images")
                                            .child(imageName)
                                            .updateChildren(hashMap);

                                }

                                progressDialog.dismiss();

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: ", e);
                                MyUtils.toast(PostAddActivity.this, "Failed to upload due to " + e.getMessage());
                                progressDialog.dismiss();
                            }
                        });
            }
        }
    }
}
