package com.example.realestate;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.realestate.databinding.RowImagesPickedBinding;

import java.util.ArrayList;

public class AdapterImagePicked extends RecyclerView.Adapter<AdapterImagePicked.HolderImagePicked> {

    //View Binding
    private RowImagesPickedBinding binding;
    //Tag to show logs in logcat
    private static final String TAG = "IMAGES_TAG";
    //Context of activity/fragment from where instance of AdapterImagePicked class is created
    private Context context;
    //imagePickedArrayList The list of the images picked/captured from Gallery/Camera or from Internet
    private ArrayList<ModelImagePicked> imagePickedArrayList;


    public AdapterImagePicked(Context context, ArrayList<ModelImagePicked> imagePickedArrayList) {
        this.context = context;
        this.imagePickedArrayList = imagePickedArrayList;
    }

    @NonNull
    @Override
    public HolderImagePicked onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate/bind the row_images_picked.xml
        binding = RowImagesPickedBinding.inflate(LayoutInflater.from(context), parent, false);
        return new HolderImagePicked(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderImagePicked holder, int position) {
        //get data from particular position of list and set to the UI Views of row_images_picked.xml and Handle clicks
        ModelImagePicked model = imagePickedArrayList.get(position);
        Uri imageUri = model.getImageUri();

        //set the image in imageIv
        Glide.with(context)
                .load(imageUri)
                .placeholder(R.drawable.image_gray)
                .into(holder.imageIv);

        //handle closeBtn click, remove image from imagePickedArrayList
        holder.closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //image is from device storage, just remove from list.
                imagePickedArrayList.remove(model);
                notifyItemRemoved(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        //return the size of list
        return imagePickedArrayList.size();
    }


    class HolderImagePicked extends RecyclerView.ViewHolder {

        //UI Views of the row_images_picked.xml
        ImageView imageIv;
        ImageButton closeBtn;

        public HolderImagePicked(@NonNull View itemView) {
            super(itemView);
            //init UI Views of the row_images_picked.xml
            imageIv = binding.imageIv;
            closeBtn = binding.closeBtn;
        }
    }
}