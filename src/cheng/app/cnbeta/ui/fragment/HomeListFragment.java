package cheng.app.cnbeta.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBNewsEntry;
import cheng.app.cnbeta.data.CBSQLiteHelper;
import cheng.app.cnbeta.ui.HmListItemView;
import cheng.app.cnbeta.ui.MessageDisplayer;
import cheng.app.cnbeta.ui.NewsListItemView;
import cheng.app.cnbeta.ui.LoadingLayout;
import cheng.app.cnbeta.ui.activity.HomeActivity;
import cheng.app.cnbeta.ui.activity.NewsDetialActivity;
import cheng.app.cnbeta.util.Configs;
import cheng.app.cnbeta.util.DataUtil;
import cheng.app.cnbeta.util.HttpUtil;
import cheng.app.cnbeta.util.ImageLoader;
import cheng.app.cnbeta.util.JSONUtil;

import com.actionbarsherlock.app.SherlockListFragment;

import java.util.LinkedList;
import java.util.List;

public class HomeListFragment extends SherlockListFragment implements
    LoaderManager.LoaderCallbacks<List<CBNewsEntry>>, OnClickListener {
    static final String TAG = "HomeListFragment";
    int mPage;
    long mLastId;
    int mTotalItems;
    static final String KEY_TOTAL_ITEM = "key_total_item";
    static final String KEY_FIRST_LOAD = "key_first_load";
    static final String KEY_IS_LOADING = "key_is_loading";
    static final String EXTRA_LAST_ID = "last_id";
    static final String EXTRA_LIMIT = "limit";
    static final String EXTRA_LOAD_FROM_NET = "load_from_net";

    ListAdapter mAdapter;
    LinkedList<CBNewsEntry> mList;
    LoadingLayout mLoadingView;
    ImageLoader mImageLoader;
    HomeActivity mContext;
    ImageView mBackTop;
    private SQLiteDatabase mDb;
    boolean mIsLoading;
    boolean mFirstLoad = true;
    SharedPreferences mPrefs;

    public HomeListFragment() {
    }

    public interface OnLoadListener {
        public void onLoadStateUpdate();
    }
    OnLoadListener mOnLoadListener;
    public void setOnLoadListener(OnLoadListener loadListener) {
        mOnLoadListener = loadListener;
    }
    private void notifyUpdateLoadState(boolean loading) {
        mIsLoading = loading;
        if (mOnLoadListener != null) {
            mOnLoadListener.onLoadStateUpdate();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mTotalItems = savedInstanceState.getInt(KEY_TOTAL_ITEM, 0);
            mFirstLoad = savedInstanceState.getBoolean(KEY_FIRST_LOAD, true);
            mIsLoading = savedInstanceState.getBoolean(KEY_IS_LOADING, false);
        }
        mList = new LinkedList<CBNewsEntry>();
        mPage = getArguments() != null ?
                getArguments().getInt(Configs.EXTRA_PAGE) : Configs.HOME_PAGE_HOME;
        mImageLoader = ImageLoader.getInstance(getActivity());
        mContext = (HomeActivity) getActivity();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        CBSQLiteHelper dbHelper = new CBSQLiteHelper(mContext);
        if (mDb == null) {
            mDb = dbHelper.getWritableDatabase();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDb != null) {
            mDb.close();
            mDb = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mList != null) {
            mTotalItems = mList.size();
        }
        if (outState != null) {
            outState.putInt(KEY_TOTAL_ITEM, mTotalItems);
            outState.putBoolean(KEY_FIRST_LOAD, mFirstLoad);
            outState.putBoolean(KEY_IS_LOADING, mIsLoading);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_pager_list, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mPage != Configs.HOME_PAGE_TOP) {
            mLoadingView = (LoadingLayout) LayoutInflater.from(getActivity())
                    .inflate(R.layout.loading_footer_layout, null);
            getListView().addFooterView(mLoadingView);
            mLoadingView.setLoading(mIsLoading);
            mBackTop = (ImageView) view.findViewById(R.id.btn_backtotop);
            mBackTop.setVisibility(View.VISIBLE);
            mBackTop.setOnClickListener(this);
        }
        mAdapter = new ListAdapter(getActivity(), mList);
        setListAdapter(mAdapter);
        getListView().setOnScrollListener(mAdapter);
        //getListView().setOnCreateContextMenuListener(this);
        onLoadCache();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_backtotop) {
            getListView().setSelection(0);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.i(TAG, "Item clicked: " + id);
        final int total = mList.size();
        if (mPage != Configs.HOME_PAGE_TOP && position == total && !mLoadingView.isLoading()) {
            mLoadingView.setLoading(true);
            onLoadNet(false);
            return;
        }
        CBNewsEntry item = mList.get(position);
        if (item != null) {
            Intent intent = new Intent(mContext, NewsDetialActivity.class);
            intent.putExtra(Configs.EXTRA_ID, item.articleId);
            intent.putExtra(Configs.EXTRA_TITLE, item.title);
            intent.putExtra(Configs.EXTRA_NUMBER, item.commentClosed == 0 ? item.commentNumber : -1);
            startActivity(intent);
        }
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    public boolean considerLoadNet() {
        boolean dataEmpty = mList == null || mList.isEmpty();
        Log.d(TAG, "page " + mPage + " considerLoadNet, dataEmpty="+dataEmpty);
        ConnectivityManager conn =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = conn.getActiveNetworkInfo();
        int autoRefresh = mPrefs.getInt(Configs.KEY_AUTO_REFRESH, 0);
        boolean isWifi = autoRefresh == 0 && net != null && net.isConnected()
                && net.getType() == ConnectivityManager.TYPE_WIFI;
        if (dataEmpty || autoRefresh == 1 || isWifi) {
            onLoadNet(true);
            return true;
        }
        return false;
    }

    public void onLoadNet(boolean isRefresh) {
        Log.d(TAG, "page " + mPage + " onLoadNet: isRefresh="+isRefresh);
        if (mIsLoading) {
            return;
        }
        notifyUpdateLoadState(true);
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_LOAD_FROM_NET, true);
        args.putLong(EXTRA_LAST_ID, isRefresh ? 0 : mLastId);
        args.putInt(EXTRA_LIMIT, 20);
        getLoaderManager().restartLoader(mPage, args, this);
        if (isRefresh) {
            getListView().setSelection(0);
        }
    }

    public void onLoadCache() {
        Log.d(TAG, "page " + mPage + " onLoadCache");
        notifyUpdateLoadState(true);
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_LOAD_FROM_NET, false);
        args.putLong(EXTRA_LAST_ID, 0);
        args.putInt(EXTRA_LIMIT, mTotalItems == 0 ? 20 : mTotalItems);
        getLoaderManager().restartLoader(mPage, args, this);
    }

    @Override
    public Loader<List<CBNewsEntry>> onCreateLoader(int id, Bundle args) {
        return new HomeListLoader(mContext, mDb, id, args);
    }

    @Override
    public void onLoadFinished(Loader<List<CBNewsEntry>> loader, List<CBNewsEntry> data) {
        Log.d(TAG, "page " + mPage +" load finished");
        HomeListLoader homeListloader = (HomeListLoader) loader;
        if (mPage == Configs.HOME_PAGE_HOME) {
            if (homeListloader.getLastId() > 0) {
                if (data == null || data.isEmpty()) {
                    Toast.makeText(mContext, R.string.load_more_empty, Toast.LENGTH_SHORT).show();
                } else {
                    mList.addAll(data);
                    mLastId = mList.getLast().articleId;
                }
            } else {
                if (data == null || data.isEmpty()) {
                } else {
                    mList.clear();
                    mLastId = 0;
                    mList.addAll(data);
                    mLastId = mList.getLast().articleId;
                }
            }
        } else if (mPage == Configs.HOME_PAGE_TOP) {
            if (data == null || data.isEmpty()) {
            } else {
                mList.clear();
                mLastId = 0;
                mList.addAll(data);
                mLastId = mList.getLast().articleId;
            }
        } else if (mPage == Configs.HOME_PAGE_HM) {
            if (homeListloader.getLastId() > 0) {
                if (data == null || data.isEmpty()) {
                    Toast.makeText(mContext, R.string.load_more_empty, Toast.LENGTH_SHORT).show();
                } else {
                    mList.addAll(data);
                    mLastId = mList.getLast().HMID;
                }
            } else {
                if (data == null || data.isEmpty()) {
                } else {
                    mList.clear();
                    mLastId = 0;
                    mList.addAll(data);
                    mLastId = mList.getLast().HMID;
                }
            }
        }
        mAdapter.notifyDataSetChanged();
        if (mLoadingView != null) {
            mLoadingView.setLoading(false);
        }
        notifyUpdateLoadState(false);
        if (mFirstLoad) {
            mFirstLoad = false;
            if (mContext.getCurrentPage() == mPage) {
                Log.d(TAG, "first load, page " + mPage + " is current page");
                considerLoadNet();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<List<CBNewsEntry>> loader) {
        mList.clear();
        mAdapter.notifyDataSetChanged();
    }

    static class HomeListLoader extends AsyncTaskLoader<List<CBNewsEntry>> {
        List<CBNewsEntry> mDatas;
        int mPageId;
        long mLastItemId;
        int mLimit;
        boolean mIsLoadFromNet;
        HomeActivity mContext;
        SQLiteDatabase mDb;

        public HomeListLoader(HomeActivity context, SQLiteDatabase db, int id, Bundle args) {
            super(context);
            mPageId = id;
            mContext = context;
            mDb = db;
            mLastItemId = args.getLong(EXTRA_LAST_ID, 0);
            mLimit = args.getInt(EXTRA_LIMIT, 20);
            mIsLoadFromNet = args.getBoolean(EXTRA_LOAD_FROM_NET, false);
        }

        public long getLastId() {
            return mLastItemId;
        }

        @Override
        public List<CBNewsEntry> loadInBackground() {
            List<CBNewsEntry> result;
            if (mIsLoadFromNet) {
                result = loadFromNet(mLastItemId, mLimit);
            } else {
                result = loadFromDb(mLastItemId, mLimit);
//                if (result == null || result.isEmpty()) {
//                    Log.d(TAG, "loadFromDb: mPageId=" + mPageId +", empty, loadFromNet");
//                    result = loadFromNet(mLastItemId, mLimit);
//                }
            }
            if (result == null) {
                result = new LinkedList<CBNewsEntry>();
            }
            return result;
        }

        private synchronized LinkedList<CBNewsEntry> loadFromDb(long lastId, int limit) {
            String minLimit = String.valueOf(limit);
            LinkedList<CBNewsEntry> result = null;
            Log.d(TAG, "page " + mPageId + " loadFromDb, lastId=" + lastId);
            if (mPageId == Configs.HOME_PAGE_TOP) {
                result = DataUtil.readTop(mDb);
            } else if (mPageId == Configs.HOME_PAGE_HM) {
                result = DataUtil.readHM(lastId, minLimit, mDb);
            } else if (mPageId == Configs.HOME_PAGE_HOME) {
                result = DataUtil.readNewsList(lastId, minLimit, mDb);
            }
            return result;
        }

        private synchronized LinkedList<CBNewsEntry> loadFromNet(long lastId, int limit) {
            Log.d(TAG, "page " + mPageId + " loadFromNet, lastId=" + lastId);
            if (mPageId == Configs.HOME_PAGE_TOP) {
                String html = HttpUtil.getInstance().httpGet(Configs.TOP_URL);
                if (TextUtils.isEmpty(html)) {
                    mContext.runOnUiThread(new MessageDisplayer(mContext, R.string.result_empty));
                    return null;
                }
                LinkedList<CBNewsEntry> list = JSONUtil.parseTop(html);
                if (list == null || list.isEmpty()) {
                    mContext.runOnUiThread(new MessageDisplayer(mContext, R.string.parser_error));
                    return null;
                }
                if (!DataUtil.saveTop(list, mDb)) {
                    mContext.runOnUiThread(new MessageDisplayer(mContext, R.string.save_error));
                    return null;
                }
                return list;
            } else if (mPageId == Configs.HOME_PAGE_HOME) {
                String url = Configs.NEWSLIST_URL + limit;
                if (lastId > 0) {
                    url += (Configs.NEWSLIST_PAGE + lastId);
                }
                String html = HttpUtil.getInstance().httpGet(url);
                if (TextUtils.isEmpty(html)) {
                    mContext.runOnUiThread(new MessageDisplayer(mContext, R.string.result_empty));
                    return null;
                }
                LinkedList<CBNewsEntry> list = JSONUtil.parseNewsList(html);
                if (list == null || list.isEmpty()) {
                    mContext.runOnUiThread(new MessageDisplayer(mContext, R.string.parser_error));
                    return null;
                }
                if (!DataUtil.saveNewsList(list, mDb)) {
                    mContext.runOnUiThread(new MessageDisplayer(mContext, R.string.save_error));
                    return null;
                } else {
                    return loadFromDb(lastId, limit);
                }
            } else if (mPageId == Configs.HOME_PAGE_HM) {
                String url = Configs.HMCOMMENT_URL + limit;
                if (lastId > 0) {
                    url += (Configs.HMCOMMENT_PAGE + lastId);
                }
                String html = HttpUtil.getInstance().httpGet(url);
                if (TextUtils.isEmpty(html)) {
                    mContext.runOnUiThread(new MessageDisplayer(mContext, R.string.result_empty));
                    return null;
                }
                LinkedList<CBNewsEntry> list = JSONUtil.parseHMComment(html);
                if (list == null || list.isEmpty()) {
                    mContext.runOnUiThread(new MessageDisplayer(mContext, R.string.parser_error));
                    return null;
                }
                if (!DataUtil.saveHM(list, mDb)) {
                    mContext.runOnUiThread(new MessageDisplayer(mContext, R.string.save_error));
                    return null;
                }
                return list;
            }
            return null;
        }

        @Override
        public void deliverResult(List<CBNewsEntry> data) {
            if (isReset()) {
                // An async query came in while the loader is stopped.  We
                // don't need the result.
                if (data != null) {
                    onReleaseResources(data);
                }
            }
            List<CBNewsEntry> oldData = data;
            mDatas = data;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(data);
            }

            if (oldData != null) {
                onReleaseResources(oldData);
            }
        }

        @Override
        protected void onStartLoading() {
            if (mDatas != null) {
                // If we currently have a result available, deliver it
                // immediately.
                deliverResult(mDatas);
            }

            if (takeContentChanged() || mDatas == null) {
                // If the data has changed since the last time it was loaded
                // or is not currently available, start a load.
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        public void onCanceled(List<CBNewsEntry> data) {
            super.onCanceled(data);
            onReleaseResources(data);
        }

        @Override
        protected void onReset() {
            super.onReset();
            // Ensure the loader is stopped
            onStopLoading();

            if (mDatas != null) {
                onReleaseResources(mDatas);
                mDatas = null;
            }
        }

        /**
         * Helper function to take care of releasing resources associated
         * with an actively loaded data set.
         */
        protected void onReleaseResources(List<CBNewsEntry> data) {
            // For a simple List<> there is nothing to do.  For something
            // like a Cursor, we would close it here.
        }
    }

    class ListAdapter extends ArrayAdapter<CBNewsEntry> implements OnScrollListener {
        final LayoutInflater mInflater;
        SharedPreferences mPrefs;
        boolean mDisplayLogo;
        int mFontSize;

        public ListAdapter(Context context, List<CBNewsEntry> list) {
            super(context, 0, list);
            mInflater = LayoutInflater.from(context);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            mDisplayLogo = mPrefs.getBoolean(Configs.KEY_DISPLAY_LOGO, true);
            mFontSize = mPrefs.getInt(Configs.KEY_FONT_SIZE, Configs.DEFAULT_FONTSIZE);
        }

        @Override
        public void notifyDataSetChanged() {
            mDisplayLogo = mPrefs.getBoolean(Configs.KEY_DISPLAY_LOGO, true);
            mFontSize = mPrefs.getInt(Configs.KEY_FONT_SIZE, Configs.DEFAULT_FONTSIZE);
            super.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (mPage == Configs.HOME_PAGE_TOP) {
                return getTopView(position, convertView, parent);
            } else if (mPage == Configs.HOME_PAGE_HM) {
                return getHMView(position, convertView, parent);
            } else {
                return getNewsListView(position, convertView, parent);
            }
        }

        private View getNewsListView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.news_item, null);
            }
            NewsListItemView view = (NewsListItemView) convertView;
            CBNewsEntry value = getItem(position);
            view.setData(value, mDisplayLogo, mFontSize);
            return view;
        }

        private View getTopView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.top_item, null);
            }
            TextView index = (TextView) convertView.findViewById(R.id.index);
            TextView title = (TextView) convertView.findViewById(R.id.title);
            TextView clicks = (TextView) convertView.findViewById(R.id.click);
            TextView comments = (TextView) convertView.findViewById(R.id.comments);
            CBNewsEntry value = getItem(position);
            index.setText(String.valueOf(position + 1));
            title.setText(value.title);
            clicks.setText(String.valueOf(value.counter));
            comments.setText(String.valueOf(value.commentNumber));
            return convertView;
        }

        private View getHMView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.hm_item, null);
            }
            HmListItemView view = (HmListItemView) convertView;
            CBNewsEntry value = getItem(position);
            view.setData(value, mFontSize);
            return convertView;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                //mImageLoader.pause();
                if (mBackTop != null) {
                    mBackTop.setEnabled(true);
                }
            } else {
                //mImageLoader.resume();
                if (mBackTop != null) {
                    mBackTop.setEnabled(false);
                }
            }
        }
    }

}
