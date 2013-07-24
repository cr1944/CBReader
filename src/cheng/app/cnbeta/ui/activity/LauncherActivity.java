package cheng.app.cnbeta.ui.activity;

import com.gfan.sdk.statitistics.GFAgent;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;
import android.widget.Toast;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.ui.fragment.CBDialogFragment;
import cheng.app.cnbeta.util.Configs;

public class LauncherActivity extends FragmentActivity {
    TextView mMessageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher);
        mMessageView = (TextView) findViewById(R.id.message);
        new CheckTask().execute();
    }
    @Override
    protected void onResume() {
        super.onResume();
        GFAgent.onResume (this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        GFAgent.onPause(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    class CheckTask extends AsyncTask<Void, Void, Void> {
        int mNetType;
        boolean mHasSdcard;
        @Override
        protected void onPreExecute() {
            mMessageView.setText(R.string.check_start);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ConnectivityManager conn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo net = conn.getActiveNetworkInfo();
            if (net == null || !net.isConnected()) {
                mNetType = Configs.NETWORK_NOT_ENABLED;
            } else if (net.getType() != ConnectivityManager.TYPE_WIFI){
                mNetType = Configs.NETWORK_MOBILE;
            } else {
                mNetType = Configs.NETWORK_WIFI;
            }
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                mHasSdcard = true;
            else
                mHasSdcard = false;
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            mMessageView.setText(R.string.check_stop);
            if (mNetType == Configs.NETWORK_NOT_ENABLED) {
                if (mHasSdcard) {
                    CBDialogFragment.newInstance(CBDialogFragment.DIALOG_NONET)
                        .show(getSupportFragmentManager(), "dialog");
                } else {
                    CBDialogFragment.newInstance(CBDialogFragment.DIALOG_NONET_NOSD)
                        .show(getSupportFragmentManager(), "dialog");
                }
            } else {
                if (!mHasSdcard) {
                    Toast.makeText(LauncherActivity.this, R.string.no_sd, Toast.LENGTH_SHORT).show();
                }
                if (mNetType == Configs.NETWORK_MOBILE) {
                    Toast.makeText(LauncherActivity.this, R.string.no_wifi, Toast.LENGTH_SHORT).show();
                }
                start();
            }
        }
    }

    public void quit() {
        finish();
    }

    public void offline() {
        Intent intent = new Intent(LauncherActivity.this, CacheActivity.class);
        startActivity(intent);
        finish();
    }

    public void start() {
        Intent intent = new Intent(LauncherActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}