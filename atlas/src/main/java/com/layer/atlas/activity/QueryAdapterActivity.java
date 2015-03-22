package com.layer.atlas.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.layer.atlas.queryadapter.BaseQueryAdapter;

public abstract class QueryAdapterActivity extends BaseActivity {
    private final int mLayoutResId;
    private final int mAdapterViewResId;

    private RecyclerView mRecyclerView;
    private BaseQueryAdapter mAdapter;

    protected QueryAdapterActivity(String appId, String gcmSenderId, int layoutResId, int adapterViewResId) {
        super(appId, gcmSenderId);
        mLayoutResId = layoutResId;
        mAdapterViewResId = adapterViewResId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mLayoutResId);

        mRecyclerView = (RecyclerView) findViewById(mAdapterViewResId);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    }

    public void setAdapter(BaseQueryAdapter adapter) {
        mAdapter = adapter;
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    void onAuthenticatedResume() {
        if (mAdapter != null) {
            mAdapter.refresh();
        }
    }
}
