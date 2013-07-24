package cheng.app.cnbeta.ui;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBSQLiteHelper.NewsColumns;
import cheng.app.cnbeta.util.HttpUtil;
import cheng.app.cnbeta.util.TimeUtil;

public class CacheListItemView extends RelativeLayout implements View.OnClickListener {
    private static final String TAG = "CacheListItemView";
    private TextView mTitleView;
    private CheckableImageView mCheckedView;
    private TextView mTimeView;
    private TextView mCmtView;

    public CacheListItemView(Context context) {
        super(context);
    }
    public CacheListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTitleView = (TextView) findViewById(R.id.cache_item_title);
        mCheckedView = (CheckableImageView) findViewById(R.id.cache_item_checked);
        mTimeView = (TextView) findViewById(R.id.cache_item_time);
        mCmtView = (TextView) findViewById(R.id.cache_item_cmt);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }


    public void setChecked(boolean isChecked) {
        mCheckedView.setChecked(isChecked);
    }

    public void setData(Cursor c) {
        if (c == null) {
            mTitleView.setText(R.string.error);
            mTimeView.setText("");
            mCmtView.setText("");
            return;
        }
        mTitleView.setText(HttpUtil.filterEntities(c.getString(c.getColumnIndex(NewsColumns.TITLE))));
        mTimeView.setText(TimeUtil.formatTime(getContext(), c.getString(c.getColumnIndex(NewsColumns.PUBTIME))));
        int cmtClosed = c.getInt(c.getColumnIndex(NewsColumns.CMT_CLOSED));
        int cmtNumber = c.getInt(c.getColumnIndex(NewsColumns.CMT_NUMBER));
        if (cmtClosed == 0) {
            mCmtView.setText(getResources().getString(R.string.cmt, cmtNumber));
        } else {
            mCmtView.setText(R.string.cmt_close);
        }
    }
}
