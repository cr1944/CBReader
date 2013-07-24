package cheng.app.cnbeta.ui.activity;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import java.io.IOException;

import cheng.app.cnbeta.R;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.gfan.sdk.statitistics.GFAgent;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HelpActivity extends SherlockFragmentActivity {
    static final String TAG = "HelpActivity";

    static final private String CHANGELOG_XML = "changelog";

    //Get the current app version
    private String GetAppVersion(Activity activity) {
        try {
            PackageInfo _info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            return _info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    //Parse a the release tag and return html code
    private String ParseReleaseTag(XmlResourceParser aXml) throws XmlPullParserException, IOException {
        String _Result = "<h1>Release: " + aXml.getAttributeValue(null, "version") + "</h1><ul>";
        int eventType = aXml.getEventType();
        while ((eventType != XmlPullParser.END_TAG) || (aXml.getName().equals("change"))) {
            if ((eventType == XmlPullParser.START_TAG) && (aXml.getName().equals("change"))) {
                eventType = aXml.next();
                _Result = _Result + "<li>" + aXml.getText() + "</li>";
            }
            eventType = aXml.next();
        }
        _Result = _Result + "</ul>";
        return _Result;
    }

    //CSS style for the html
    private String GetStyle() {
        return
                "<style type=\"text/css\">"
                        + "h1 { margin-left: 0px; font-size: 12pt; }"
                        + "li { margin-left: 0px; font-size: 9pt;}"
                        + "ul { padding-left: 30px;}"
                        + "</style>";
    }

    //Get the changelog in html code, this will be shown in the dialog's webview
    private String GetHTMLChangelog(int aResourceId, Resources aResource) {
        String _Result = "<html><head>" + GetStyle() + "</head><body>";
        XmlResourceParser _xml = aResource.getXml(aResourceId);
        try {
            int eventType = _xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if ((eventType == XmlPullParser.START_TAG) && (_xml.getName().equals("release"))) {
                    _Result = _Result + ParseReleaseTag(_xml);

                }
                eventType = _xml.next();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);

        } finally {
            _xml.close();
        }
        _Result = _Result + "</body></html>";
        return _Result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
        WebView webview = (WebView) findViewById(R.id.tips);
        String _PackageName = getPackageName();
        Resources _Resource = null;
        String _HTML = "";
        try {
            _Resource = getPackageManager().getResourcesForApplication(_PackageName);
            int _resID = _Resource.getIdentifier(CHANGELOG_XML, "xml", _PackageName);
            _HTML = GetHTMLChangelog(_resID, _Resource);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        webview.loadData(_HTML, "text/html; charset=UTF-8", null);
        GFAgent.setReportUncaughtExceptions(true);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
