package cheng.app.cnbeta.ui.activity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBSQLiteHelper;
import cheng.app.cnbeta.data.CBSQLiteHelper.NewsColumns;
import cheng.app.cnbeta.data.CBSQLiteHelper.TABLES;
import cheng.app.cnbeta.ui.CacheListItemView;
import cheng.app.cnbeta.util.Configs;
import cheng.app.cnbeta.util.DataUtil;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gfan.sdk.statitistics.GFAgent;

import java.util.HashSet;

public class CacheActivity extends SherlockActivity implements OnItemClickListener,
    OnItemLongClickListener {
    static final String TAG = "CacheActivity";
    static final String KEY_ACTION_MODE = "key_action_mode";
    ActionMode mMode;
    ListView mListView;
    CacheAdapter mAdapter;
    boolean mActionMode;
    private SQLiteDatabase mDb;
    private HashSet<Long> mSelectedIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mActionMode = savedInstanceState.getBoolean(KEY_ACTION_MODE, false);
        }
        setContentView(R.layout.fragment_pager_list);
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        View emptyView = findViewById(android.R.id.empty);
        mListView.setEmptyView(emptyView);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        mAdapter = new CacheAdapter(this);
        mListView.setAdapter(mAdapter);
        CBSQLiteHelper dbHelper = new CBSQLiteHelper(this);
        if (mDb == null) {
            mDb = dbHelper.getWritableDatabase();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.offline);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            BitmapDrawable bg = (BitmapDrawable)getResources().getDrawable(R.drawable.bg_striped);
            bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setBackgroundDrawable(bg);

            BitmapDrawable bgSplit = (BitmapDrawable)getResources().getDrawable(R.drawable.bg_striped);
            bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setSplitBackgroundDrawable(bgSplit);
        }
        GFAgent.setReportUncaughtExceptions(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        GFAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        GFAgent.onPause(this);
    }

    @Override
    protected void onStart() {
        new LoadTask().execute();
        if (mActionMode) {
            mMode = startActionMode(new AnActionModeOfEpicProportions());
        }
        super.onStart();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState != null) {
            outState.putBoolean(KEY_ACTION_MODE, mActionMode);
        }
        super.onSaveInstanceState(outState);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //CacheListItemView v = (CacheListItemView) view;
        //v.setChecked(mListView.isItemChecked(position));
        Cursor c = (Cursor)mListView.getItemAtPosition(position);
        long articleId = c.getLong(c.getColumnIndex(NewsColumns.ARTICLE_ID));
        if (!mActionMode) {
            String title = c.getString(c.getColumnIndex(NewsColumns.TITLE));
            int commentClosed = c.getInt(c.getColumnIndex(NewsColumns.CMT_CLOSED));
            int commentNumber = c.getInt(c.getColumnIndex(NewsColumns.CMT_NUMBER));
            Intent intent = new Intent(this, NewsDetialActivity.class);
            intent.putExtra(Configs.EXTRA_ID, articleId);
            intent.putExtra(Configs.EXTRA_TITLE, title);
            intent.putExtra(Configs.EXTRA_NUMBER, commentClosed == 0 ? commentNumber : -1);
            startActivity(intent);
            mListView.setItemChecked(position, false);
            return;
        }
        if (mListView.isItemChecked(position)) {
            mSelectedIds.add(articleId);
        } else {
            mSelectedIds.remove(articleId);
        }
        mAdapter.notifyDataSetChanged();
        Log.i(TAG, "onItemClick");
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!mActionMode) {
            mActionMode = true;
            mMode = startActionMode(new AnActionModeOfEpicProportions());
            mListView.setLongClickable(false);
            mListView.setItemChecked(position, true);
            mAdapter.notifyDataSetChanged();
            if (mSelectedIds == null) {
                mSelectedIds = new HashSet<Long>();
            }
            Cursor c = (Cursor)mListView.getItemAtPosition(position);
            mSelectedIds.clear();
            mSelectedIds.add(c.getLong(c.getColumnIndex(NewsColumns.ARTICLE_ID)));
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        super.onBackPressed();
    }

    private void checkAll(boolean checked) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            mListView.setItemChecked(i, checked);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void showConfirmDeleteDialog() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                new DeleteThread().start();
            }
            
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title)
            .setCancelable(true)
            .setPositiveButton(R.string.yes, listener)
            .setNegativeButton(R.string.no, null)
            .show();

    }

    private class DeleteThread extends Thread {

        @Override
        public void run() {
            if (mSelectedIds == null || mSelectedIds.isEmpty()) {
                Log.d(TAG, "nothing selected, just return");
                return;
            }
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(NewsColumns.ARTICLE_ID + " IN ( ");
                for (long id : mSelectedIds) {
                    if (DataUtil.removeNews(id, mDb)) {
                        sb.append(id);
                        sb.append(',');
                    }
                }
                sb.replace(sb.length() - 1, sb.length(), ")");
                ContentValues v = new ContentValues();
                v.put(NewsColumns.CACHED, 0);
                mDb.update(TABLES.NEWS_LIST, v, sb.toString(), null);
            } catch (RuntimeException ex) {
                Log.e(TAG, "DeleteThread: RuntimeException", ex);
                GFAgent.onError(CacheActivity.this, ex);
            }
            runOnUiThread(new updateUI());
        }
    }

    private class updateUI implements Runnable {

        @Override
        public void run() {
            if (!isFinishing()) {
                new LoadTask().execute();
            }
        }
        
    }
    private final class AnActionModeOfEpicProportions implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(R.string.del);

            menu.add(0, 1, 0, R.string.del)
                .setIcon(R.drawable.ic_menu_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case 1:
                    showConfirmDeleteDialog();
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            checkAll(false);
            mActionMode = false;
            mListView.setLongClickable(true);
        }
    }

    private Cursor loadCache() {
        Cursor c = null;
        try {
            String sel = NewsColumns.CACHED + "=1";
            c = mDb.query(TABLES.NEWS_LIST, null, sel, null, null, null,
                    NewsColumns.ARTICLE_ID + " DESC");
        } catch (RuntimeException ex) {
            Log.e(TAG, "loadCache: RuntimeException", ex);
            GFAgent.onError(CacheActivity.this, ex);
        }
        return c;
    }

    private class LoadTask extends AsyncTask<Void, Void, Cursor> {
        @Override
        protected Cursor doInBackground(Void... params) {
            return loadCache();
        }
        @Override
        protected void onPostExecute(Cursor result) {
            if (result != null && result.getCount() >= 0) {
                mAdapter.changeCursor(result);
            }
        }
    }

    private class CacheAdapter extends CursorAdapter {

        public CacheAdapter(Context context) {
            super(context, null, false);
        }

        @Override
        public void bindView(View itemView, Context context, Cursor c) {
            CacheListItemView view = (CacheListItemView) itemView;
            view.setData(c);
            view.setChecked(mListView.isItemChecked(c.getPosition()));
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            LayoutInflater vi =
                    (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = vi.inflate(R.layout.cache_item, parent, false);
            return view;
        }
        
    }
}
