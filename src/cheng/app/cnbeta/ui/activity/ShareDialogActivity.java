package cheng.app.cnbeta.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.ui.WeiboShare;
import cheng.app.cnbeta.util.Configs;
import cheng.app.cnbeta.util.ImageUtil;

import com.actionbarsherlock.app.SherlockActivity;

public class ShareDialogActivity extends SherlockActivity implements OnClickListener,
    OnCheckedChangeListener {
    static final String TAG = "ShareDialogActivity";
    Bitmap mBitmap;
    String mText;
    SharedPreferences mPrefs;
    ImageView mScreenshot;
    TextView mSizeView;
    CheckBox mShareSina;
    CheckBox mShareTencent;
    CheckBox mShareImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share);
        mScreenshot = (ImageView) findViewById(R.id.share_image);
        mSizeView = (TextView) findViewById(R.id.share_image_size);
        findViewById(R.id.share_btn).setOnClickListener(this);
        findViewById(R.id.share_cancel_btn).setOnClickListener(this);
        mShareSina = (CheckBox) findViewById(R.id.share_sina_checkbox);
        mShareTencent = (CheckBox) findViewById(R.id.share_tencent_checkbox);
        mShareImage = (CheckBox) findViewById(R.id.share_image_checkbox);
        mShareSina.setOnCheckedChangeListener(this);
        mShareTencent.setOnCheckedChangeListener(this);
        mShareImage.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        long start = System.currentTimeMillis();
        Intent intent = getIntent();
        byte[] src = intent.getByteArrayExtra(Configs.EXTRA_BITMAP);
        mText = intent.getStringExtra(Configs.EXTRA_TEXT);
        int titleRes = intent.getIntExtra(Configs.EXTRA_TITLE, -1);
        if (titleRes != -1) {
            setTitle(titleRes);
        } else {
            setTitle(R.string.share);
        }
        if (src != null) {
            mBitmap = ImageUtil.byteToBitmap(src);
            mScreenshot.setImageBitmap(mBitmap);
            mSizeView.setText(getString(R.string.image_size, src.length / 1024));
        } else {
            mScreenshot.setImageResource(R.drawable.no_pic_icon);
            mSizeView.setText(getString(R.string.image_size, 0));
        }
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean syncSina = mPrefs.getBoolean(Configs.KEY_SYNC_SINA, true);
        boolean syncTencent = mPrefs.getBoolean(Configs.KEY_SYNC_TENCENT, true);
        boolean shareImage = mPrefs.getBoolean(Configs.KEY_SHARE_PIC, true);
        mShareSina.setChecked(syncSina);
        mShareTencent.setChecked(syncTencent);
        mShareImage.setChecked(shareImage);
        long time = System.currentTimeMillis() - start;
        Log.d(TAG, "time:"+time);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.share_btn:
                doShare();
                return;

            case R.id.share_cancel_btn:
                onBackPressed();
                return;
        }
    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.share_sina_checkbox: {
                final Editor editor = mPrefs.edit();
                editor.putBoolean(Configs.KEY_SYNC_SINA, isChecked);
                editor.commit();
                break;
            }
            case R.id.share_tencent_checkbox: {
                final Editor editor = mPrefs.edit();
                editor.putBoolean(Configs.KEY_SYNC_TENCENT, isChecked);
                editor.commit();
                break;
            }
            case R.id.share_image_checkbox: {
                final Editor editor = mPrefs.edit();
                editor.putBoolean(Configs.KEY_SHARE_PIC, isChecked);
                editor.commit();
                break;
            }
        }
    }

    private void doShare() {
        boolean sina = mShareSina.isChecked();
        boolean tencent = mShareTencent.isChecked();
        boolean pic = mShareImage.isChecked();
        if (pic && !ImageUtil.hasSdcard()) {
            Toast.makeText(this, R.string.share_screenshot_error, Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap src = pic ? mBitmap : null;
        if (sina && tencent) {
            new WeiboShare(this).share(src, mText);
        } else {
            if (sina) {
                new WeiboShare(this).shareSina(src, mText);
            }
            if (tencent) {
                new WeiboShare(this).shareTencent(src, mText);
            }
        }
        onBackPressed();
    }
}
