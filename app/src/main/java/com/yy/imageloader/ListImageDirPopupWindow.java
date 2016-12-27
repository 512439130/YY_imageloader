package com.yy.imageloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import android.view.WindowManager;

import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;




import com.yy.imageloader.adapter.ListDirAdapter;
import com.yy.imageloader.bean.FolderBean;


import java.util.List;

/**
 * Created by 13160677911 on 2016-12-27.
 */

public class ListImageDirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;
    private ListDirAdapter mListDirAdapter;

    private List<FolderBean> mDatas;

    public interface OnDirSelectedListener{
        void onSelected(FolderBean folderBean);
    }
    public OnDirSelectedListener mListener;

    public void setOnDirSelectedListener(OnDirSelectedListener mListener) {
        this.mListener = mListener;
    }

    public ListImageDirPopupWindow(Context context, List<FolderBean> datas) {
        //计算宽和高
        calWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_main, null);
        mDatas = datas;

        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                } else {
                    return false;
                }
            }
        });

        initViews(context);
        initEvents();
    }


    private void initViews(Context context) {
        mListView = (ListView) mConvertView.findViewById(R.id.id_list_dir);

        mListDirAdapter = new ListDirAdapter(context, mDatas);
        if(null == mListView){
            System.out.println("mListView对象为空");
        }else{
            System.out.println("mListView对象不为空");
        }
        mListView.setAdapter(mListDirAdapter);
    }

    private void initEvents() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mListener != null){
                    mListener.onSelected(mDatas.get(position));;
                }
            }
        });
    }

    /**
     * 计算popupWindow的宽度和高度
     *
     * @param context
     */
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.7);
    }




}
