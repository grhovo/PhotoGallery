package com.android.hovo.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {
    public static final String TAG = "PhotoGalleryFR";

    private List<GalleryItem> mItems = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>>{

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetcher().fetchItems();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems = galleryItems;
            setupAdapter();
        }
    }
    public static Fragment newInstance(){
        return new PhotoGalleryFragment();
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setRetainInstance(true);
        super.onCreate(savedInstanceState);
        new FetchItemsTask().execute();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setTThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(),thumbnail);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG,"Background thread started");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery,container,false);
        mRecyclerView = view.findViewById(R.id.photo_gallery);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        setupAdapter();
        return view;
    }

    private void setupAdapter() {
        if(isAdded()){
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder{
        private ImageView mPhoto;

        public PhotoHolder(View itemView){
            super(itemView);

            mPhoto = itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable){
            mPhoto.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;
        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }
        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item,viewGroup,false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int i) {
            GalleryItem galleryItem = mGalleryItems.get(i);
            Drawable placeholder = getResources().getDrawable(R.drawable.ic_action_name);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder,galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG,"Background thread destroyed");
    }


}
