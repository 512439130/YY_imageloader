package com.yy.imageloader.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.yy.imageloader.R;
import com.yy.imageloader.util.ImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends BaseAdapter {

        private static final Set<String> mSelectedImg = new HashSet<String>();

        private String mDirPath;
        private List<String> mImgPaths;
        private LayoutInflater mInflater;

        private int mScreenWidth;

        //构造方法
        public ImageAdapter(Context context, List<String> mDatas, String dirPath) {
            this.mDirPath = dirPath;
            this.mImgPaths = mDatas;
            mInflater = LayoutInflater.from(context);

            //获取屏幕宽度
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            mScreenWidth = outMetrics.widthPixels;
        }

        @Override
        public int getCount() {
            return mImgPaths.size();
        }

        @Override
        public Object getItem(int position) {
            return mImgPaths.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_gridview, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.mImg = (ImageView) convertView.findViewById(R.id.id_item_image);
                viewHolder.mSelect = (ImageButton) convertView.findViewById(R.id.id_item_select);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            //重置状态
            viewHolder.mImg.setImageResource(R.mipmap.pictures_no);
            viewHolder.mSelect.setImageResource(R.mipmap.picture_unselected);
            viewHolder.mImg.setColorFilter(null);

            viewHolder.mImg.setMaxWidth(mScreenWidth / 3);

            //图片的加载
            ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(mDirPath + "/" + mImgPaths.get(position), viewHolder.mImg);

            final String filePath = mDirPath+"/"+mImgPaths.get(position);

            viewHolder.mImg.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    //使用图片的完整路径
                    //已经被选择
                    if(mSelectedImg.contains(filePath)){
                        mSelectedImg.remove(filePath);
                        viewHolder.mImg.setColorFilter(null);  //透明黑色
                        viewHolder.mSelect.setImageResource(R.mipmap.picture_unselected);

                    }else{  //未被选择
                        mSelectedImg.add(filePath);
                        viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));  //透明黑色
                        viewHolder.mSelect.setImageResource(R.mipmap.pictures_selected);
                    }
                    //notifyDataSetChanged();
                }
            });
            //选中状态的图片效果
            if(mSelectedImg.contains(filePath)){
                viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));  //透明黑色
                viewHolder.mSelect.setImageResource(R.mipmap.pictures_selected);
            }

            return convertView;
        }

        private class ViewHolder {
            ImageView mImg;
            ImageButton mSelect;
        }
    }