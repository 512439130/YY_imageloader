package com.yy.imageloader;

import android.app.ProgressDialog;
import android.content.ContentResolver;

import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;

import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yy.imageloader.adapter.ImageAdapter;
import com.yy.imageloader.bean.FolderBean;


import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private GridView mGridView;

    private ImageAdapter mImgAdapter;

    //GridView的数据集
    private List<String> mImages;

    private RelativeLayout mBottomLayout;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

    private ProgressDialog mProgressDialog;

    private static final int DATA_LOADED = 0X110;

    private ListImageDirPopupWindow mDirPopupWindow;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED) {
                mProgressDialog.dismiss();

                dataToView();

                initDirPopupWindow();
            }
        }
    };

    /**
     * 初始化PopupWindow
     */
    private void initDirPopupWindow() {
        mDirPopupWindow = new ListImageDirPopupWindow(this, mFolderBeans);
        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        mDirPopupWindow.setOnDirSelectedListener(new ListImageDirPopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelected(FolderBean folderBean) {
                //更新路径
                mCurrentDir = new File(folderBean.getDir());

                mImages = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String fileName) {
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }));

                mImgAdapter = new ImageAdapter(MainActivity.this,mImages,mCurrentDir.getAbsolutePath());
                mGridView.setAdapter(mImgAdapter);


                mDirCount.setText(mImages.size()+"");
                mDirName.setText(folderBean.getName());

                mDirPopupWindow.dismiss();//隐藏Popupwindow
            }
        });
    }



    private void dataToView() {
        if (mCurrentDir == null) {
            Toast.makeText(this, "未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImages = Arrays.asList(mCurrentDir.list());

        mImgAdapter = new ImageAdapter(this, mImages, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImgAdapter);

        mDirCount.setText(mMaxCount + "");
        mDirName.setText(mCurrentDir.getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initDatas();
        initEvents();
    }

    private void initViews() {
        mGridView = (GridView) findViewById(R.id.id_gridView);
        mBottomLayout = (RelativeLayout) findViewById(R.id.id_bottom_layout);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
    }

    /**
     * 利用ContentProvider扫描手机中的所有图片
     */
    private void initDatas() {
        //判断存储卡是否可用
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前存储卡不可用！", Toast.LENGTH_SHORT).show();
            return;
        }
        //显示正在加载的ProgressDialog
        mProgressDialog = ProgressDialog.show(this, null, "正在加载...");

        //开启线程
        new Thread() {
            @Override
            public void run() {
                //ContentProvider使用，需要Uri指向手机中所有的图片
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                //通过Resolver查询图片
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(
                        mImgUri,
                        null,
                        MediaStore.Images.Media.MIME_TYPE + " = ? or " + MediaStore.Images.Media.MIME_TYPE + " = ? ",
                        new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED);

                Set<String> mDirPaths = new HashSet<String>();


                while (cursor.moveToNext()) {
                    //遍历cursor，拿到每一个图片的路径
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null) {
                        continue;
                    }
                    String dirPath = parentFile.getAbsolutePath();
                    FolderBean folderBean = null;  //FolderBean初始化
                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgPath(path);
                    }
                    if (parentFile.list() == null) {
                        continue;
                    }
                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String fileName) {
                            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }).length;
                    folderBean.setCount(picSize);
                    mFolderBeans.add(folderBean);
                    if (picSize > mMaxCount) {
                        //用于显示Activity的底部（文件夹的名称和数量）
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }

                }
                cursor.close();
                //通知Handler扫描图片完成
                mHandler.sendEmptyMessage(DATA_LOADED);
            }
        }.start();
    }


    private void initEvents() {
        mBottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //添加打开关闭动画（android5默认有动画，暂时不用添加）
                //mDirPopupWindow.setAnimationStyle(R.style.dir_popup_window_anim);
                //设置PopupWindow显示的位置
                mDirPopupWindow.showAsDropDown(mBottomLayout,0,0);
                lightOff();
            }
        });
    }
    /**
     * 内容区域变亮
     */
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }
    /**
     * 内容区域变暗
     */
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = .3f;
        getWindow().setAttributes(lp);
    }

}
