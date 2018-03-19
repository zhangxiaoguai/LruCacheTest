package com.ericzhang.lrucachetest.lrucache;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.ericzhang.lrucachetest.R;
import com.ericzhang.lrucachetest.disklrucache.Main3Activity;
import com.ericzhang.lrucachetest.disklrucache.Main4Activity;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int ONE_MIB = 1024 * 1024;
    // 缓存大小
    private static final int CACHE_SIZE = 7 * ONE_MIB;
    private LruCache<String, Bitmap> bitmapLruCache;
    private LruCache<String, Bitmap> mMemoryCache;

    private ImageView iv_test;
    private Button bt_test;
    private Button bt_test_original;

    private ImageView iv_test2;
    private Button bt_test2;

    private ImageView iv_test3;
    private Button bt_test3;

    private ImageView iv_test4;
    private Button bt_test4;

    private Button bt_jump;

    private Button bt_disklrucache;
    private Button bt_mix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        iv_test = findViewById(R.id.iv_test);
        bt_test = findViewById(R.id.bt_test);
        bt_test.setOnClickListener(this);
        bt_test_original = findViewById(R.id.bt_test_original);
        bt_test_original.setOnClickListener(this);

        iv_test2 = findViewById(R.id.iv_test2);
        bt_test2 = findViewById(R.id.bt_test2);
        bt_test2.setOnClickListener(this);

        iv_test3 = findViewById(R.id.iv_test3);
        bt_test3 = findViewById(R.id.bt_test3);
        bt_test3.setOnClickListener(this);

        iv_test4 = findViewById(R.id.iv_test4);
        bt_test4 = findViewById(R.id.bt_test4);
        bt_test4.setOnClickListener(this);

        bt_jump = findViewById(R.id.bt_jump);
        bt_jump.setOnClickListener(this);

        bt_disklrucache = findViewById(R.id.bt_disklrucache);
        bt_disklrucache.setOnClickListener(this);
        bt_mix = findViewById(R.id.bt_mix);
        bt_mix.setOnClickListener(this);
    }

    private void initData() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), R.mipmap.mytestjpg, options);
        int outWidth = options.outWidth;
        int outHeight = options.outHeight;
        String outMimeType = options.outMimeType;
        Log.e(TAG, "outWidth: " + outWidth);
        Log.e(TAG, "outHeight: " + outHeight);
        Log.e(TAG, "outMimeType: " + outMimeType);

        // 缓存大小固定的LruCache
        bitmapLruCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
            }

            @Override
            protected int sizeOf(String key, Bitmap value) {
                // 默认返回图片数量
                // 一般肯定会重写此方法，来衡量每张图片的大小
                // 此处计算方法要和CACHE_SIZE对应
                return value.getByteCount();
            }
        };

        // 缓存大小随手机型号不同变化的LruCache
        // 每个应用程序最高可用内存是多少
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        Log.e(TAG, "maxMemory: " + maxMemory);
        // 使用最大可用内存值的1/8作为缓存的大小
        int mCacheSize = maxMemory / 8;
        Log.e(TAG, "mCacheSize: " + mCacheSize);
        mMemoryCache = new LruCache<String, Bitmap>(mCacheSize) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                Log.e(TAG, "entryRemoved evicted: " + evicted);
                Log.e(TAG, "entryRemoved key: " + key);
                Log.e(TAG, "entryRemoved oldValue: " + oldValue.hashCode());
                // 此处newValue为null,先删除后添加
                Log.e(TAG, "entryRemoved newValue: " + newValue);
                super.entryRemoved(evicted, key, oldValue, newValue);
            }

            @Override
            protected int sizeOf(String key, Bitmap value) {
                // 此处计算方法要和CACHE_SIZE对应
                return value.getByteCount() / 1024;
            }
        };
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        Log.e(TAG, "reqWidth: " + reqWidth);
        Log.e(TAG, "reqHeight: " + reqHeight);
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // options.inJustDecodeBounds置为true时返回的bitmap为null
        Bitmap bitmap = BitmapFactory.decodeResource(res, resId, options);
        Log.e(TAG, "bitmap: " + bitmap);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        Log.e(TAG, "inSampleSize: " + options.inSampleSize);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap1 = BitmapFactory.decodeResource(res, resId, options);
        Log.e(TAG, "bitmap1: " + bitmap1.getByteCount() / 1024);
        return bitmap1;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        // 压缩比例默认是1
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
            Log.e(TAG, "addBitmapToMemoryCache key: " + key);
            Log.e(TAG, "addBitmapToMemoryCache bitmap: " + bitmap.hashCode());
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_test:
//                int width = iv_test.getWidth();
//                int height = iv_test.getHeight();
//                iv_test.setImageBitmap(decodeSampledBitmapFromResource(getResources(), R.mipmap.mytestjpg, width, height));

                loadBitmap(R.mipmap.mytestjpg, iv_test);
                break;
            case R.id.bt_test_original:
                iv_test.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.mytestjpg, null));
                break;
            case R.id.bt_test2:
//                int width2 = iv_test2.getWidth();
//                int height2 = iv_test2.getHeight();
//                iv_test2.setImageBitmap(decodeSampledBitmapFromResource(getResources(), R.mipmap.mytest2, width2, height2));

                loadBitmap(R.mipmap.mytest2, iv_test2);
                break;
            case R.id.bt_test3:
//                int width3 = iv_test3.getWidth();
//                int height3 = iv_test3.getHeight();
//                iv_test3.setImageBitmap(decodeSampledBitmapFromResource(getResources(), R.mipmap.mytest3, width3, height3));

                loadBitmap(R.mipmap.mytest3, iv_test3);
                break;
            case R.id.bt_test4:
//                int width4 = iv_test4.getWidth();
//                int height4 = iv_test4.getHeight();
//                iv_test4.setImageBitmap(decodeSampledBitmapFromResource(getResources(), R.mipmap.mytest4, width4, height4));

                loadBitmap(R.mipmap.mytest4, iv_test4);
                break;
            case R.id.bt_jump:
                startActivity(new Intent(this, Main2Activity.class));
                break;
            case R.id.bt_disklrucache:
                startActivity(new Intent(this, Main3Activity.class));
                break;
            case R.id.bt_mix:
                startActivity(new Intent(this, Main4Activity.class));
                break;
            default:
                break;
        }
    }

    public void loadBitmap(int resId, ImageView imageView) {
        String imageKey = String.valueOf(resId);
        Bitmap bitmap = getBitmapFromMemCache(imageKey);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            int width = imageView.getWidth();
            int height = imageView.getHeight();
            bitmap = decodeSampledBitmapFromResource(getResources(), resId, width, height);
            imageView.setImageBitmap(bitmap);
            addBitmapToMemoryCache(String.valueOf(resId), bitmap);
        }
    }
}
