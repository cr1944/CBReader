package cheng.app.cnbeta.ui;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBNewsEntry;
import cheng.app.cnbeta.ui.activity.ShareDialogActivity;
import cheng.app.cnbeta.util.Configs;
import cheng.app.cnbeta.util.ImageLoader;
import cheng.app.cnbeta.util.TimeUtil;

public class NewsListItemView extends LinearLayout implements View.OnClickListener {
    private static final String TAG = "NewsListItemView";
    private TextView mTitleView;
    private ImageView mShareView;
    private TextView mSummaryView;
    private ImageView mLogoView;
    private TextView mTimeView;
    private TextView mCmtView;
    private ImageView mCacheView;
    private ImageLoader mImageLoader;
    CBNewsEntry mData;

    public NewsListItemView(Context context) {
        super(context);
        init(context);
    }
    public NewsListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context) {
        Log.d(TAG, "init");
        mImageLoader = ImageLoader.getInstance(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTitleView = (TextView) findViewById(R.id.news_item_title);
        mSummaryView = (TextView) findViewById(R.id.news_item_summary);
        mLogoView = (ImageView) findViewById(R.id.news_item_logo);
        mTimeView = (TextView) findViewById(R.id.news_item_time);
        mCmtView = (TextView) findViewById(R.id.news_item_cmt);
        mCacheView = (ImageView) findViewById(R.id.news_item_cache);
        mShareView = (ImageView) findViewById(R.id.news_item_share);
        mCacheView.setOnClickListener(this);
        mShareView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.news_item_cache: {
                if (mData != null) {
                    Toast.makeText(getContext(), R.string.cache_start,
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Configs.ACTION_NEWS_LOAD);
                    intent.putExtra(Configs.EXTRA_ID, mData.articleId);
                    getContext().startService(intent);
                }
                break;
            }
            case R.id.news_item_share: {
                if (mData != null) {
                    new ShotTask(this).execute();
                }
                break;
            }
        }
    }

    private class ShotTask extends ScreenShotTask {

        public ShotTask(View traget) {
            super(traget);
        }

        @Override
        protected void onFinish(byte[] result) {
            if (result == null) {
                return;
            }
            String url = "http://www.cnbeta.com/articles/" + mData.articleId + ".htm";
            String text = getResources().getString(R.string.share_news_text,
                    mData.title, url);
            Intent intent = new Intent(getContext(), ShareDialogActivity.class);
            intent.putExtra(Configs.EXTRA_BITMAP, result);
            intent.putExtra(Configs.EXTRA_TITLE, R.string.share_news);
            intent.putExtra(Configs.EXTRA_TEXT, text);
            getContext().startActivity(intent);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setData(CBNewsEntry data, boolean displayLogo, int fontSize) {
        mData = data;
        if (mData == null) {
            mTitleView.setText("");
            mTimeView.setText("");
            mSummaryView.setText(R.string.error);
            mLogoView.setImageResource(R.drawable.ic_launcher);
            mCmtView.setText("");
            return;
        }
        mTitleView.setText(mData.title);
        mSummaryView.setTextSize(fontSize);
        mSummaryView.setText(Html.fromHtml(mData.summary));
        mTimeView.setText(TimeUtil.formatTime(getContext(), mData.pubTime));
        if (mData.commentClosed == 0) {
            mCmtView.setText(getResources().getString(R.string.cmt, mData.commentNumber));
        } else {
            mCmtView.setText(R.string.cmt_close);
        }
        if (mData.cached == 0) {
            mTitleView.setTextColor(getResources().getColor(R.color.news_title_color));
            mCacheView.setEnabled(true);
        } else {
            mTitleView.setTextColor(getResources().getColor(R.color.news_cached_color));
            mCacheView.setEnabled(false);
        }
        if (displayLogo) {
            mLogoView.setVisibility(View.VISIBLE);
            mImageLoader.loadPhoto(mData.logo, mLogoView, R.drawable.ic_launcher);
        } else {
            mLogoView.setVisibility(View.GONE);
        }
    }
}
