
package cheng.app.cnbeta.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBCommentEntry;
import cheng.app.cnbeta.data.CBSQLiteHelper;
import cheng.app.cnbeta.ui.CommentListItemView;
import cheng.app.cnbeta.ui.CommentListItemView.CommentActionListener;
import cheng.app.cnbeta.ui.MessageDisplayer;
import cheng.app.cnbeta.ui.fragment.CommentListFragment;
import cheng.app.cnbeta.ui.fragment.PostCommentFragment;
import cheng.app.cnbeta.util.Configs;
import cheng.app.cnbeta.util.DataUtil;
import cheng.app.cnbeta.util.HttpUtil;
import cheng.app.cnbeta.util.JSONUtil;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.gfan.sdk.statitistics.GFAgent;
import com.viewpagerindicator.TitlePageIndicator;

import java.util.LinkedList;

public class NewsCommentActivity extends SherlockFragmentActivity implements OnPageChangeListener,
    OnClickListener, CommentActionListener {
    static final String TAG = "NewsCommentActivity";
    static final String ALL_TAG = "cbreader_comment_pager_all";
    static final String HOT_TAG = "cbreader_comment_pager_hot";
    static final String POST_TAG = "cbreader_comment_pager_post";
    long mNewsId;
    String mTitle;
    CommentPageAdapter mAdapter;
    ViewPager mPager;
    CommentListFragment[] mFragments = new CommentListFragment[Configs.CMT_PAGE_NUM];
    PostCommentFragment mPostCommentFragment;
    private boolean mIsRecreatedInstance;
    private SQLiteDatabase mDb;
    private SharedPreferences mPrefs;
    boolean mInAction;
    LinkedList<CBCommentEntry> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            mNewsId = intent.getLongExtra(Configs.EXTRA_ID, 0);
            mTitle = intent.getStringExtra(Configs.EXTRA_TITLE);
        }
        mIsRecreatedInstance = (savedInstanceState != null);
        initViewsAndFragments(savedInstanceState);
        CBSQLiteHelper dbHelper = new CBSQLiteHelper(this);
        mDb = dbHelper.getWritableDatabase();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        GFAgent.setReportUncaughtExceptions(true);
    }

    @Override
    protected void onStart() {
        int refreshType = mPrefs.getInt(Configs.KEY_AUTO_REFRESH, 0);
        boolean refresh = true;
        if (refreshType == 2) {
            refresh = false;
        } else if (refreshType == 0) {
            ConnectivityManager conn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo net = conn.getActiveNetworkInfo();
            if (net == null || !net.isConnected()
                    || net.getType() != ConnectivityManager.TYPE_WIFI) {
                refresh = false;
            }
        }
        new LoadCommentTask().execute(refresh && !mIsRecreatedInstance);
        super.onStart();
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
    protected void onDestroy() {
        super.onDestroy();
        if (mDb != null) {
            mDb.close();
            mDb = null;
        }
    }

    private void initViewsAndFragments(Bundle savedState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.comments_layout);
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        mAdapter = new CommentPageAdapter(fragmentManager);
        mPager = (ViewPager) findViewById(R.id.comment_pager);
        mPager.setAdapter(mAdapter);
        //mPager.setOnPageChangeListener(this);
        mFragments[Configs.CMT_PAGE_ALL] =
                (CommentListFragment) fragmentManager.findFragmentByTag(ALL_TAG);
        mFragments[Configs.CMT_PAGE_HOT] =
                (CommentListFragment) fragmentManager.findFragmentByTag(HOT_TAG);
        mPostCommentFragment =
                (PostCommentFragment) fragmentManager.findFragmentByTag(POST_TAG);
        if (mFragments[Configs.CMT_PAGE_ALL] == null) {
            newFragment(Configs.CMT_PAGE_ALL);
            newFragment(Configs.CMT_PAGE_HOT);

            transaction.add(R.id.comment_pager, mFragments[Configs.CMT_PAGE_ALL], ALL_TAG);
            transaction.add(R.id.comment_pager, mFragments[Configs.CMT_PAGE_HOT], HOT_TAG);
        }
        if (mPostCommentFragment == null) {
            newPostCommentFragment();
        }
        transaction.hide(mPostCommentFragment);
        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
        //PagerTabStrip tabs = (PagerTabStrip) findViewById(R.id.comment_pager_tabs);
        //tabs.setTabIndicatorColorResource(R.color.holo_blue_light);
        //tabs.setBackgroundResource(R.drawable.tab_bg);
        TitlePageIndicator indicator = (TitlePageIndicator)findViewById(R.id.comment_indicator);
        indicator.setViewPager(mPager);
        indicator.setBackgroundResource(R.drawable.tab_bg);

//        View custom = LayoutInflater.from(this).inflate(R.layout.custom_title_view, null);
//        TextView menuText = (TextView) custom.findViewById(R.id.custom_menu_item_text);
//        TextView menuText2 = (TextView) custom.findViewById(R.id.custom_menu_item_text2);
//        menuText.setText(R.string.refresh);
//        menuText2.setText(R.string.new_comment);
//        menuText.setOnClickListener(this);
//        menuText2.setOnClickListener(this);
//        menuText2.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(R.string.comments);
        if (!TextUtils.isEmpty(mTitle)) {
            getSupportActionBar().setSubtitle(mTitle);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setDisplayShowCustomEnabled(true);
        setSupportProgressBarIndeterminateVisibility(false);
//        getSupportActionBar().setCustomView(custom,
//                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
//                ActionBar.LayoutParams.WRAP_CONTENT,
//                Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            BitmapDrawable bg = (BitmapDrawable) getResources().getDrawable(R.drawable.bg_striped);
            bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setBackgroundDrawable(bg);

            BitmapDrawable bgSplit = (BitmapDrawable) getResources().getDrawable(
                    R.drawable.bg_striped);
            bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setSplitBackgroundDrawable(bgSplit);
        }
    }

    @Override
    public void onClick(View v) {
    }

    private void newFragment(int page) {
        mFragments[page] = new CommentListFragment();
        Bundle args = new Bundle();
        args.putInt(Configs.EXTRA_PAGE, page);
        //args.putLong(Configs.EXTRA_ID, mNewsId);
        //args.putString(Configs.EXTRA_TITLE, mTitle);
        mFragments[page].setArguments(args);
    }

    private void newPostCommentFragment() {
        Log.d(TAG, "newPostCommentFragment");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fragment_slide_bottom_enter,
                R.anim.fragment_slide_bottom_exit);
        mPostCommentFragment = new PostCommentFragment();
        Bundle args = new Bundle();
        args.putLong(Configs.EXTRA_ID, mNewsId);
        args.putString(Configs.EXTRA_TITLE, mTitle);
        mPostCommentFragment.setArguments(args);
        ft.add(R.id.new_comment, mPostCommentFragment, POST_TAG);
        ft.hide(mPostCommentFragment);
        ft.commitAllowingStateLoss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 100, 0, R.string.refresh)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, 101, 0, R.string.new_comment)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case 100:
                new LoadCommentTask().execute(true);
                return true;
            case 101:
                openNewComment();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void openNewComment() {
        if (mPostCommentFragment != null && mPostCommentFragment.isHidden()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.fragment_slide_bottom_enter,
                    R.anim.fragment_slide_bottom_exit);
            mPostCommentFragment.setTitle(R.string.new_comment);
            mPostCommentFragment.setTid(-1);
            mPostCommentFragment.refreshValidate();
            ft.show(mPostCommentFragment);
            ft.commit();
        }
    }

    public void openReplyComment(long tid) {
        if (mPostCommentFragment != null && mPostCommentFragment.isHidden()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.fragment_slide_bottom_enter,
                    R.anim.fragment_slide_bottom_exit);
            mPostCommentFragment.setTitle(R.string.reply_comment);
            mPostCommentFragment.setTid(tid);
            mPostCommentFragment.refreshValidate();
            ft.show(mPostCommentFragment);
            ft.commit();
        }
    }

    public boolean closeComment() {
        if (mPostCommentFragment != null && !mPostCommentFragment.isHidden()) {
            mPostCommentFragment.setTid(-1);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.fragment_slide_bottom_enter,
                R.anim.fragment_slide_bottom_exit);
            ft.hide(mPostCommentFragment);
            ft.commit();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (closeComment())
            return;
        super.onBackPressed();
    }

    @Override
    public void onAction(int action, long tid) {
        if (action == CommentListItemView.TYPE_REPLY) {
            openReplyComment(tid);
        } else if (!mInAction) {
            new ActionTask(action).execute(tid);
        }
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int arg0) {

    }

    class CommentPageAdapter extends PagerAdapter {
        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;
        private Fragment mCurrentPrimaryItem;

        public CommentPageAdapter(FragmentManager fm) {
            mFragmentManager = fm;
        }

        @Override
        public int getCount() {
            return Configs.CMT_PAGE_NUM;
        }
        @Override
        public int getItemPosition(Object object) {
            if (object == mFragments[Configs.CMT_PAGE_ALL]) {
                return Configs.CMT_PAGE_ALL;
            }
            if (object == mFragments[Configs.CMT_PAGE_HOT]) {
                return Configs.CMT_PAGE_HOT;
            }
            return POSITION_NONE;
        }

        @Override
        public void startUpdate(ViewGroup container) {
        }

        private Fragment getFragment(int position) {
            return mFragments[position];
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            Fragment f = getFragment(position);
            mCurTransaction.show(f);

            // Non primary pages are not visible.
            f.setUserVisibleHint(f == mCurrentPrimaryItem);
            return f;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.hide((Fragment) object);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            if (mCurTransaction != null) {
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case Configs.CMT_PAGE_ALL:
                    return getText(R.string.all_comment);
                case Configs.CMT_PAGE_HOT:
                    return getText(R.string.hot_comment);
                default:
                    return null;
            }
        }
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            Fragment fragment = (Fragment) object;
            if (mCurrentPrimaryItem != fragment) {
                if (mCurrentPrimaryItem != null) {
                    mCurrentPrimaryItem.setUserVisibleHint(false);
                }
                if (fragment != null) {
                    fragment.setUserVisibleHint(true);
                }
                mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }
    }

    public void support(long tid) {
        if (!mList.isEmpty()) {
            int size = mList.size();
            for (int i = 0; i < size; i++) {
                CBCommentEntry item = mList.get(i);
                if (item.tid == tid) {
                    item.support += 1;
                    mList.set(i, item);
                    break;
                }
            }
        }
        mFragments[Configs.CMT_PAGE_ALL].notifyDataChange();
        mFragments[Configs.CMT_PAGE_HOT].notifyDataChange();
    }

    public void aggainst(long tid) {
        if (!mList.isEmpty()) {
            int size = mList.size();
            for (int i = 0; i < size; i++) {
                CBCommentEntry item = mList.get(i);
                if (item.tid == tid) {
                    item.against += 1;
                    mList.set(i, item);
                    break;
                }
            }
        }
        mFragments[Configs.CMT_PAGE_ALL].notifyDataChange();
        mFragments[Configs.CMT_PAGE_HOT].notifyDataChange();
    }

    class ActionTask extends AsyncTask<Long, Void, String> {
        int mType;
        long mTid;
        ActionTask(int type) {
            mType = type;
        }
        @Override
        protected void onPreExecute() {
            mInAction = true;
        }
        @Override
        protected String doInBackground(Long... params) {
            mTid = params[0];
            String result = null;
            switch (mType) {
                case CommentListItemView.TYPE_REPLY:
                    break;
                case CommentListItemView.TYPE_SUPPORT:
                    result = HttpUtil.getInstance().httpGet(Configs.SUPPORT_URL + mTid);
                    break;
                case CommentListItemView.TYPE_AGAINST:
                    result = HttpUtil.getInstance().httpGet(Configs.AGGAINST_URL + mTid);
                    break;
                case CommentListItemView.TYPE_REPORT:
                    result = HttpUtil.getInstance().httpGet(Configs.REPORT_URL + mTid);
                    break;
            }
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            mInAction = false;
            if (TextUtils.isEmpty(result)) {
                Toast.makeText(NewsCommentActivity.this, R.string.result_empty, Toast.LENGTH_SHORT).show();
            } else {
                switch (mType) {
                    case CommentListItemView.TYPE_SUPPORT:
                        if (result.substring(0, 1).equals("0")) {
                            support(mTid);
                            Toast.makeText(NewsCommentActivity.this, R.string.vote_success, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(NewsCommentActivity.this, R.string.vote_fail, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case CommentListItemView.TYPE_AGAINST:
                        if (result.substring(0, 1).equals("0")) {
                            aggainst(mTid);
                            Toast.makeText(NewsCommentActivity.this, R.string.vote_success, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(NewsCommentActivity.this, R.string.vote_fail, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case CommentListItemView.TYPE_REPORT:
                        if (result.substring(0, 1).equals("0")) {
                            Toast.makeText(NewsCommentActivity.this, R.string.report_success, Toast.LENGTH_SHORT).show();
                        } else if (result.substring(0, 1).equals("1")) {
                            Toast.makeText(NewsCommentActivity.this, R.string.report_fail_1, Toast.LENGTH_SHORT).show();
                        } else if (result.substring(0, 1).equals("2")) {
                            Toast.makeText(NewsCommentActivity.this, R.string.report_fail_2, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(NewsCommentActivity.this, R.string.report_fail_3, Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        }
    }

    class LoadCommentTask extends AsyncTask<Boolean, Void, LinkedList<CBCommentEntry>> {

        @Override
        protected void onPreExecute() {
            setTitle(R.string.loading);
            getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
            setSupportProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected LinkedList<CBCommentEntry> doInBackground(Boolean... params) {
            boolean forceReload = params[0];
            if (mNewsId < 1) {
                runOnUiThread(new MessageDisplayer(NewsCommentActivity.this, R.string.url_empty));
                return null;
            }
            String result = DataUtil.readComments(mNewsId, mDb, forceReload);
            if (TextUtils.isEmpty(result)) {
                runOnUiThread(new MessageDisplayer(NewsCommentActivity.this, R.string.result_empty));
                return null;
            }
            LinkedList<CBCommentEntry> list = null;
            list = JSONUtil.parseComments(mNewsId, mTitle, result);
            if (list == null) {
                runOnUiThread(new MessageDisplayer(NewsCommentActivity.this, R.string.parser_error));
                return null;
            }
            return list;
        }

        @Override
        protected void onPostExecute(LinkedList<CBCommentEntry> result) {
            if (isFinishing()) {
                return;
            }
            mList = result;
            setTitle(R.string.comments);
            if (mList != null && !mList.isEmpty()) {
                DataUtil.updateCommentNumber(mNewsId, mList.size(), mDb);
                mFragments[Configs.CMT_PAGE_ALL].setData(mList);
                mFragments[Configs.CMT_PAGE_HOT].setHotData(mList);
            }
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
            setSupportProgressBarIndeterminateVisibility(false);
        }
    }
}
