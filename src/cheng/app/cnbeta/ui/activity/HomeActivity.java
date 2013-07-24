package cheng.app.cnbeta.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBService;
import cheng.app.cnbeta.ui.fragment.HomeListFragment;
import cheng.app.cnbeta.ui.fragment.HomeListFragment.OnLoadListener;
import cheng.app.cnbeta.util.Configs;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.gfan.sdk.statitistics.GFAgent;
import com.viewpagerindicator.TitlePageIndicator;

public class HomeActivity extends SherlockFragmentActivity implements OnPageChangeListener,
    OnClickListener, OnLoadListener {
    static final String TAG = "HomeActivity";
    static final String HOME_TAG = "cbreader_comment_pager_home";
    static final String TOP_TAG = "cbreader_comment_pager_top";
    static final String HM_TAG = "cbreader_comment_pager_hm";
    static final String KEY_JUST_CREATED = "key_just_created";
    static final String KEY_CURRENT_PAGE = "key_current_page";
    int mCurrentPage = 0;
    boolean mIsRecreatedInstance;
    TabsAdapter mAdapter;
    ViewPager mPager;
    HomeListFragment[] mFragments = new HomeListFragment[Configs.HOME_PAGE_NUM];
    boolean mJustCreated = true;
    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mJustCreated = savedInstanceState.getBoolean(KEY_JUST_CREATED, true);
            mCurrentPage =
                    savedInstanceState.getInt(KEY_CURRENT_PAGE, Configs.HOME_PAGE_HOME);
        }
        mIsRecreatedInstance = savedInstanceState != null;
        initViewsAndFragments(savedInstanceState);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Configs.ACTION_NEWS_LOAD_DONE);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Configs.ACTION_NEWS_LOAD_DONE)) {
                    boolean success = intent.getBooleanExtra(Configs.EXTRA_SUCCESS, false);
                    if (success) {
                        Toast.makeText(context, R.string.cache_success,
                                Toast.LENGTH_SHORT).show();
                        mFragments[mCurrentPage].onLoadCache();
                    } else {
                        Toast.makeText(context, R.string.cache_fail,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        mLocalBroadcastManager.registerReceiver(mReceiver, filter);
        GFAgent.setReportUncaughtExceptions(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState != null) {
            outState.putBoolean(KEY_JUST_CREATED, mJustCreated);
            outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        }
        super.onSaveInstanceState(outState);
    }

    private void initViewsAndFragments(Bundle savedState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.tabs);
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        setSupportProgressBarIndeterminateVisibility(false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            BitmapDrawable bg = (BitmapDrawable)getResources().getDrawable(R.drawable.bg_striped);
            bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setBackgroundDrawable(bg);

            BitmapDrawable bgSplit = (BitmapDrawable)getResources().getDrawable(R.drawable.bg_striped);
            bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setSplitBackgroundDrawable(bgSplit);
        }

        mAdapter = new TabsAdapter(fragmentManager);
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(2);
        mPager.setAdapter(mAdapter);
        //mPager.setOnPageChangeListener(this);
        mFragments[Configs.HOME_PAGE_HOME] =
                (HomeListFragment) fragmentManager.findFragmentByTag(HOME_TAG);
        mFragments[Configs.HOME_PAGE_TOP] =
                (HomeListFragment) fragmentManager.findFragmentByTag(TOP_TAG);
        mFragments[Configs.HOME_PAGE_HM] =
                (HomeListFragment) fragmentManager.findFragmentByTag(HM_TAG);
        if (mFragments[Configs.HOME_PAGE_HOME] == null) {
            newFragment(Configs.HOME_PAGE_HOME);
            newFragment(Configs.HOME_PAGE_TOP);
            newFragment(Configs.HOME_PAGE_HM);

            transaction.add(R.id.pager, mFragments[Configs.HOME_PAGE_HOME], HOME_TAG);
            transaction.add(R.id.pager, mFragments[Configs.HOME_PAGE_TOP], TOP_TAG);
            transaction.add(R.id.pager, mFragments[Configs.HOME_PAGE_HM], HM_TAG);
        }
        //PagerTabStrip tabs = (PagerTabStrip) findViewById(R.id.tabs);
        //tabs.setTabIndicatorColorResource(R.color.holo_blue_light);
        //tabs.setBackgroundResource(R.drawable.tab_bg);
        TitlePageIndicator indicator = (TitlePageIndicator)findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        indicator.setBackgroundResource(R.drawable.tab_bg);
        indicator.setOnPageChangeListener(this);
        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
    }
    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        mPager.setCurrentItem(mCurrentPage);
    }

    @Override
    protected void onResume() {
        if (mJustCreated) {
            mJustCreated = false;
        } else {
            mFragments[mCurrentPage].onLoadCache();
        }
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
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
        stopService(new Intent(this, CBService.class));
    }

    @Override
    public void onLoadStateUpdate() {
        boolean isLoading = mFragments[Configs.HOME_PAGE_HOME].isLoading()
                || mFragments[Configs.HOME_PAGE_TOP].isLoading()
                || mFragments[Configs.HOME_PAGE_HM].isLoading();
        if (isLoading) {
            setTitle(R.string.loading);
            //getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
            setSupportProgressBarIndeterminateVisibility(true);
        } else {
            setTitle(R.string.app_name);
            //getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
            setSupportProgressBarIndeterminateVisibility(false);
        }
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    private void newFragment(int page) {
        mFragments[page] = new HomeListFragment();
        Bundle args = new Bundle();
        args.putInt(Configs.EXTRA_PAGE, page);
        mFragments[page].setArguments(args);
        mFragments[page].setOnLoadListener(this);
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        SubMenu subMenu1 = menu.addSubMenu(R.string.more);
//        subMenu1.add(0, 1, 0, R.string.offline);
//        subMenu1.add(0, 2, 0, R.string.setting);
//
//        MenuItem subMenu1Item = subMenu1.getItem();
//        subMenu1Item.setIcon(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
//        subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, 1, 0, R.string.refresh)
        //.setIcon(R.drawable.ic_refresh_holo_dark)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, 3, 0, R.string.setting)
        //.setIcon(R.drawable.ic_bookmarks_history_holo_dark)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, 2, 0, R.string.offline)
        //.setIcon(R.drawable.ic_refresh_holo_dark)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                mFragments[mCurrentPage].onLoadNet(true);
                return true;
            case 2:
                Intent intent = new Intent(this, CacheActivity.class);
                startActivity(intent);
                return true;
            case 3:
                startActivity(new Intent(this, SettingActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class TabsAdapter extends PagerAdapter {
        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;
        private Fragment mCurrentPrimaryItem;

        public TabsAdapter(FragmentManager fm) {
            mFragmentManager = fm;
        }

        @Override
        public int getCount() {
            return Configs.HOME_PAGE_NUM;
        }

        @Override
        public int getItemPosition(Object object) {
            if (object == mFragments[Configs.HOME_PAGE_HOME]) {
                return Configs.HOME_PAGE_HOME;
            }
            if (object == mFragments[Configs.HOME_PAGE_TOP]) {
                return Configs.HOME_PAGE_TOP;
            }
            if (object == mFragments[Configs.HOME_PAGE_HM]) {
                return Configs.HOME_PAGE_HM;
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
                case Configs.HOME_PAGE_HOME:
                    return getText(R.string.home);
                case Configs.HOME_PAGE_TOP:
                    return getText(R.string.top);
                case Configs.HOME_PAGE_HM:
                    return getText(R.string.hm);
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

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int position) {
        mCurrentPage = position;
        invalidateOptionsMenu();
        if(!mFragments[mCurrentPage].considerLoadNet()) {
            Log.d(TAG, "switch to page " + position + ", not load from net");
            mFragments[mCurrentPage].onLoadCache();
        }
    }
}
