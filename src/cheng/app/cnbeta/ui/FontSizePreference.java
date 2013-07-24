
package cheng.app.cnbeta.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.util.Configs;

public class FontSizePreference extends DialogPreference implements OnSeekBarChangeListener {
    private static final String TAG = "FontSizePreference";
    private SeekBar mSeekBar;
    private TextView mSampleText;
    private SharedPreferences mPref;

    public FontSizePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPref = PreferenceManager.getDefaultSharedPreferences(context);
        setDialogLayoutResource(R.layout.font_setting_view);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mSeekBar = (SeekBar) view.findViewById(R.id.font_size_seekbar);
        mSampleText = (TextView) view.findViewById(R.id.font_size_sample);
        mSeekBar.setMax(Configs.MAXIMUM_FONTSIZE - Configs.MINIMUM_FONTSIZE);
        int fontsize = mPref.getInt(Configs.KEY_FONT_SIZE, Configs.DEFAULT_FONTSIZE);
        mSeekBar.setProgress(fontsize - Configs.MINIMUM_FONTSIZE);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSampleText.setTextSize(fontsize);
    }
    private void setFontSize(int fontsize) {
        Editor editor = mPref.edit();
        editor.putInt(Configs.KEY_FONT_SIZE, fontsize);
        editor.commit();
    }
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            setFontSize(mSeekBar.getProgress() + Configs.MINIMUM_FONTSIZE);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //setFontSize(progress + MINIMUM_FONTSIZE);
        mSampleText.setTextSize(progress + Configs.MINIMUM_FONTSIZE);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "onStartTrackingTouch");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

}
