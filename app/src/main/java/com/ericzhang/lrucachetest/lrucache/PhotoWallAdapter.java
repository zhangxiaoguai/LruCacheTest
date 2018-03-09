package com.ericzhang.lrucachetest.lrucache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.ericzhang.lrucachetest.R;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class PhotoWallAdapter extends ArrayAdapter<String> implements AbsListView.OnScrollListener {

    private static final String TAG = PhotoWallAdapter.class.getSimpleName();

    /**
     * 所有正在下载或等待下载的任务
     */
    private Set<BitmapTask> taskCollection;

    /**
     * 图片缓存技术的核心类，用于缓存所有下载好的图片，在程序内存达到设定值时会将最少最近使用的图片移除掉
     */
    private LruCache<String, Bitmap> mMemoryCache;

    /**
     * GridView的实例
     */
    private GridView mPhotoWall;

    /**
     * 第一张可见图片的下标
     */
    private int mFirstVisibleItem;

    /**
     * 一屏有多少张图片可见
     */
    private int mVisibleItemCount;

    /**
     * 记录是否刚打开程序，用于解决进入程序不滚动屏幕，不会下载图片的问题。
     */
    private boolean isFirstEnter = true;

    private static int iv_image_width;
    private static int iv_image_height;

    public PhotoWallAdapter(Context context, int textViewResourceId, String[] objects, GridView photoWall) {
        super(context, textViewResourceId, objects);
        mPhotoWall = photoWall;
        taskCollection = new HashSet<>();
//        int maxMemory = (int) Runtime.getRuntime().maxMemory();
//        int cacheSize = maxMemory / 8;
        int ONE_MIB = 1024 * 1024;
        // 缓存大小
        int CACHE_SIZE = 7 * ONE_MIB;
        mMemoryCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                Log.e(TAG, "key: " + key);
            }

            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        mPhotoWall.setOnScrollListener(this);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final String url = getItem(position);
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.activity_main2_item, null);
        } else {
            view = convertView;
        }
        final ImageView iv_item = view.findViewById(R.id.iv_item);
        iv_image_width = iv_item.getWidth();
        iv_image_height = iv_item.getHeight();
        iv_item.setTag(url);
        setImageView(iv_item, url);
        return view;
    }

    private void setImageView(ImageView iv_item, String url) {
        Bitmap bitmapFromMemoryCache = getBitmapFromMemoryCache(url);
        if (bitmapFromMemoryCache == null) {
            iv_item.setImageResource(R.mipmap.ic_launcher);
        } else {
            iv_item.setImageBitmap(bitmapFromMemoryCache);
        }
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            Log.e(TAG, "addBitmapToMemoryCache: " + key);
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemoryCache(String key) {
        Log.e(TAG, "getBitmapFromMemoryCache: " + key);
        return mMemoryCache.get(key);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // 仅当GridView静止时才去下载图片，GridView滑动时取消所有正在下载的任务
        if (scrollState == SCROLL_STATE_IDLE) {
            cancelAllTasks();
            loadBitmaps(mFirstVisibleItem, mVisibleItemCount);
        } else {
            cancelAllTasks();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mFirstVisibleItem = firstVisibleItem;
        mVisibleItemCount = visibleItemCount;
        // 下载的任务应该由onScrollStateChanged里调用，但首次进入程序时onScrollStateChanged并不会调用，
        // 因此在这里为首次进入程序开启下载任务。
        if (isFirstEnter && visibleItemCount > 0) {
            loadBitmaps(firstVisibleItem, visibleItemCount);
            isFirstEnter = false;
        }
    }

    private void loadBitmaps(int mFirstVisibleItem, int mVisibleItemCount) {
        for (int i = mFirstVisibleItem; i < mFirstVisibleItem + mVisibleItemCount; i++) {
            String url = Images.imageThumbUrls[i];
            // TODO: 2018/3/7
//            String item = getItem(i);
            Bitmap cacheBitmap = getBitmapFromMemoryCache(url);
            if (cacheBitmap == null) {
                BitmapTask task = new BitmapTask();
                taskCollection.add(task);
                task.execute(url);
            } else {
                ImageView imageView = mPhotoWall.findViewWithTag(url);
                if (imageView != null) {
                    imageView.setImageBitmap(cacheBitmap);
                }
            }
        }
    }

    /**
     * 取消所有正在下载或等待下载的任务。
     */
    public void cancelAllTasks() {
        if (taskCollection != null) {
            for (BitmapTask task : taskCollection) {
                task.cancel(true);
            }
        }
    }

    class BitmapTask extends AsyncTask<String, Integer, Bitmap> {

        private String imageUrl;

        @Override
        protected Bitmap doInBackground(String... strings) {
            imageUrl = strings[0];
            Bitmap bitmap = downloadBitmap(imageUrl);
            if (bitmap != null) {
                addBitmapToMemoryCache(imageUrl, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ImageView item = mPhotoWall.findViewWithTag(imageUrl);
            if (item != null && bitmap != null) {
                item.setImageBitmap(bitmap);
            }
            taskCollection.remove(this);
        }

        /**
         * 建立HTTP请求，并获取Bitmap对象。
         *
         * @param imageUrl 图片的URL地址
         * @return 解析后的Bitmap对象
         */
        private Bitmap downloadBitmap(String imageUrl) {
            Bitmap bitmap = null;
            HttpURLConnection con = null;
            try {
                URL url = new URL(imageUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5 * 1000);
                con.setReadTimeout(10 * 1000);
                if (iv_image_height != 0 && iv_image_width != 0) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.outHeight = iv_image_height;
                    options.outWidth = iv_image_width;
                    bitmap = BitmapFactory.decodeStream(con.getInputStream(), null, options);
                } else {
                    bitmap = BitmapFactory.decodeStream(con.getInputStream());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
            return bitmap;
        }

    }


    public static class Images {

        public final static String[] imageThumbUrls = new String[]{
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0j3u36_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0j3aa9_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0j34sm_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0j309v_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0it9mf_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0irhod_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0iq8h1_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0iq4q8_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0ipskv_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0ipi9l_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0iphuv_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0iph4l_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0ipgi4_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0ip3lq_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0iotte_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0ioj1i_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0iodj6_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0ioadv_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0ink7i_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0inbk0_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmb0in6ri_.jpg",
                "http://avatar.csdn.net/1/1/E/1_fengyuzhengfan.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmav87ofo_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmav6c90q_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmav5lf2g_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmav5ldnd_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmav5lck0_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmav5k4mu_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmav5k3qt_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmav4i0j3_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmav0s657_.jpg",
                "http://cs-img.bbtree.com/4833/201803/4kcajmav0s4cu_.jpg",
                "http://avatar.csdn.net/9/B/0/1_xb12369.jpg"};
    }

}
