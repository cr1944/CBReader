
package cheng.app.cnbeta.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBSQLiteHelper;
import cheng.app.cnbeta.ui.ScreenShotTask;
import cheng.app.cnbeta.ui.activity.NewsCommentActivity;
import cheng.app.cnbeta.ui.activity.ShareDialogActivity;
import cheng.app.cnbeta.util.Configs;
import cheng.app.cnbeta.util.HttpUtil;
import cheng.app.cnbeta.util.ImageUtil;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import java.util.HashMap;

public class PostCommentFragment extends SherlockFragment implements OnClickListener {
    static final String TAG = "PostCommentFragment";
    ImageView mValidate;
    TextView mTitleView;
    EditText mName;
    EditText mEmail;
    EditText mComment;
    EditText mValidateText;
    CheckBox mSyncCheckbox;
    long mNewsId;
    long mTid = -1;
    String mTitle;
    boolean mIsRefreshing = false;
    NewsCommentActivity mContext;
    private SharedPreferences mPrefs;
    private SQLiteDatabase mDb;

    public PostCommentFragment() {
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mTitle = bundle.getString(Configs.EXTRA_TITLE);
            mNewsId = bundle.getLong(Configs.EXTRA_ID);
        }
        mContext = (NewsCommentActivity) getActivity();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        CBSQLiteHelper dbHelper = new CBSQLiteHelper(mContext);
        mDb = dbHelper.getWritableDatabase();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mDb != null) {
            mDb.close();
            mDb = null;
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.new_comment_layout, container, false);
        mValidate = (ImageView) v.findViewById(R.id.new_comment_security_img);
        mTitleView = (TextView) v.findViewById(R.id.new_comment_title);
        mName = (EditText) v.findViewById(R.id.new_comment_name);
        mEmail = (EditText) v.findViewById(R.id.new_comment_email);
        mComment = (EditText) v.findViewById(R.id.new_comment_text);
        mValidateText = (EditText) v.findViewById(R.id.new_comment_security);
        mSyncCheckbox = (CheckBox) v.findViewById(R.id.new_comment_sync);
        v.findViewById(R.id.new_comment_close).setOnClickListener(this);
        v.findViewById(R.id.new_comment_refresh).setOnClickListener(PostCommentFragment.this);
        v.findViewById(R.id.new_comment_submit).setOnClickListener(PostCommentFragment.this);
        String commentName = mPrefs.getString(Configs.KEY_COMMENT_NAME, "");
        String commentEmail = mPrefs.getString(Configs.KEY_COMMENT_EMAIL, "");
        mName.setText(commentName);
        mEmail.setText(commentEmail);
        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_comment_refresh:
                refreshValidate();
                break;
            case R.id.new_comment_submit:
                hideInputMethod();
                if (checkPost()) {
                    new PostCommentTask().execute();
                }
                break;
            case R.id.new_comment_close:
                mContext.closeComment();
                break;
        }
    }

    private void hideInputMethod() {
        View view = mContext.getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void setTid(long tid) {
        mTid = tid;
    }

    public void setTitle(int textRes) {
        mTitleView.setText(textRes);
    }

    public void refreshValidate() {
        if (!mIsRefreshing) {
            new LoadValidateTask().execute();
        }
    }

    private boolean checkPost() {
        if (TextUtils.isEmpty(mComment.getText().toString())) {
            Toast.makeText(getActivity(), R.string.comment_hint, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(mValidateText.getText().toString())) {
            Toast.makeText(getActivity(), R.string.validate_hint, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!TextUtils.isEmpty(mEmail.getText().toString())) {
            if (!mEmail.getText().toString().matches(Configs.EMAIL_REX)) {
                Toast.makeText(getActivity(), R.string.email_hint, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private String postComment() {
        String subject = "";//mSubject.getText().toString();
        String email = mEmail.getText().toString();
        String name = mName.getText().toString();
        StringBuilder comment = new StringBuilder();
        comment.append(mComment.getText());
        //String tail = mPrefs.getString(Configs.KEY_COMMENT_TAIL,
        //        getString(R.string.default_comment_tail));
        boolean appendTail = mPrefs.getBoolean(Configs.KEY_COMMENT_TAIL, true);
        if (appendTail) {
            comment.append('\n');
            comment.append('[');
            comment.append(getString(R.string.default_comment_tail));
            comment.append(',');
            comment.append(android.os.Build.MODEL);
            comment.append(',');
            comment.append(android.os.Build.VERSION.RELEASE);
            comment.append(']');
        }
        String validate = mValidateText.getText().toString();
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("tid", mTid <= 0 ? "0" : String.valueOf(mTid));
        params.put("sid", String.valueOf(mNewsId));
        params.put("valimg_main", validate);
        params.put("nowname", HttpUtil.escape(name));
        params.put("comment", HttpUtil.escape(comment.toString()));
        params.put("nowsubject", subject);
        params.put("nowemail", email);
        String result = HttpUtil.getInstance().httpPost(Configs.POST_COMMENT_URL, params, "utf-8");
        HttpUtil.getInstance().reset();
        return result;
    }

    class PostCommentTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            mContext.getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
            mContext.setSupportProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected String doInBackground(String... params) {
            //String shot = params[0];
            return postComment();
        }

        @Override
        protected void onPostExecute(String result) {
            mContext.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
            mContext.setSupportProgressBarIndeterminateVisibility(false);
            String message = null;
            char type;
            if (TextUtils.isEmpty(result)) {
                type = ' ';
            } else {
                type = result.charAt(0);
            }
            boolean isDone = true;
            switch (type) {
                case '0':
                    message = getString(R.string.result_0);
                    break;
                case '1':
                    isDone = false;
                    message = getString(R.string.result_1);
                    break;
                case '2':
                    message = getString(R.string.result_2);
                    break;
                case '3':
                    isDone = false;
                    message = getString(R.string.result_3);
                    break;
                case '4':
                    isDone = false;
                    message = getString(R.string.result_4);
                    break;
                case '5':
                    message = getString(R.string.result_5);
                    boolean sync = mSyncCheckbox.isChecked();
                    if (sync) {
                        new ShotTask(mComment).execute();
                    }
                    break;
                case '6':
                    isDone = false;
                    message = getString(R.string.result_6);
                    break;
                case '7':
                    message = getString(R.string.result_7);
                    break;
                case '8':
                    message = getString(R.string.result_8);
                    break;
                case '9':
                    message = getString(R.string.result_9);
                    break;
                default:
                    message = getString(R.string.result_unknown, result);
                    break;
            }
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            if (isDone) {
                mContext.closeComment();
            }
        }
    }

    private class ShotTask extends ScreenShotTask {

        public ShotTask(View traget) {
            super(traget);
        }

        @Override
        protected void onFinish(byte[] result) {
            String name = mName.getText().toString();
            if(TextUtils.isEmpty(name)) {
                name = getResources().getString(R.string.anonymity);
            }
            String comment = mComment.getText().toString();
            String url = "http://www.cnbeta.com/articles/" + mNewsId + ".htm";
            String mText = getResources().getString(R.string.share_comment_text,
                    mTitle, url, name, comment);
            Intent intent = new Intent(mContext, ShareDialogActivity.class);
            intent.putExtra(Configs.EXTRA_BITMAP, result);
            intent.putExtra(Configs.EXTRA_TEXT, mText);
            intent.putExtra(Configs.EXTRA_TITLE, R.string.share_comments);
            mContext.startActivity(intent);
        }
    }

    class LoadValidateTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected void onPreExecute() {
            mIsRefreshing = true;
            mContext.getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
            mContext.setSupportProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                // return ImageUtil.getBitmapFromUrl(Configs.VALIDATE_URL);
                byte[] b = HttpUtil.getInstance().httpGetByte(Configs.VALIDATE_URL);
                return ImageUtil.byteToBitmap(b);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            mIsRefreshing = false;
            if (mValidate != null && result != null) {
                mValidate.setImageBitmap(result);
            }
            mContext.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
            mContext.setSupportProgressBarIndeterminateVisibility(false);
        }
    }
}
