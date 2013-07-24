package cheng.app.cnbeta.ui.activity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBSQLiteHelper;
import cheng.app.cnbeta.data.CBSQLiteHelper.AccountColumns;
import cheng.app.cnbeta.data.CBSQLiteHelper.TABLES;
import cheng.app.cnbeta.util.Configs;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.gfan.sdk.statitistics.GFAgent;
import com.tencent.weibo.oauthv2.OAuthV2;
import com.tencent.weibo.oauthv2.OAuthV2Client;
import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.sso.SsoHandler;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingActivity extends SherlockPreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "SettingActivity";
    static final int REQUEST_CODE_SINA = 100;
    static final int REQUEST_CODE_TENCENT = 101;

    private CheckBoxPreference mDisplayLogo;
    //private FontSizePreference mFontSize;
    private ListPreference mAutoRefresh;
    private EditTextPreference mCommentName;
    private EditTextPreference mCommentEmail;
    private CheckBoxPreference mCommentTail;
    private PreferenceScreen mAccountSina;
    private PreferenceScreen mAccountTencent;
    //private CheckBoxPreference mSharePic;
    private PreferenceScreen mCleanCache;
    private PreferenceScreen mSuggestion;
    private SharedPreferences mPrefs;
    private SQLiteDatabase mDb;
    boolean mHasSinaAccount;
    boolean mHasTencentAccount;
    SsoHandler mSsoHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.setting);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            BitmapDrawable bg = (BitmapDrawable)getResources().getDrawable(R.drawable.bg_striped);
            bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setBackgroundDrawable(bg);

            BitmapDrawable bgSplit = (BitmapDrawable)getResources().getDrawable(R.drawable.bg_striped);
            bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setSplitBackgroundDrawable(bgSplit);
        }
        addPreferencesFromResource(R.xml.setting);
        mDisplayLogo = (CheckBoxPreference) findPreference(Configs.KEY_DISPLAY_LOGO);
        // mAutoCache = (CheckBoxPreference)
        // findPreference(Configs.KEY_AUTO_CACHE);
        //mFontSize = (FontSizePreference) findPreference(Configs.KEY_FONT_SIZE);
        mAutoRefresh = (ListPreference) findPreference(Configs.KEY_AUTO_REFRESH);
        mCommentName = (EditTextPreference) findPreference(Configs.KEY_COMMENT_NAME);
        mCommentEmail = (EditTextPreference) findPreference(Configs.KEY_COMMENT_EMAIL);
        mCommentTail = (CheckBoxPreference) findPreference(Configs.KEY_COMMENT_TAIL);
        mAccountSina = (PreferenceScreen) findPreference(Configs.KEY_ACCOUNT_SINA);
        mAccountTencent = (PreferenceScreen) findPreference(Configs.KEY_ACCOUNT_TENCENT);
        //mSharePic = (CheckBoxPreference)findPreference(Configs.KEY_SHARE_PIC);
        mCleanCache = (PreferenceScreen) findPreference(Configs.KEY_CLEAN_CACHE);
        mSuggestion = (PreferenceScreen) findPreference(Configs.KEY_SUGGESTION);
        mAutoRefresh.setOnPreferenceChangeListener(this);
        mCommentName.setOnPreferenceChangeListener(this);
        mCommentEmail.setOnPreferenceChangeListener(this);
        CBSQLiteHelper dbHelper = new CBSQLiteHelper(this);
        mDb = dbHelper.getWritableDatabase();
        GFAgent.setReportUncaughtExceptions(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateState();
        GFAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        GFAgent.onPause(this);
    }

    @Override
    protected void onDestroy() {
        if (mDb != null) {
            mDb.close();
            mDb = null;
        }
        super.onDestroy();
    }

    private void updateState() {
        boolean displayLogo = mPrefs.getBoolean(Configs.KEY_DISPLAY_LOGO, true);
        int autoRefresh = mPrefs.getInt(Configs.KEY_AUTO_REFRESH, 0);
        String commentName = mPrefs.getString(Configs.KEY_COMMENT_NAME, "");
        String commentEmail = mPrefs.getString(Configs.KEY_COMMENT_EMAIL, "");
        boolean commentTail = mPrefs.getBoolean(Configs.KEY_COMMENT_TAIL, true);
        Cursor c = null;
        try {
            c = mDb.query(TABLES.ACCOUNT, null, null, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    int type = c.getInt(c.getColumnIndex(AccountColumns.ACCOUNTTYPE));
                    long expire = c.getLong(c.getColumnIndex(AccountColumns.EXPIRESIN));
                    if (type == Configs.ACCOUNT_TYPE_SINA) {
                        updateSinaAccountSummary(expire);
                    } else if (type == Configs.ACCOUNT_TYPE_TENCENT) {
                        String nick = c.getString(c.getColumnIndex(AccountColumns.USERNICK));
                        String name = c.getString(c.getColumnIndex(AccountColumns.USERNAME));
                        updateTencentAccountSummary(nick, name, expire);
                    }
                } while (c.moveToNext());
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "updateState:RuntimeException", e);
            GFAgent.onError(SettingActivity.this, e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        mDisplayLogo.setChecked(displayLogo);
        updateAutoRefreshSummary(autoRefresh);
        updateCommentNameSummary(commentName);
        updateCommentEmailSummary(commentEmail);
        updateCommentTailSummary(commentTail);
    }

    private void updateAutoRefreshSummary(int index) {
        CharSequence[] summaries = getResources().getTextArray(R.array.entries_auto_refresh);
        mAutoRefresh.setSummary(summaries[index]);
    }

    private void updateCommentNameSummary(String name) {
        if (TextUtils.isEmpty(name)) {
            name = getString(R.string.account_hint);
        }
        mCommentName.setSummary(name);
    }

    private void updateCommentEmailSummary(String email) {
        if (TextUtils.isEmpty(email)) {
            email = getString(R.string.account_hint);
        }
        mCommentEmail.setSummary(email);
    }

    private void updateCommentTailSummary(boolean commentTail) {
        mCommentTail.setChecked(commentTail);
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.default_comment_tail));
        sb.append(',');
        sb.append(android.os.Build.MODEL);
        sb.append(',');
        sb.append(android.os.Build.VERSION.RELEASE);
        mCommentTail.setSummary(sb.toString());
    }

    private void updateCleanCacheSummary(int step) {
        switch (step) {
            case 1:
                mCleanCache.setSummary(R.string.clean_step1);
                break;
            case 2:
                mCleanCache.setSummary(R.string.clean_step2);
                break;
            case 3:
                mCleanCache.setSummary(R.string.clean_step3);
                break;
            case 4:
                mCleanCache.setSummary(R.string.clean_step4);
                break;
            case 5:
                mCleanCache.setSummary(R.string.clean_step5);
                break;
            default:
                mCleanCache.setSummary(R.string.clean_cache_info);
                break;
        }
    }
    private void updateTencentAccountSummary(String nick, String name, long expire) {
        String date = "";
        if (expire != 0) {
            date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(expire));
        }
        String expireIn = getString(R.string.expires_in, date);
        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(nick)) {
            mHasTencentAccount = false;
            mAccountTencent.setTitle(R.string.account_tencent);
            mAccountTencent.setSummary(getString(R.string.account_hint));
        } else {
            mHasTencentAccount = true;
            mAccountTencent.setTitle(R.string.tencent_bind);
            mAccountTencent.setSummary(nick + "(@" + name + ")\n" + expireIn);
        }
    }

    private void updateSinaAccountSummary(long expire) {
        if (expire == 0) {
            mHasSinaAccount = false;
            mAccountSina.setSummary(getString(R.string.account_hint));
            mAccountSina.setTitle(R.string.account_sina);
        } else {
            mHasSinaAccount = true;
            String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(expire));
            String expireIn = getString(R.string.expires_in, date);
            mAccountSina.setSummary(expireIn);
            mAccountSina.setTitle(R.string.sina_bind);
        }
    }

    private void removeAccount(final int accountType) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mDb.delete(TABLES.ACCOUNT, AccountColumns.ACCOUNTTYPE + "=" + accountType, null);
                } catch (RuntimeException e) {
                    Log.e(TAG, "RuntimeException in removeAccount:" + accountType);
                }
                if (accountType == Configs.ACCOUNT_TYPE_SINA) {
                    updateSinaAccountSummary(0);
                } else if (accountType == Configs.ACCOUNT_TYPE_TENCENT) {
                    updateTencentAccountSummary(null, null, 0);
                }
            }

        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.remove_account_title).setCancelable(true)
                .setPositiveButton(R.string.yes, listener).setNegativeButton(R.string.no, null)
                .show();
    }

    private void cleanCache() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                new CleanCacheTask().execute();
            }

        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.clean_cache_title).setCancelable(true)
                .setPositiveButton(R.string.yes, listener).setNegativeButton(R.string.no, null)
                .show();
    }

    class CleanCacheTask extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                publishProgress(1);
                mDb.delete(TABLES.NEWS_LIST, null, null);
                mDb.delete(TABLES.HM, null, null);
                mDb.delete(TABLES.TOP, null, null);
                //mDb.delete(TABLES.IMAGE, null, null);
                Thread.sleep(1000);
                publishProgress(2);
                File images = new File(Configs.IMAGE_PATH);
                if (images.exists() && images.canWrite()) {
                    for (File f : images.listFiles()) {
                        f.delete();
                    }
                }
                publishProgress(3);
                File news = new File(Configs.NEWS_PATH);
                if (news.exists() && news.canWrite()) {
                    for (File f : news.listFiles()) {
                        f.delete();
                    }
                }
                publishProgress(4);
                File comments = new File(Configs.COMMENT_PATH);
                if (comments.exists() && comments.canWrite()) {
                    for (File f : comments.listFiles()) {
                        f.delete();
                    }
                }
                publishProgress(5);
                File screenshots = new File(Configs.SCREENSHOT_PATH);
                if (screenshots.exists() && screenshots.canWrite()) {
                    for (File f : screenshots.listFiles()) {
                        f.delete();
                    }
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Exception in cleanCache", e);
                GFAgent.onError(SettingActivity.this, e);
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress = values[0];
            updateCleanCacheSummary(progress);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            updateCleanCacheSummary(0);
            if (result) {
                Toast.makeText(SettingActivity.this, R.string.clean_cache_success,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SettingActivity.this, R.string.clean_cache_fail,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void bindSinaAccount() {
        Weibo mWeibo = Weibo.getInstance(Configs.SINA_APPKEY, Configs.SINA_REDIRECT_URL);
        mSsoHandler = new SsoHandler(SettingActivity.this, mWeibo);
        mSsoHandler.authorize(new AuthDialogListener());
    }

    class AuthDialogListener implements WeiboAuthListener {

        @Override
        public void onComplete(Bundle values) {
            String token = values.getString("access_token");
            String expires_in = values.getString("expires_in");
            Oauth2AccessToken accessToken = new Oauth2AccessToken(token, expires_in);
            if (accessToken.isSessionValid()) {
                long expire = accessToken.getExpiresTime();
                ContentValues v = new ContentValues();
                v.put(AccountColumns.ACCOUNTTYPE, Configs.ACCOUNT_TYPE_SINA);
                v.put(AccountColumns.ACCESSTOKEN, token);
                v.put(AccountColumns.EXPIRESIN, expire);
                mDb.insert(TABLES.ACCOUNT, null, v);
                updateSinaAccountSummary(expire);
                Toast.makeText(getApplicationContext(), R.string.authorize_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.authorize_fail, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            Toast.makeText(getApplicationContext(), "WeiboException: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(WeiboDialogError e) {
            Toast.makeText(getApplicationContext(), "WeiboDialogError: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCancel() {
        }

    }

    private void bindTencentAccount() {
        OAuthV2 oAuth = new OAuthV2(Configs.REDIRECTURL);
        oAuth.setClientId(Configs.CLIENTID);
        oAuth.setClientSecret(Configs.CLIENTSECRET);
        OAuthV2Client.getQHttpClient().shutdownConnection();
        Intent intent = new Intent(this, TencentOAuthV2Authorize.class);
        intent.putExtra("oauth", oAuth);
        startActivityForResult(intent, REQUEST_CODE_TENCENT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean parserTencentResponseData(String responseData) {
        if (!TextUtils.isEmpty(responseData)) {
            String[] tokenArray = responseData.split("&");
            if (tokenArray.length < 4) {
                return false;
            }

            ContentValues values = new ContentValues();
            values.put(AccountColumns.ACCOUNTTYPE, Configs.ACCOUNT_TYPE_TENCENT);
            String name = null;
            String nick = null;
            long expire = 0;
            for (int i = 0; i < tokenArray.length; i++) {
                String token = tokenArray[i];
                String[] s = token.split("=");
                if (s.length < 2) {
                    continue;
                }
                String key = s[0];
                String val = s[1];
                if ("access_token".equals(key)) {
                    values.put(AccountColumns.ACCESSTOKEN, val);
                } else if ("expires_in".equals(key)) {
                    expire = System.currentTimeMillis()
                            + Long.parseLong(val) * 1000;
                    values.put(AccountColumns.EXPIRESIN, expire);
                } else if ("openid".equals(key)) {
                    values.put(AccountColumns.OPENID, val);
                } else if ("openkey".equals(key)) {
                    values.put(AccountColumns.OPENKEY, val);
                } else if ("refresh_token".equals(key)) {
                    values.put(AccountColumns.REFRESHTOKEN, val);
                } else if ("name".equals(key)) {
                    name = val;
                    values.put(AccountColumns.USERNAME, val);
                } else if ("nick".equals(key)) {
                    nick = val;
                    values.put(AccountColumns.USERNICK, val);
                }
            }
            mDb.insert(TABLES.ACCOUNT, null, values);
            updateTencentAccountSummary(nick, name, expire);
            return true;
        }
        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TENCENT) {
            if (resultCode == RESULT_OK) {
                String responseData = data.getStringExtra("response_data");
                if (parserTencentResponseData(responseData)) {
                    Toast.makeText(this, R.string.authorize_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.authorize_fail, Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (Configs.KEY_AUTO_REFRESH.equals(key)) {
            try {
                int value = Integer.parseInt((String) newValue);
                final Editor editor = mPrefs.edit();
                editor.putInt(Configs.KEY_AUTO_REFRESH, value);
                editor.commit();
                updateAutoRefreshSummary(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist setting", e);
            }
        }
        if (Configs.KEY_COMMENT_NAME.equals(key)) {
            String value = (String) newValue;
            final Editor editor = mPrefs.edit();
            editor.putString(Configs.KEY_COMMENT_NAME, value);
            editor.commit();
            updateCommentNameSummary(value);
        }
        if (Configs.KEY_COMMENT_EMAIL.equals(key)) {
            String value = (String) newValue;
            if (!value.equals("") && !value.matches(Configs.EMAIL_REX)) {
                mCommentEmail.setText("");
                Toast.makeText(this, R.string.email_hint, Toast.LENGTH_SHORT).show();
            } else {
                final Editor editor = mPrefs.edit();
                editor.putString(Configs.KEY_COMMENT_EMAIL, value);
                editor.commit();
                updateCommentEmailSummary(value);
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDisplayLogo) {
            final Editor editor = mPrefs.edit();
            editor.putBoolean(Configs.KEY_DISPLAY_LOGO, mDisplayLogo.isChecked());
            editor.commit();
            return true;
        }
        if (preference == mCommentTail) {
            final Editor editor = mPrefs.edit();
            editor.putBoolean(Configs.KEY_COMMENT_TAIL, mCommentTail.isChecked());
            editor.commit();
            return true;
        }
        if (preference == mAccountTencent) {
            if (mHasTencentAccount) {
                removeAccount(Configs.ACCOUNT_TYPE_TENCENT);
            } else {
                bindTencentAccount();
            }
            return true;
        }
        if (preference == mAccountSina) {
            if (mHasSinaAccount) {
                removeAccount(Configs.ACCOUNT_TYPE_SINA);
            } else {
                bindSinaAccount();
            }
            return true;
        }
        if (preference == mCleanCache) {
            cleanCache();
            return true;
        }
        if (preference == mSuggestion) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.suggestion_text));
            sb.append(getString(R.string.version));
            sb.append('_');
            sb.append(android.os.Build.MODEL);
            sb.append('_');
            sb.append(android.os.Build.VERSION.RELEASE);
            sb.append('_');
            ConnectivityManager conManager = (ConnectivityManager) this
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo net = conManager.getActiveNetworkInfo();
            if (net == null || !net.isConnected()) {
                sb.append("nonetwork");
            } else if (net.getType() == ConnectivityManager.TYPE_WIFI) {
                sb.append("wifi");
            } else {
                String apn = net.getExtraInfo();
                if (TextUtils.isEmpty(apn)) {
                    sb.append("null");
                } else {
                    sb.append(apn);
                }
            }
            shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
            startActivity(shareIntent);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
