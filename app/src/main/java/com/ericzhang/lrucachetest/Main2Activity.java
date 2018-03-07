package com.ericzhang.lrucachetest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;

public class Main2Activity extends Activity {

    private GridView gridView;
    private PhotoWallAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        gridView = findViewById(R.id.gv_main);
        adapter = new PhotoWallAdapter(this, R.layout.activity_main2_item, PhotoWallAdapter.Images.imageThumbUrls, gridView);
        gridView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        adapter.cancelAllTasks();
        super.onDestroy();
    }
}
