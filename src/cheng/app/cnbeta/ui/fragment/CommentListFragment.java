
package cheng.app.cnbeta.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.data.CBCommentEntry;
import cheng.app.cnbeta.ui.CommentListItemView;
import cheng.app.cnbeta.ui.activity.NewsCommentActivity;
import cheng.app.cnbeta.util.Configs;
import cheng.app.cnbeta.util.DataUtil;

import com.actionbarsherlock.app.SherlockListFragment;
import java.util.LinkedList;
import java.util.List;

public class CommentListFragment extends SherlockListFragment implements
        OnCreateContextMenuListener {
    static final String TAG = "CommentListFragment";
    int mPage;
    CommentListAdapter mAdapter;
    LinkedList<CBCommentEntry> mList;
    TextView mTotalView;
    NewsCommentActivity mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mList = new LinkedList<CBCommentEntry>();
        Bundle args = getArguments();
        mContext = (NewsCommentActivity) getActivity();
        mPage = args != null ? args.getInt(Configs.EXTRA_PAGE) : Configs.CMT_PAGE_ALL;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.comment_pager_list, container, false);
        mTotalView = (TextView) v.findViewById(R.id.comment_pager_title);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!mList.isEmpty()) {
            mAdapter = new CommentListAdapter(getActivity(), mList, mPage);
            setListAdapter(mAdapter);
        }
        getListView().setOnCreateContextMenuListener(this);
    }

    /*@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(R.string.share_to);
        menu.add(0, 1, 0, R.string.sync_sina);
        menu.add(0, 2, 0, R.string.sync_tencent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        CBCommentEntry listItem = mAdapter.getItem(info.position);
        // View v = getListView().getChildAt(info.position);
        //String shot = ImageUtil.shot(info.targetView);
        switch (item.getItemId()) {
            case 1: {
                String tag = "cmt_" + listItem.tid + "_" + info.targetView.hashCode();
                info.targetView.setTag(tag);
                new WeiboShare(getActivity(), mDb)
                .shareSina(info.targetView, mNewsId, mTitle, listItem);
                return true;
            }
            case 2: {
                String tag = "cmt_" + listItem.tid + "_" + info.targetView.hashCode();
                info.targetView.setTag(tag);
                new WeiboShare(getActivity(), mDb)
                .shareTencent(info.targetView, mNewsId, mTitle, listItem);
                return true;
            }

        }
        return super.onContextItemSelected(item);
    }*/

    public void setData(LinkedList<CBCommentEntry> list) {
        if (!mList.isEmpty()) {
            mList.clear();
        }
        mList.addAll(list);
        mTotalView.setText(getString(R.string.total_comment, mList.size()));
        if (mAdapter == null) {
            mAdapter = new CommentListAdapter(getActivity(), mList, mPage);
            setListAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    public void setHotData(LinkedList<CBCommentEntry> list) {
        LinkedList<CBCommentEntry> hots = new LinkedList<CBCommentEntry>();
        for (CBCommentEntry item : list) {
            if (item.against > 10 || item.support > 10) {
                hots.add(item);
            }
        }
        setData(hots);
    }
    public void notifyDataChange() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.i(TAG, "Item clicked: " + id);
    }

    class CommentListAdapter extends ArrayAdapter<CBCommentEntry> {
        int mPage;
        final LayoutInflater mInflater;

        public CommentListAdapter(Context context, List<CBCommentEntry> list, int page) {
            super(context, 0, list);
            mInflater = LayoutInflater.from(context);
            mPage = page;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (mPage == Configs.CMT_PAGE_HOT) {
                return getHotView(position, convertView, parent);
            }
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.comment_item, null);
            }
            CommentListItemView view = (CommentListItemView) convertView;
            view.setListener(mContext);
            CBCommentEntry value = getItem(position);
            view.setData(value, position);
            return view;
        }

        private View getHotView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.comment_item, null);
                convertView.setBackgroundResource(R.drawable.bg_hot_cmt);
            }
            CommentListItemView view = (CommentListItemView) convertView;
            view.setListener(mContext);
            CBCommentEntry value = getItem(position);
            view.setData(value, -1);
            return view;
        }
    }
}
