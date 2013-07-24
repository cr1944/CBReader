package cheng.app.cnbeta.ui;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBCommentEntry;
import cheng.app.cnbeta.ui.activity.ShareDialogActivity;
import cheng.app.cnbeta.util.Configs;
import cheng.app.cnbeta.util.TimeUtil;

public class CommentListItemView extends LinearLayout implements View.OnClickListener{
    private static final String TAG = "CommentListItemView";
    TextView mFloor;
    TextView mName;
    TextView mText;
    TextView mReply;
    TextView mReport;
    TextView mSupport;
    TextView mAgainst;
    ImageView mShareView;
    CBCommentEntry mData;
    private CommentActionListener mListener;
    public static final int TYPE_REPLY = 0;
    public static final int TYPE_SUPPORT = 1;
    public static final int TYPE_AGAINST = 2;
    public static final int TYPE_REPORT = 3;

    public interface CommentActionListener {
        public void onAction(int action, long tid);
    }
    public void setListener(CommentActionListener listener) {
        mListener = listener;
    }

    public CommentListItemView(Context context) {
        super(context);
        init(context);
    }
    public CommentListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFloor = (TextView) findViewById(R.id.comment_floor);
        mName = (TextView) findViewById(R.id.comment_name);
        mText = (TextView) findViewById(R.id.comment_text);
        mReply = (TextView) findViewById(R.id.comment_reply);
        mReport = (TextView) findViewById(R.id.comment_report);
        mSupport = (TextView) findViewById(R.id.comment_support);
        mAgainst = (TextView) findViewById(R.id.comment_against);
        mShareView = (ImageView) findViewById(R.id.comment_share);
        mShareView.setOnClickListener(this);
        mReply.setOnClickListener(this);
        mReport.setOnClickListener(this);
        mSupport.setOnClickListener(this);
        mAgainst.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
        if (mData == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.comment_share: {
                new ShotTask(this).execute();
                break;
            }
            case R.id.comment_reply:
                doAction(TYPE_REPLY);
                break;
            case R.id.comment_support:
                doAction(TYPE_SUPPORT);
                break;
            case R.id.comment_against:
                doAction(TYPE_AGAINST);
                break;
            case R.id.comment_report:
                doAction(TYPE_REPORT);
                break;
        }
    }
    private void doAction(int action) {
        if (mListener != null) {
            mListener.onAction(action, mData.tid);
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
            String url = "http://www.cnbeta.com/articles/" + mData.newsId + ".htm";
            String mText = getResources().getString(R.string.share_comment_text,
                    mData.title, url, name, mData.comment);
            Intent intent = new Intent(getContext(), ShareDialogActivity.class);
            intent.putExtra(Configs.EXTRA_BITMAP, result);
            intent.putExtra(Configs.EXTRA_TEXT, mText);
            intent.putExtra(Configs.EXTRA_TITLE, R.string.share_comments);
            getContext().startActivity(intent);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setData(CBCommentEntry data, int position) {
        mData = data;
        if (position != -1) {
            mFloor.setText(getContext().getString(R.string.floor, position + 1));
        }
        if (data == null) {
            mName.setText("");
            mText.setText(R.string.error);
            mSupport.setText("");
            mAgainst.setText("");
            return;
        }
        String name = data.name;
        if (TextUtils.isEmpty(name)) {
            name = getContext().getString(R.string.anonymity);
        }
        String time = TimeUtil.formatTime(getContext(), data.date);
        mName.setText(getContext().getString(R.string.comment_info, name, time));
        mText.setText(data.comment);
        mSupport.setText(getContext().getString(R.string.support, data.support));
        mAgainst.setText(getContext().getString(R.string.against, data.against));
    }
}
