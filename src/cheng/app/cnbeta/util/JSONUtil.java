package cheng.app.cnbeta.util;

import android.text.TextUtils;

import cheng.app.cnbeta.data.CBCommentEntry;
import cheng.app.cnbeta.data.CBNewsEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.LinkedList;

public class JSONUtil {
    public static final String TAG = "JSONUtil";

    public static LinkedList<CBNewsEntry> parseNewsList(String text) {
        try {
            JSONArray array = new JSONArray(new JSONTokener(text));
            int length = array.length();
            LinkedList<CBNewsEntry> result = new LinkedList<CBNewsEntry>();
            for (int i = 0; i < length; i++) {
                JSONObject item = array.getJSONObject(i);
                CBNewsEntry news = new CBNewsEntry();
                news.articleId = item.optLong("ArticleID");
                news.title = item.optString("title");
                news.pubTime = item.optString("pubtime");
                news.commentClosed = item.optInt("cmtClosed");
                news.commentNumber = item.optInt("cmtnum");
                news.summary = item.optString("summary");
                news.logo = item.optString("topicLogo").replace(" ", "%20");
                news.theme = item.optString("theme").replace(" ", "%20");
                result.add(news);
            }
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static LinkedList<CBNewsEntry> parseTop(String text) {
        try {
            JSONArray array = new JSONArray(new JSONTokener(text));
            int length = array.length();
            LinkedList<CBNewsEntry> result = new LinkedList<CBNewsEntry>();
            for (int i = 0; i < length; i++) {
                JSONObject item = array.getJSONObject(i);
                CBNewsEntry news = new CBNewsEntry();
                news.articleId = item.optLong("ArticleID");
                news.title = item.optString("title");
                news.counter = item.optInt("counter");
                news.commentClosed = item.optInt("cmtClosed");
                news.commentNumber = item.optInt("cmtnum");
                result.add(news);
            }
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static LinkedList<CBNewsEntry> parseHMComment(String text) {
        try {
            JSONArray array = new JSONArray(new JSONTokener(text));
            int length = array.length();
            LinkedList<CBNewsEntry> result = new LinkedList<CBNewsEntry>();
            for (int i = 0; i < length; i++) {
                JSONObject item = array.getJSONObject(i);
                CBNewsEntry news = new CBNewsEntry();
                news.articleId = item.optLong("ArticleID");
                news.comment = item.optString("comment");
                news.title = item.optString("title");
                news.name = item.optString("name");
                news.HMID = item.optLong("HMID");
                news.commentClosed = item.optInt("cmtClosed");
                news.commentNumber = item.optInt("cmtnum");
                result.add(news);
            }
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static LinkedList<CBCommentEntry> parseComments(long newsId, String title, String text) {
        try {
            JSONArray array = new JSONArray(new JSONTokener(text));
            int length = array.length();
            LinkedList<CBCommentEntry> result = new LinkedList<CBCommentEntry>();
            for (int i = 0; i < length; i++) {
                JSONObject item = array.getJSONObject(i);
                CBCommentEntry cmt = new CBCommentEntry();
                cmt.newsId = newsId;
                cmt.title = title;
                cmt.name = item.optString("name");
                cmt.comment = item.optString("comment");
                cmt.date = item.optString("date");
                cmt.tid = item.optLong("tid");
                cmt.support = item.optInt("support");
                cmt.against = item.optInt("against");
                result.add(cmt);
            }
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean parserTencentResponse(String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        try {
            JSONObject jo = new JSONObject(text);
            int errCode = jo.optInt("errcode");
            int ret = jo.optInt("ret");
            if (errCode == 0 && ret == 0) {
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}
