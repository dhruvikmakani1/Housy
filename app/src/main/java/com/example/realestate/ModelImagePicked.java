package com.example.realestate;

import android.net.Uri;

public class ModelImagePicked {

    //variables
    String id = "";
    Uri imageUri = null;
    String imageUrl = null;
    boolean fromInternet = false;

    //Empty constructor require for firebase db
    public ModelImagePicked() {
    }

    //Constructor with all params
    public ModelImagePicked(String id, Uri imageUri, String imageUrl, boolean fromInternet) {
        this.id = id;
        this.imageUri = imageUri;
        this.imageUrl = imageUrl;
        this.fromInternet = fromInternet;
    }

    //---Getter & Setters---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isFromInternet() {
        return fromInternet;
    }

    public void setFromInternet(boolean fromInternet) {
        this.fromInternet = fromInternet;
    }
}