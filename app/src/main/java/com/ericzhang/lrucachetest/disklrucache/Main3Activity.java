package com.ericzhang.lrucachetest.disklrucache;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.ericzhang.lrucachetest.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main3Activity extends Activity implements View.OnClickListener {

    private static final String TAG = Main3Activity.class.getSimpleName();

    private DiskLruCache mDiskLruCache;

    private ImageView iv_test;
    private Button bt_test;
    private Button bt_remove;
    private Button bt_size;
    private Button bt_flush;
    private Button bt_close;
    private Button bt_delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        initView();
        initDiskLruCache();
    }

    private void initView() {
        iv_test = findViewById(R.id.iv_test);

        bt_test = findViewById(R.id.bt_test);
        bt_test.setOnClickListener(this);

        bt_remove = findViewById(R.id.bt_remove);
        bt_remove.setOnClickListener(this);

        bt_size = findViewById(R.id.bt_size);
        bt_size.setOnClickListener(this);

        bt_flush = findViewById(R.id.bt_flush);
        bt_flush.setOnClickListener(this);

        bt_close = findViewById(R.id.bt_close);
        bt_close.setOnClickListener(this);

        bt_delete = findViewById(R.id.bt_delete);
        bt_delete.setOnClickListener(this);
    }

    private void initDiskLruCache() {
        File cacheDir = getDiskCacheDir(this, "bitmap");
        if (!cacheDir.exists()) {
            // 没有的parent目录也会创建
            boolean mkdirs = cacheDir.mkdirs();
            // 只会创建自己
            // cacheDir.mkdir();
        }
        Log.e(TAG, "cacheDir: " + cacheDir);
        // TODO: 2018/3/8  最大容量也可以设置为最大磁盘容量的百分比
        long maxSize = 1024 * 1024 * 10;
        try {
            // 创建
            mDiskLruCache = DiskLruCache.open(cacheDir, getAppVersion(this), 1, maxSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            // /storage/emulated/0/Android/data/com.ericzhang.lrucachetest/cache
            // context.getExternalCacheDir()会返回null原因：
            // 权限问题：没有写外部存储的权限：
            // <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            // /data/data/com.ericzhang.lrucachetest/cache
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    public int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        Log.e(TAG, "hashKeyForDisk: " + cacheKey);
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_test:
                startLoadNetImage();
                break;
            case R.id.bt_remove:
                removeDiskLruCache();
                break;
            case R.id.bt_size:
                long size = mDiskLruCache.size();
                Log.e(TAG, "mDiskLruCache size: " + size);
                break;
            case R.id.bt_flush:
                try {
                    mDiskLruCache.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bt_close:
                try {
                    mDiskLruCache.close();
                    // close状态之后在调用会报错:java.lang.IllegalStateException: cache is closed
                    // 需要重新open才行
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bt_delete:
                try {
                    mDiskLruCache.delete();
                    // delete之后DiskLruCache会变成close状态
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    private void startLoadNetImage() {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // FIXME: 2018/3/8  存储和读取的时候可以增加一个压缩后的尺寸，与控件本身大小匹配
                            saveDiskLruCache();
                            getDiskLruCache();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();
    }

    private void saveDiskLruCache() throws IOException {
        String imageUrl = "http://img.my.csdn.net/uploads/201309/01/1378037235_7476.jpg";
        String keyForDisk = hashKeyForDisk(imageUrl);
        DiskLruCache.Editor edit = mDiskLruCache.edit(keyForDisk);
        if (edit != null) {
            OutputStream outputStream = edit.newOutputStream(0);
            if (downloadUrlToStream(imageUrl, outputStream)) {
                edit.commit();
            } else {
                edit.abort();
            }
        }
        mDiskLruCache.flush();
    }

    private void getDiskLruCache() throws IOException {
        String imageUrl = "http://img.my.csdn.net/uploads/201309/01/1378037235_7476.jpg";
        String keyForDisk = hashKeyForDisk(imageUrl);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(keyForDisk);
        if (snapshot != null) {
            InputStream inputStream = snapshot.getInputStream(0);
            final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    iv_test.setImageBitmap(bitmap);
                }
            });
        }
    }

    private void removeDiskLruCache() {
        // 这个方法不应该经常调用
        // DiskLruCache会根据我们在调用open()方法时设定的缓存最大值来自动删除多余的缓存
        // 只有你确定某个key对应的缓存内容已经过期，需要从网络获取最新数据的时候才应该调用remove()方法来移除缓存
        try {
            String imageUrl = "http://img.my.csdn.net/uploads/201309/01/1378037235_7476.jpg";
            String keyForDisk = hashKeyForDisk(imageUrl);
            mDiskLruCache.remove(keyForDisk);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
