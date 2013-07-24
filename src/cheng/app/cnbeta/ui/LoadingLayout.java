package cheng.app.cnbeta.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import cheng.app.cnbeta.R;

public class LoadingLayout extends LinearLayout {
    ProgressBar mProgressBar;
    TextView mTextView;
    boolean mIsLoading = false;

    public LoadingLayout(Context context) {
        super(context);
    }

    public LoadingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mTextView = (TextView) findViewById(R.id.text);
    }

    public void setLoading(boolean loading) {
        if (mIsLoading == loading) {
            return;
        }
        mIsLoading = loading;
        if (mIsLoading) {
            mProgressBar.setVisibility(View.VISIBLE);
            mTextView.setText(R.string.loading);
        } else {
            mProgressBar.setVisibility(View.GONE);
            mTextView.setText(R.string.load_more);
        }
    }
    public boolean isLoading() {
        return mIsLoading;
    }
}
