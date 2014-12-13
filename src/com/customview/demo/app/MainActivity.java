package com.customview.demo.app;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private DataListView mDataListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.activity_main);

        mDataListView = (DataListView) findViewById(R.id.data_list);
        List<Data> dataList = new ArrayList<Data>();
        for (int i = 0; i < 5; i++) {
            dataList.add(new Data(R.drawable.cat0));
            dataList.add(new Data(R.drawable.cat1));
            dataList.add(new Data(R.drawable.cat2));
            dataList.add(new Data(R.drawable.cat3));
        }
        mDataListView.setData(dataList, false);
    }
}
