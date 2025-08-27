package com.example.realestate.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.view.MenuItem; // Added import
import androidx.annotation.NonNull; // Added import for @NonNull

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.navigation.NavigationBarView; // Added import
import com.example.realestate.databinding.ActivityMainBinding; // Added import
import com.example.realestate.fragments.ChatsListFragment; // Added import
import com.example.realestate.fragments.FavoritListFragment; // Added import
import com.example.realestate.fragments.HomeFragment; // Added import
import com.example.realestate.fragments.ProfileFragment; // Added import

public class MainActivity extends AppCompatActivity {

    //View Binding
    private ActivityMainBinding binding;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth=FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser()==null){
            startLoginOptionsActivity();
        }

        //By default (when app open) show HomeFragment
        showHomeFragment();

        //handle bottomNv item clicks to navigate between fragments
        binding.bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //get id of the menu item clicked
                int itemId = item.getItemId();

                //check which item is clicked and show fragment accordingly
                if (itemId == R.id.item_home) {
                    //Home Item clicked, show HomeFragment
                    showHomeFragment();
                    return true;
                } else if (itemId == R.id.item_chats) {
                    //Chats item clicked, show ChatsListFragment

                    if (firebaseAuth.getCurrentUser() == null){
                        MyUtils.toast(MainActivity.this, "Login Required...!");
                        return false;
                    } else {
                        showChatsListFragment();
                        return true;
                    }
                } else if (itemId == R.id.item_favorite) {
                    //Favorites item clicked, show FavoritListFragment

                    if (firebaseAuth.getCurrentUser() == null){
                        MyUtils.toast(MainActivity.this, "Login Required...!");
                        return false;
                    } else {
                        showFavoritListFragment();
                        return true;
                    }
                } else if (itemId == R.id.item_profile) {
                    //Profile item clicked, show ProfileFragment

                    if (firebaseAuth.getCurrentUser() == null){
                        MyUtils.toast(MainActivity.this, "Login Required...!");
                        return false;
                    } else {
                        showProfileFragment();
                        return true;
                    }
                }else{
                    return false;
                }

            }
        });
    }



    private void showHomeFragment(){
        binding.toolbarTitleTv.setText("Home");
        HomeFragment homeFragment = new HomeFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFL.getId(), homeFragment, "HomeFragment");
        fragmentTransaction.commit();
    }

    private void showChatsListFragment(){
        binding.toolbarTitleTv.setText("Chats");
        ChatsListFragment chatsListFragment = new ChatsListFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFL.getId(), chatsListFragment, "ChatsListFragment");
        fragmentTransaction.commit();
    }

    private void showFavoritListFragment(){
        binding.toolbarTitleTv.setText("Favorites");
        FavoritListFragment favoritListFragment = new FavoritListFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFL.getId(), favoritListFragment, "FavoritListFragment");
        fragmentTransaction.commit();
    }

    private void showProfileFragment(){
        binding.toolbarTitleTv.setText("Profile");
        ProfileFragment profileFragment = new ProfileFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFL.getId(), profileFragment, "ProfileFragment");
        fragmentTransaction.commit();
    }
    private void startLoginOptionsActivity() {
        startActivity(new Intent(this, LoginOptionsActivity.class));
    }
}