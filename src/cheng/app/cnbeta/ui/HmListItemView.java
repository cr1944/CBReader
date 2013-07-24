package cheng.app.cnbeta.ui;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBNewsEntry;
import cheng.app.cnbeta.ui.activity.ShareDialogActivity;
import cheng.app.cnbeta.util.Configs;

public class HmListItemView extends LinearLayout implements View.OnClickListener {
    private static final String TAG = "HmListItemView";
    private TextView mTitleView;
    private TextView mCommentView;
    private TextView mNameView;
    private ImageView mShareView;
    CBNewsEntry mData;
    
    public HmListItemView(Context context) {
        super(context);
    }
    public HmListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTitleView = (TextView) findViewById(R.id.hm_item_title);
        mCommentView = (TextView) findViewById(R.id.hm_item_comment);
        mNameView = (TextView) findViewById(R.id.hm_item_name);
        mShareView = (ImageView) findViewById(R.id.hm_item_share);
        mShareView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
        switch (v.getId()) {
            case R.id.hm_item_share: {
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
            String name = mData.name;
            if(TextUtils.isEmpty(name)) {
                name = getResources().getString(R.string.anonymity);
            }
            String url = "http://www.cnbeta.com/articles/" + mData.articleId + ".htm";
            String text = getResources().getString(R.string.share_comment_text,
                    mData.title, url, name, mData.comment);
            Intent intent = new Intent(getContext(), ShareDialogActivity.class);
            intent.putExtra(Configs.EXTRA_BITMAP, result);
            intent.putExtra(Configs.EXTRA_TEXT, text);
            intent.putExtra(Configs.EXTRA_TITLE, R.string.share_comments);
            getContext().startActivity(intent);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setData(CBNewsEntry data, int fontSize) {
        mData = data;
        if (data == null) {
            mTitleView.setText("");
            mNameView.setText("");
            mCommentView.setText(R.string.error);
            return;
        }
        mCommentView.setTextSize(fontSize);
        mCommentView.setText(data.comment);
        String n = data.name;
        if (TextUtils.isEmpty(n)) {
            n = getResources().getString(R.string.anonymity);
        }
        mNameView.setText(getResources().getString(R.string.name, n));
        mTitleView.setText(data.title);
    }
}
