package com.twilio.sync.testapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {
    private Context mContext;

    public ImageAdapter(Context c) {
        mContext = c;
    }

    public void setItem(int position, int type) {
        boardItems[position] = type;
    }

    public int getCount() {
        return boardItems.length;
    }

    public Object getItem(int position) {
        return boardItems[position];
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageResource(boardItems[position]);
        return imageView;
    }

    // references to our images
    private Integer[] boardItems = {
        R.drawable.empty, R.drawable.empty, R.drawable.empty,
        R.drawable.empty, R.drawable.empty, R.drawable.empty,
        R.drawable.empty, R.drawable.empty, R.drawable.empty
    };
}
