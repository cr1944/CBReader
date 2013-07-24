package cheng.app.cnbeta.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import cheng.app.cnbeta.util.ImageUtil;

public abstract class ScreenShotTask extends AsyncTask<Void, Void, byte[]> {
    static final String TAG = "ScreenShotTask";
    Bitmap mSrc;
    View mTragetView;

    public ScreenShotTask(View traget) {
        mTragetView = traget;
    }

    @Override
    protected void onPreExecute() {
    }

    protected abstract void onFinish(byte[] result);

    @Override
    protected byte[] doInBackground(Void... params) {
        if (mTragetView == null) {
            Log.e(TAG, "no view to shot");
            return null;
        }
        int w = mTragetView.getWidth();
        int h = mTragetView.getHeight();
        mSrc = Bitmap.createBitmap(w /2, h / 2, Bitmap.Config.RGB_565);
        mSrc.eraseColor(0xffffffff);
        Canvas canvas = new Canvas(mSrc);
        Matrix matrix = new Matrix(); 
        matrix.postScale(0.5f,0.5f);
        canvas.setMatrix(matrix);
        mTragetView.draw(canvas);
        //Bitmap resizeBmp =
        //    Bitmap.createBitmap(mSrc, 0, 0, mSrc.getWidth(), mSrc.getHeight(), matrix, true);
        return ImageUtil.Bitmap2Bytes(mSrc);
    }

    @Override
    protected void onPostExecute(byte[] result) {
        if (mSrc != null) {
            mSrc.recycle();
        }
        onFinish(result);
    }

}
