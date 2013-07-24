package cheng.app.cnbeta.ui;

import android.content.Context;
import android.widget.Toast;

public class MessageDisplayer implements Runnable {
    private final CharSequence mErrorMessage;
    private final Context mContext;

    public MessageDisplayer(Context context, int resId) {
        mContext = context;
        mErrorMessage = context.getText(resId);
    }

    @Override
    public void run() {
        Toast.makeText(mContext, mErrorMessage, Toast.LENGTH_SHORT).show();
    }
}


