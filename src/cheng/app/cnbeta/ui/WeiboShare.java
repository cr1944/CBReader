
package cheng.app.cnbeta.ui;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBSQLiteHelper;
import cheng.app.cnbeta.data.CBSQLiteHelper.AccountColumns;
import cheng.app.cnbeta.data.CBSQLiteHelper.TABLES;
import cheng.app.cnbeta.util.Configs;
import cheng.app.cnbeta.util.HttpUtil;
import cheng.app.cnbeta.util.ImageUtil;
import cheng.app.cnbeta.util.JSONUtil;

import com.tencent.weibo.api.TAPI;
import com.tencent.weibo.constants.OAuthConstants;
import com.tencent.weibo.oauthv2.OAuthV2;
import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.api.StatusesAPI;
import com.weibo.sdk.android.net.RequestListener;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

public class WeiboShare implements RequestListener {
    static final String TAG = "WeiboShare";

    public static final int RESULT_ERROR = -1;
    public static final int RESULT_NO_COMMENT = 0;
    public static final int RESULT_ACCOUNT_EMPTY = 1;
    public static final int RESULT_OK = 2;
    private final Handler mHandler;
    SQLiteDatabase mDb;
    Context mContext;

    public WeiboShare(Context context) {
        mContext = context;
        CBSQLiteHelper dbHelper = new CBSQLiteHelper(mContext);
        mDb = dbHelper.getWritableDatabase();
        mHandler = new Handler(Looper.getMainLooper());
    }

    private void finish() {
        if (mDb != null) {
            mDb.close();
            mDb = null;
        }
    }

    public void share(Bitmap bitmap, String text) {
        new ShareTencentTask(bitmap, false).execute(text);
    }

    public void shareSina(Bitmap bitmap, String text) {
        new ShareSinaTask(bitmap).execute(text);
    }

    public void shareTencent(Bitmap bitmap, String text) {
        new ShareTencentTask(bitmap, true).execute(text);
    }

    public final void runOnUiThread(Runnable action) {
        mHandler.post(action);
    }

    private class ShareTencentTask extends AsyncTask<String, Void, Void> {
        Bitmap mTargetBitmap;
        boolean mFinish;
        String mText;

        public ShareTencentTask(Bitmap bitmap, boolean finish) {
            mTargetBitmap = bitmap;
            mFinish = finish;
        }

        @Override
        protected void onPreExecute() {
            runOnUiThread(new MessageDisplayer(mContext, R.string.sync_tencent_start));
        }

        @Override
        protected Void doInBackground(String... params) {
            mText = params[0];
            //String path = params[3];
            //String path = ImageUtil.shot(mTargetView);
            String path = ImageUtil.saveScreenshot(mTargetBitmap);
            shareViaTencent(mText, path);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            if (mFinish) {
                finish();
            } else {
                shareSina(mTargetBitmap, mText);
            }
        }

    }

    private class ShareSinaTask extends AsyncTask<String, Void, Void> {
        Bitmap mTargetBitmap;

        public ShareSinaTask(Bitmap bitmap) {
            mTargetBitmap = bitmap;
        }

        @Override
        protected void onPreExecute() {
            runOnUiThread(new MessageDisplayer(mContext, R.string.sync_sina_start));
        }

        @Override
        protected Void doInBackground(String... params) {
            String text = params[0];
            //String comment = params[1];
            //String title = params[2];
            //String path = ImageUtil.shot(mTargetView);
            String path = ImageUtil.saveScreenshot(mTargetBitmap);
            shareViaSina(text, path);
            return null;
        }

    }

    private void shareViaTencent(String text, String path) {
        if (TextUtils.isEmpty(text)) {
            runOnUiThread(new MessageDisplayer(mContext, R.string.no_text_info));
            return;
        }
        String accessToken = null;
        String openId = null;
        String openKey = null;
        Cursor c = null;
        try {
            c = mDb.query(TABLES.ACCOUNT, null, AccountColumns.ACCOUNTTYPE + "="
                    + Configs.ACCOUNT_TYPE_TENCENT, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                accessToken = c.getString(c.getColumnIndex(AccountColumns.ACCESSTOKEN));
                openId = c.getString(c.getColumnIndex(AccountColumns.OPENID));
                openKey = c.getString(c.getColumnIndex(AccountColumns.OPENKEY));
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException in shareViaTencent");
        } finally {
            if (c != null) {
                c.close();
            }
        }
        if (TextUtils.isEmpty(accessToken) || TextUtils.isEmpty(openId)
                || TextUtils.isEmpty(openKey)) {
            runOnUiThread(new MessageDisplayer(mContext, R.string.no_tencent_account_info));
            return;
        }
        OAuthV2 oAuthV2 = new OAuthV2(Configs.REDIRECTURL);
        oAuthV2.setClientId(Configs.CLIENTID);
        oAuthV2.setClientSecret(Configs.CLIENTSECRET);
        oAuthV2.setAccessToken(accessToken);
        oAuthV2.setOpenid(openId);
        oAuthV2.setOpenkey(openKey);
        TAPI tAPI = new TAPI(OAuthConstants.OAUTH_VERSION_2_A);
        String ip = HttpUtil.getInstance().getLocalIPAddress();
        String response = null;
        try {
            String content = text.length() > 140 ? text.substring(0, 140) : text;
            if (!TextUtils.isEmpty(path)) {
                response = tAPI.addPic(oAuthV2, "json", content, ip, path);
            } else {
                response = tAPI.add(oAuthV2, "json", content, ip);
            }
        } catch (Exception e) {
            Log.e(TAG, "tencent api error!");
        }
        tAPI.shutdownConnection();
        if (JSONUtil.parserTencentResponse(response)) {
            runOnUiThread(new MessageDisplayer(mContext, R.string.sync_tencent_success));
            return;
        }
        runOnUiThread(new MessageDisplayer(mContext, R.string.sync_tencent_fail));
    }

    private void shareViaSina(String text, String path) {
        if (TextUtils.isEmpty(text)) {
            runOnUiThread(new MessageDisplayer(mContext, R.string.no_text_info));
            return;
        }
        String accessToken = null;
        long expire = 0;
        Cursor c = null;
        try {
            c = mDb.query(TABLES.ACCOUNT, null, AccountColumns.ACCOUNTTYPE + "="
                    + Configs.ACCOUNT_TYPE_SINA, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                accessToken = c.getString(c.getColumnIndex(AccountColumns.ACCESSTOKEN));
                expire = c.getLong(c.getColumnIndex(AccountColumns.EXPIRESIN));
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException in shareViaSina");
        } finally {
            if (c != null) {
                c.close();
            }
        }
        if (TextUtils.isEmpty(accessToken)) {
            runOnUiThread(new MessageDisplayer(mContext, R.string.no_sina_account_info));
            return;
        }
        Oauth2AccessToken token = new Oauth2AccessToken();
        token.setToken(accessToken);
        token.setExpiresTime(expire);
        if (!token.isSessionValid()) {
            runOnUiThread(new MessageDisplayer(mContext, R.string.no_sina_account_info));
            return;
        }
        StatusesAPI api = new StatusesAPI(token);
        String content = text.length() > 140 ? text.substring(0, 140) : text;
        if (!TextUtils.isEmpty(path)) {
            api.upload(content, path, "0.0", "0.0", this);
        } else {
            api.update(content, "0.0", "0.0", this);
        }
        runOnUiThread(new MessageDisplayer(mContext, R.string.sync_sina_start));
    }

    @Override
    public void onComplete(String response) {
        runOnUiThread(new MessageDisplayer(mContext, R.string.sync_sina_success));
        finish();
    }

    @Override
    public void onIOException(IOException e) {
        runOnUiThread(new MessageDisplayer(mContext, R.string.sync_sina_fail));
        finish();
    }

    @Override
    public void onError(WeiboException e) {
        runOnUiThread(new MessageDisplayer(mContext, R.string.sync_sina_fail));
        finish();
    }
}
