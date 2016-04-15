package com.example.jess.drawingtest.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.example.jess.drawingtest.R;
import com.example.jess.drawingtest.singleton.Singleton;

import java.util.ArrayList;

/**
 * Created by sanya on 31-Jan-16.
 */
public class GalleryGridviewAdapter extends BaseAdapter {

    private int count;
    private ArrayList<String> galleryImages;
    private Context mContext;
    private LayoutInflater layoutInflater;
    private int imgWidth, imgHeight;

    public GalleryGridviewAdapter(Context context, ArrayList<String> imagepaths) {

        mContext = context;
        galleryImages = imagepaths;
        count = imagepaths.size();

        imgWidth = Singleton.INSTANCE.getWidth();
        imgHeight = Singleton.INSTANCE.getHeight();

    }

    public int getCount() {
        return count;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();

            layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.gridimageview, null);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.imageViewGrid);

            convertView.setTag(viewHolder);
        }
        else {

            viewHolder = (ViewHolder)convertView.getTag();
        }


        Bitmap myBitmap = BitmapFactory.decodeFile(galleryImages.get(position));
        Bitmap scaledBitmap =  Bitmap.createScaledBitmap(myBitmap,imgWidth,imgHeight,false);
        viewHolder.imageView.setImageBitmap(scaledBitmap);
        return convertView;
    }


    static class ViewHolder
    {
        ImageView imageView;
    }
}