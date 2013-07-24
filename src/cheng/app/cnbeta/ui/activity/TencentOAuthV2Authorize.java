package cheng.app.cnbeta.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.gfan.sdk.statitistics.GFAgent;
import com.tencent.weibo.oauthv2.OAuthV2;
import com.tencent.weibo.oauthv2.OAuthV2Client;

public class TencentOAuthV2Authorize extends SherlockActivity {
    private static final String TAG = "TencentOAuthV2Authorize";
    private OAuthV2 oAuth;
    private WebView mWebView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        mWebView = new WebView(this);
        linearLayout.addView(mWebView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        setContentView(linearLayout);
        Intent intent = this.getIntent();
        oAuth = (OAuthV2) intent.getExtras().getSerializable("oauth");
        String urlStr = OAuthV2Client.generateImplicitGrantUrl(oAuth);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        mWebView.requestFocus();
        mWebView.loadUrl(urlStr);
        System.out.println(urlStr.toString());
        Log.i(TAG, "WebView Starting....");
        mWebView.setWebChromeClient(new MyWebChromeClient());
        WebViewClient client = new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.i(TAG, "WebView onPageStarted...");
                Log.i(TAG, "URL = " + url);
                if (url.indexOf("access_token=") != -1) {
                    int start = url.indexOf("access_token=");
                    String responseData = url.substring(start);
                    //OAuthV2Client.parseAccessTokenAndOpenId(responseData, oAuth);
                    Intent intent = new Intent();
                    intent.putExtra("response_data", responseData);
                    //intent.putExtra("oauth", oAuth);
                    setResult(RESULT_OK, intent);
                    //view.destroyDrawingCache();
                    //view.destroy();
                    finish();
                }
                super.onPageStarted(view, url, favicon);
            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                //if ((null != view.getUrl()) && (view.getUrl().startsWith("https://open.t.qq.com"))) {
                    handler.proceed();
                //} else {
                    //handler.cancel();
                //}
                    Log.d(TAG, "onReceivedSslError");
            }
        };
        mWebView.setWebViewClient(client);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mWebView != null) {
                    mWebView.destroyDrawingCache();
                    mWebView.destroy();
                }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            int progress = (Window.PROGRESS_END - Window.PROGRESS_START) / 100 * newProgress;
            setSupportProgress(progress);
        }
    }

}
