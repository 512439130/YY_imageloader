package com.yy.imageloader.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.yy.imageloader.R;
import com.yy.imageloader.bean.FolderBean;
import com.yy.imageloader.util.ImageLoader;



import java.util.List;

/**
 * Created by 13160677911 on 2016-12-27.
 */

public class ListDirAdapter extends ArrayAdapter<FolderBean> {

    private LayoutInflater mInflater;
    private List<FolderBean> mDatas;

    public ListDirAdapter(Context context, List<FolderBean> objects) {
        super(context, 0, objects);
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolader holder = null;
        if(convertView == null){
            holder = new ViewHolader();
            convertView = mInflater.inflate(R.layout.item_popup_main,parent,false);
            holder.mImg = (ImageView) convertView.findViewById(R.id.id_dir_item_image);
            holder.mDirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
            holder.mDirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);

            convertView.setTag(holder);
        }else{
            holder = (ViewHolader) convertView.getTag();
        }

        FolderBean bean = getItem(position);
        //重置
        holder.mImg.setImageResource(R.mipmap.pictures_no);

        ImageLoader.getInstance().loadImage(bean.getFirstImgPath(), holder.mImg);

        holder.mDirCount.setText(bean.getCount()+"");
        holder.mDirName.setText(bean.getName());


        return convertView;
    }

    private class ViewHolader{
        ImageView mImg;
        TextView mDirName;
        TextView mDirCount;
    }
}
