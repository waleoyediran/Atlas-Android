package com.layer.atlas.sampleapp.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import com.layer.atlas.adapter.BaseQueryAdapter;
import com.layer.atlas.view.BaseQueryView;

public abstract class QueryViewActivity extends BaseActivity {
    private final int mLayoutResId;
    private final int mAdapterViewResId;

    private BaseQueryView mQueryView;

    protected QueryViewActivity(String appId, String gcmSenderId, int layoutResId, int adapterViewResId) {
        super(appId, gcmSenderId);
        mLayoutResId = layoutResId;
        mAdapterViewResId = adapterViewResId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mLayoutResId);

        mQueryView = (BaseQueryView) findViewById(mAdapterViewResId);
        mQueryView.setLayout(LinearLayoutManager.VERTICAL, false);
    }

    public void setAdapter(BaseQueryAdapter adapter) {
        mQueryView.setAdapter(adapter);
    }

    @Override
    void onAuthenticatedResume() {
        BaseQueryAdapter adapter = (BaseQueryAdapter) mQueryView.getAdapter();
        if (adapter != null) {
            adapter.refresh();
        }
    }
}
