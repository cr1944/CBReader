package cheng.app.cnbeta.ui.activity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.TextSize;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBSQLiteHelper;
import cheng.app.cnbeta.ui.MessageDisplayer;
import cheng.app.cnbeta.util.Configs;
import cheng.app.cnbeta.util.DataUtil;
import cheng.app.cnbeta.util.HttpUtil;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.gfan.sdk.statitistics.GFAgent;

public class NewsDetialActivity extends SherlockFragmentActivity implements OnClickListener {
    private static final String TAG = "NewsDetialActivity";
    long mNewsId;
    int mCmtNumber;
    String mTitle;
    TextView mEmptyView;
    WebView mWebView;
    SQLiteDatabase mDb;
    boolean isDiging;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        Intent intent = getIntent();
        if (intent != null) {
            mNewsId = intent.getLongExtra(Configs.EXTRA_ID, 0);
            mCmtNumber = intent.getIntExtra(Configs.EXTRA_NUMBER, 0);
            mTitle = intent.getStringExtra(Configs.EXTRA_TITLE);
        }
        setTitle(R.string.news_detial);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.news_detial);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            BitmapDrawable bg = (BitmapDrawable)getResources().getDrawable(R.drawable.bg_striped);
            bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setBackgroundDrawable(bg);

            BitmapDrawable bgSplit = (BitmapDrawable)getResources().getDrawable(R.drawable.bg_striped);
            bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setSplitBackgroundDrawable(bgSplit);
        }
        if (mNewsId == 0) {
            Toast.makeText(this, R.string.news_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mEmptyView = (TextView)findViewById(R.id.empty);
        mWebView = (WebView)findViewById(R.id.web);
        mWebView.setBackgroundColor(0);
        WebSettings s = mWebView.getSettings();
        s.setTextSize(TextSize.NORMAL);
        s.setPluginsEnabled(true);
        s.setJavaScriptEnabled(true);
        s.setDefaultTextEncodingName("utf-8");
        s.setPluginState(PluginState.ON);
        mWebView.requestFocus();
        CBSQLiteHelper dbHelper = new CBSQLiteHelper(this);
        mDb = dbHelper.getWritableDatabase();
        new LoadTask().execute(mNewsId);
        GFAgent.setReportUncaughtExceptions(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCmtNumber = DataUtil.readCommentNumber(mNewsId, mDb);
        invalidateOptionsMenu();
        GFAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        GFAgent.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDb != null) {
            mDb.close();
            mDb = null;
        }
    }

    @Override
    public void onClick(View v) {
    }

    private void dig() {
        if (!isDiging) {
            new DigTask().execute();
        }
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String text = getString(R.string.share_news_text, mTitle, "http://www.cnbeta.com/articles/" + mNewsId + ".htm");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        return shareIntent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getSupportMenuInflater().inflate(R.menu.share_action_provider, menu);
        //MenuItem actionItem = menu.findItem(R.id.menu_item_share_action_provider_action_bar);
        //ShareActionProvider actionProvider = (ShareActionProvider) actionItem.getActionProvider();
        //actionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
        //actionProvider.setShareIntent(createShareIntent());
        String title;
        if (mCmtNumber == -1) {
            title = getString(R.string.cmt_close);
        } else {
            title = getString(R.string.view_comment, mCmtNumber);
        }
        menu.add(0, 100, 0, title)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, 101, 0, R.string.dig)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        //menu.add(0, 102, 0, R.string.rating)
        //.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, 103, 0, R.string.share)
        //.setIcon(R.drawable.ic_menu_share)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case 100:
                if (mCmtNumber == -1) {
                    return true;
                }
                Intent intent = new Intent(NewsDetialActivity.this, NewsCommentActivity.class);
                intent.putExtra(Configs.EXTRA_ID, mNewsId);
                intent.putExtra(Configs.EXTRA_TITLE, mTitle);
                startActivity(intent);
                return true;
            case 101:
                dig();
                return true;
            case 102:
                return true;
            case 103:
                startActivity(createShareIntent());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class DigTask extends AsyncTask<Long, Void, String> {
        @Override
        protected void onPreExecute() {
            isDiging = true;
        }
        @Override
        protected String doInBackground(Long... params) {
            return HttpUtil.getInstance().httpGet(Configs.DIG_URL + mNewsId);
        }
        @Override
        protected void onPostExecute(String result) {
            isDiging = false;
            if (TextUtils.isEmpty(result)) {
                Toast.makeText(NewsDetialActivity.this, R.string.result_empty, Toast.LENGTH_SHORT).show();
            } else {
                if (result.substring(0, 1).equals("0")) {
                    Toast.makeText(NewsDetialActivity.this, R.string.dig_result1, Toast.LENGTH_SHORT).show();
                } else if (result.substring(0, 1).equals("1")) {
                    Toast.makeText(NewsDetialActivity.this, R.string.dig_result2, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(NewsDetialActivity.this, R.string.dig_result3, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    class LoadTask extends AsyncTask<Long, Void, String> {
        @Override
        protected void onPreExecute() {
            setTitle(R.string.loading);
            getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
            setSupportProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected String doInBackground(Long... params) {
            long id = params[0];
            if (id < 1) {
                runOnUiThread(new MessageDisplayer(NewsDetialActivity.this, R.string.url_empty));
                return null;
            }
            String result = DataUtil.readNews(id, mDb);
            if (TextUtils.isEmpty(result)) {
                runOnUiThread(new MessageDisplayer(NewsDetialActivity.this, R.string.result_empty));
                return null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            setTitle(R.string.news_detial);
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
            setSupportProgressBarIndeterminateVisibility(false);
            if (!TextUtils.isEmpty(result)) {
                mEmptyView.setVisibility(View.GONE);
                Log.d(TAG, "loadDataWithBaseURL");
                //mWebView.loadUrl("file://" + result);
                //mWebView.loadData(result, "text/html;charset=utf-8", null);
                mWebView.loadDataWithBaseURL(null, result, "text/html", "utf-8", null);
            } else {
                mEmptyView.setText(R.string.no_items);
            }
        }
    }
}
