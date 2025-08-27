package com.example.realestate.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.realestate.MyUtils;
import com.example.realestate.databinding.ActivityLoginPhoneBinding;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class LoginPhoneActivity extends AppCompatActivity {

    // View Binding
    private ActivityLoginPhoneBinding binding;
    // Tag to show logs in logcat
    private static final String TAG = "LOGIN_PHONE_TAG";
    // ProgressDialog to show while phone login, saving user info
    private ProgressDialog progressDialog;
    // Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    // To handle success and failure of OTP sending and verification
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;
    // To hold verification ID, will be used to verify OTP
    private String mVerificationId;
    // To hold force resending token, will be used to resend OTP if required
    private PhoneAuthProvider.ForceResendingToken forceResendingToken;

    // Variables to hold phone details entered by user
    private String phoneCode = "";
    private String phoneNumber = "";
    private String phoneNumberWithCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // For the start show phone input UI and hide OTP UI
        binding.phoneInputRl.setVisibility(View.VISIBLE);
        binding.otpInputRl.setVisibility(View.GONE);

        // init/setup ProgressDialog to show while login
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        // Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize phone login callbacks
        phoneLoginCallBack();

        // handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(v -> finish());

        // handle sendOtpBtn, send OTP to input phone number
        binding.sendOtpBtn.setOnClickListener(v -> validateData());

        // handle resendOtpTv click, resend OTP
        binding.resendOtpTv.setOnClickListener(v -> resendVerificationCode(forceResendingToken));

        // handle verifyOtpBtn click, verify OTP received
        binding.verifyOtpBtn.setOnClickListener(v -> {
            // input OTP
            String otp = binding.otpEt.getText().toString().trim();
            // validate if otp is entered and length is 6 characters
            if (otp.isEmpty()){
                binding.otpEt.setError("Enter OTP");
                binding.otpEt.requestFocus();
            } else if (otp.length() < 6){
                binding.otpEt.setError("OTP Length must be 6 characters");
                binding.otpEt.requestFocus();
            } else {
                // data is valid, start verification
                verifyPhoneNumberWithCode(otp);
            }
        });
    }

    private void validateData() {
        // input data
        phoneCode = binding.phoneCodeTil.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();
        phoneNumberWithCode = phoneCode + phoneNumber;

        // show input data in logcat
        Log.d(TAG, "validateData: Phone Code: " + phoneCode);
        Log.d(TAG, "validateData: Phone Number: " + phoneNumber);
        Log.d(TAG, "validateData: Phone Number With Code: " + phoneNumberWithCode);

        // validate data
        if (phoneNumber.isEmpty()){
            // Phone Number is not entered, show error
            binding.phoneNumberEt.setError("Enter Phone Number");
            binding.phoneNumberEt.requestFocus();
        } else {
            // data is valid, start phone number verification
            startPhoneNumberVerification();
        }
    }

    private void startPhoneNumberVerification(){
        // show progress
        progressDialog.setMessage("Sending OTP to " + phoneNumberWithCode);
        progressDialog.show();

        // Setup Phone Auth Options with phone number, time out, callback etc.
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phoneNumberWithCode)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallBacks)
                        .build();
        // Start phone verification with PhoneAuthOptions
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendVerificationCode(PhoneAuthProvider.ForceResendingToken token) {
        progressDialog.setMessage("Resending OTP to " + phoneNumberWithCode);
        progressDialog.show();

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phoneNumberWithCode)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallBacks)
                        .setForceResendingToken(token) // Pass the force resending token here
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyPhoneNumberWithCode(String otp) {
        // show progress
        Log.d(TAG,"verifyPhoneNumberWithCode:OTP:"+otp);
        progressDialog.setMessage("Verifying OTP...");
        progressDialog.show();

        // PhoneAuthCredential with verification id and otp to signin user with signInWithPhoneAuthCredential
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp);
        signInWithPhoneAuthCredential(credential);
    }

    private void phoneLoginCallBack() {
        Log.d(TAG,"phoneLoginCallBack: ");
        mCallBacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted: ");
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.e(TAG, "onVerificationFailed: ", e);
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                progressDialog.dismiss();
                MyUtils.toast(LoginPhoneActivity.this, "Failed to verify due to " + e.getMessage());
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verificationId, token);
                Log.d(TAG, "onCodeSent: ");


                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                forceResendingToken = token;

                // OTP is sent so hide progress for now
                progressDialog.dismiss();

                // OTP is sent so hide phone ui and show otp ui
                binding.phoneInputRl.setVisibility(View.GONE);
                binding.otpInputRl.setVisibility(View.VISIBLE);

                // Show toast for success sending OTP
                MyUtils.toast(LoginPhoneActivity.this, "OTP sent to " + phoneNumberWithCode);
                // Show user a message that Please type the verification code sent to the phone number user has input
                binding.loginPhoneLabel.setText("Please type verification code sent to " + phoneNumberWithCode);
            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        // show progress
        progressDialog.setMessage("Logging In...");
        progressDialog.show();

        // SignIn to firebase auth using Phone Credentials
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // SignIn Success, let's check if the user is new (New Account Register) or existing (Existing Login)
                        Log.d(TAG, "onSuccess: ");
                        if (authResult.getAdditionalUserInfo().isNewUser()){
                            // New User, Account created. Let's save user info to firebase realtime database
                            Log.d(TAG, "onSuccess: New User, Account created...");
                            updateUserInfo();
                        } else {
                            // Existing User, Logged In.
                            Log.d(TAG, "onSuccess: Existing User, Logged In...");
                            // New User, Account created. No need to save user info to firebase realtime database. Start MainActivity
                            startActivity(new Intent(LoginPhoneActivity.this, MainActivity.class));
                            finishAffinity();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // SignIn failed, show exception message
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(LoginPhoneActivity.this, "Login Failed due to " + e.getMessage());
                    }
                });
    }

    private void updateUserInfo(){
        Log.d(TAG, "updateUserInfo: ");
        progressDialog.setMessage("Saving User Info...");
        progressDialog.show();

        // get current timestamp e.g. to show user registration date/time
        long timestamp = MyUtils.timestamp();
        // get uid of registered user
        String registeredUserUid = firebaseAuth.getUid();

        // setup data to save in firebase realtime db. most of the data will be empty and will set in edit profile
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", registeredUserUid);
        hashMap.put("email", "");
        hashMap.put("name", "");
        hashMap.put("timestamp", timestamp);
        hashMap.put("phoneCode", "" + phoneCode);
        hashMap.put("phoneNumber", "" + phoneNumber);
        hashMap.put("profileImageUrl", "");
        hashMap.put("dob", "");
        hashMap.put("userType",""+ MyUtils.USER_TYPE_PHONE);
        hashMap.put("token", ""); // FCM token to send push notifications

        // set data to firebase db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(registeredUserUid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // User info save success
                        Log.d(TAG, "onSuccess: User info saved...");
                        progressDialog.dismiss();
                        // Start MainActivity
                        startActivity(new Intent(LoginPhoneActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // User info save failed
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(LoginPhoneActivity.this, "Failed to save due to " + e.getMessage());
                    }
                });
    }
}