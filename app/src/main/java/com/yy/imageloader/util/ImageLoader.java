package com.yy.imageloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by 13160677911 on 2016-12-27.
 * 图片加载类
 */

public class ImageLoader {
    //单例模式
    private static ImageLoader mInstance;

    public static ImageLoader getInstance() {
        if (mInstance == null) {  //双重判断提高效率
            synchronized (ImageLoader.class) {  //同步处理
                if (mInstance == null) {
                    mInstance = new ImageLoader(3, Type.LIFO);
                }
            }
        }
        return mInstance;
    }

    public static ImageLoader getInstance(int threadCount, Type type) {
        if (mInstance == null) {  //双重判断提高效率
            synchronized (ImageLoader.class) {  //同步处理
                if (mInstance == null) {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }
    //构造方法
    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    //创建成员变量
    /**
     * 图片缓存的核心对象
     */
    private LruCache<String, Bitmap> mLruCache;

    /**
     * 线程池(执行加载图片的任务)
     */
    private ExecutorService mThreadPool;
    private static final int DEFAULT_THREAD_COUNT = 1;
    /**
     * 队列的调度方式
     */
    private Type mType = Type.LIFO;

    //LinkedList为链表存储，不需要内存
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;
    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    //和线程绑定，发送消息
    private Handler mPoolThreadHandler;
    /**
     * UI线程中的Handler(用户更新ImageView)
     */
    private Handler mUIHandler;

    //信号量控制
    //异步线程的顺序执行，防止空指针异常
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    private Semaphore mSemaphoreThreadPool;


    public enum Type {
        FIFO, LIFO;
    }

    private class ImageSize {
        int width;
        int height;
    }




    /**
     * 初始化操作
     *
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {
        //后台轮询线程
        mPoolThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                //如果有任务，Handler会发送一个Message,会调用handleMessage方法
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池去取出一个任务进行执行
                        mThreadPool.execute(getTask());

                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {

                        }
                    }
                };
                //释放一个信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();//进行循环
            }
        };
        mPoolThread.start();

        //获取应用的最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        //内存池大小
        int cacheMemory = maxMemory / 8;

        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            //测量每个Bitmap的值
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight(); //每一行的字节数 * 高度
            }
        };

        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;//执行策略

        //初始化信号量
        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    /**
     * 从任务队列取出一个方法
     *
     * @return
     */
    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    /**
     * 根据path为imageview设置图片
     *
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);//防止imageview调用多次

        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //获取得到的图片，为imageview回调设置图片
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bitmap = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;
                    //将path与getTag存储路径进行比较
                    if (imageView.getTag().toString().equals(path)) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            };
        }

        //根据path在缓存中获取bitmap
        Bitmap bitmap = getBitmapFromLruCache(path);

        if (bitmap != null) {
            refreashBitmap(bitmap, path, imageView);
        } else {
            addTask(new Runnable() {
                @Override
                public void run() {
                    //加载图片
                    //图片的压缩
                    //1.获得图片需要显示的大小
                    ImageSize imageSize = getImageViewSize(imageView);
                    //2.压缩图片
                    Bitmap bitmap = decodeSampledBitmapFromPath(path, imageSize.width, imageSize.height);
                    //3.把图片加入到缓存中
                    addBitmapToLruCache(path,bitmap);
                    refreashBitmap(bitmap, path, imageView);

                    mSemaphoreThreadPool.release();
                }
            });


        }
    }

    /**
     * 刷新Bitmap
     * @param bitmap
     * @param path
     * @param imageView
     */
    private void refreashBitmap(Bitmap bitmap, String path, ImageView imageView) {
        Message message = Message.obtain();
        ImageBeanHolder holder = new ImageBeanHolder();
        holder.bitmap = bitmap;
        holder.path = path;
        holder.imageView = imageView;
        message.obj = holder;
        mUIHandler.sendMessage(message);  //发送消息
    }

    /**
     * 将图片加入LruCache
     * @param path
     * @param bitmap
     */
    private void addBitmapToLruCache(String path, Bitmap bitmap) {
        if(getBitmapFromLruCache(path) == null){
            if(bitmap != null){
                mLruCache.put(path,bitmap);//加入缓存
            }
        }
    }

    /**
     * 根据图片需要显示的宽和高对图片进行压缩
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        //获取图片的宽和高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        //计算InSampleSize(缩放比例)
        options.inSampleSize = caculateInSampleSize(options, width, height);

        //使用获取到的InSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);

        return bitmap;
    }

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
     *
     * @param options
     * @param reqWidth  //实际宽度
     * @param reqHeight  //实际高度
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);

            inSampleSize = Math.max(widthRadio, heightRadio);

        }
        return inSampleSize;
    }

    /**
     * 根据ImageView获得适当的压缩宽和高
     *
     * @param imageView
     * @return
     */
    protected ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        //获取屏幕宽度
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();

        int width = imageView.getWidth(); //获取imageview的实际宽度
        if (width <= 0) {
            width = lp.width;  //获取imageview在layout中声明的宽度
        }
        if (width <= 0) {
           // width = imageView.getMaxWidth();  //检查最大值
            //通过反射机制获取图片宽度
            width = getImageViewFieldValue(imageView,"mMaxWidth");
        }
        if (width <= 0) {
            width = displayMetrics.widthPixels;  //使用屏幕宽度
        }


        int height = imageView.getHeight(); //获取imageview的实际宽度
        if (height <= 0) {
            height = lp.height;  //获取imageview在layout中声明的宽度
        }
        if (height <= 0) {
            height = imageView.getMaxHeight();  //检查最大值
            //通过反射机制获取图片高度
            height = getImageViewFieldValue(imageView,"mMaxHeight");
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;  //使用屏幕宽度
        }

        imageSize.width = width;
        imageSize.height = height;

        return imageSize;
    }

    /**
     * 通过反射获取imageview的某个属性值
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object, String fieldName){
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if(fieldValue > 0 && fieldValue < Integer.MAX_VALUE){
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }


    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        //判断
        try {
            if(mPoolThreadHandler == null){
                mSemaphorePoolThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * 根据path在缓存中获取bitmap
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    private class ImageBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
