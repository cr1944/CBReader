package cheng.app.cnbeta.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import cheng.app.cnbeta.data.CBNewsEntry;
import cheng.app.cnbeta.data.CBSQLiteHelper.HmColumns;
import cheng.app.cnbeta.data.CBSQLiteHelper.NewsColumns;
import cheng.app.cnbeta.data.CBSQLiteHelper.TABLES;
import cheng.app.cnbeta.data.CBSQLiteHelper.TopColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

public class DataUtil {
    static final String TAG = "DataUtil";

    public static LinkedList<CBNewsEntry> readNewsList(long lastId, String limit, SQLiteDatabase db) {
        Cursor c = null;
        LinkedList<CBNewsEntry> result = new LinkedList<CBNewsEntry>();
        try {
            String sel = "1";
            if (lastId > 0) {
                sel += " AND " + NewsColumns.ARTICLE_ID + " < " + lastId;
            }
            c = db.query(TABLES.NEWS_LIST, null, sel, null, null, null,
                    NewsColumns.ARTICLE_ID + " DESC", limit);
            if (c != null && c.moveToFirst()) {
                do {
                    CBNewsEntry news = new CBNewsEntry();
                    news.articleId = c.getLong(c.getColumnIndex(NewsColumns.ARTICLE_ID));
                    news.title = HttpUtil.filterEntities(c.getString(c.getColumnIndex(NewsColumns.TITLE)));
                    news.pubTime = c.getString(c.getColumnIndex(NewsColumns.PUBTIME));
                    news.commentClosed = c.getInt(c.getColumnIndex(NewsColumns.CMT_CLOSED));
                    news.commentNumber = c.getInt(c.getColumnIndex(NewsColumns.CMT_NUMBER));
                    news.cached = c.getInt(c.getColumnIndex(NewsColumns.CACHED));
                    news.summary = HttpUtil.unescape(c.getString(c.getColumnIndex(NewsColumns.SUMMARY)));
                    news.theme = c.getString(c.getColumnIndex(NewsColumns.THEME));
                    news.logo = c.getString(c.getColumnIndex(NewsColumns.TOPIC_LOGO));
                    result.add(news);
                } while (c.moveToNext());
            }
            return result;
        } catch (RuntimeException ex) {
            Log.e(TAG, "readNewsList: RuntimeException", ex);
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static boolean saveNewsList(LinkedList<CBNewsEntry> list,
            SQLiteDatabase db) {
        Log.d(TAG, "saveNewsList");
        try {
            for (CBNewsEntry item : list) {
                ContentValues values = new ContentValues();
                values.put(NewsColumns.ARTICLE_ID, item.articleId);
                values.put(NewsColumns.TITLE, item.title);
                values.put(NewsColumns.PUBTIME, item.pubTime);
                values.put(NewsColumns.CMT_CLOSED, item.commentClosed);
                values.put(NewsColumns.CMT_NUMBER, item.commentNumber);
                values.put(NewsColumns.SUMMARY, HttpUtil.escape(item.summary));
                values.put(NewsColumns.THEME, item.theme);
                values.put(NewsColumns.TOPIC_LOGO, item.logo);
                int result = db.update(TABLES.NEWS_LIST, values,
                        NewsColumns.ARTICLE_ID + "=" + item.articleId, null);
                if (result < 1) {
                    db.insert(TABLES.NEWS_LIST, null, values);
                }
            }
            return true;
        } catch (RuntimeException ex) {
            Log.e(TAG, "saveNewsList: RuntimeException", ex);
            return false;
        }
    }

    public static LinkedList<CBNewsEntry> readHM(long lastId, String limit, SQLiteDatabase db) {
        Cursor c = null;
        LinkedList<CBNewsEntry> result = new LinkedList<CBNewsEntry>();
        try {
            String sel = "1";
            if (lastId > 0) {
                sel += " AND " + HmColumns.ARTICLE_ID + " < " + lastId;
            }
            c = db.query(TABLES.HM, null, sel, null, null, null,
                    HmColumns.ARTICLE_ID + " DESC", limit);
            if (c != null && c.moveToFirst()) {
                do {
                    CBNewsEntry news = new CBNewsEntry();
                    news.articleId = c.getLong(c.getColumnIndex(HmColumns.ARTICLE_ID));
                    news.title = c.getString(c.getColumnIndex(HmColumns.TITLE));
                    news.comment = c.getString(c.getColumnIndex(HmColumns.COMMENT));
                    news.commentClosed = c.getInt(c.getColumnIndex(HmColumns.CMT_CLOSED));
                    news.commentNumber = c.getInt(c.getColumnIndex(HmColumns.CMT_NUMBER));
                    news.HMID = c.getLong(c.getColumnIndex(HmColumns.HMID));
                    news.name = c.getString(c.getColumnIndex(HmColumns.NAME));
                    result.add(news);
                } while (c.moveToNext());
            }
            return result;
        } catch (RuntimeException ex) {
            Log.e(TAG, "readHM: RuntimeException", ex);
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    public static boolean saveHM(LinkedList<CBNewsEntry> list,
            SQLiteDatabase db) {
        Log.d(TAG, "saveHM");
        try {
            for (CBNewsEntry item : list) {
                ContentValues values = new ContentValues();
                values.put(HmColumns.ARTICLE_ID, item.articleId);
                values.put(HmColumns.TITLE, item.title);
                values.put(HmColumns.COMMENT, item.comment);
                values.put(HmColumns.CMT_CLOSED, item.commentClosed);
                values.put(HmColumns.CMT_NUMBER, item.commentNumber);
                values.put(HmColumns.HMID, item.HMID);
                values.put(HmColumns.NAME, item.name);
                db.replace(TABLES.HM, null, values);
            }
            return true;
        } catch (RuntimeException ex) {
            Log.e(TAG, "saveHM: RuntimeException", ex);
            return false;
        }
    }

    public static LinkedList<CBNewsEntry> readTop(SQLiteDatabase db) {
        Cursor c = null;
        LinkedList<CBNewsEntry> result = new LinkedList<CBNewsEntry>();
        try {
            c = db.query(TABLES.TOP, null, null, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    CBNewsEntry news = new CBNewsEntry();
                    news.articleId = c.getLong(c.getColumnIndex(TopColumns.ARTICLE_ID));
                    news.title = c.getString(c.getColumnIndex(TopColumns.TITLE));
                    news.counter = c.getInt(c.getColumnIndex(TopColumns.COUNTER));
                    news.commentClosed = c.getInt(c.getColumnIndex(TopColumns.CMT_CLOSED));
                    news.commentNumber = c.getInt(c.getColumnIndex(TopColumns.CMT_NUMBER));
                    result.add(news);
                } while (c.moveToNext());
            }
            return result;
        } catch (RuntimeException ex) {
            Log.e(TAG, "readTop: RuntimeException", ex);
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static boolean saveTop(LinkedList<CBNewsEntry> list,
            SQLiteDatabase db) {
        Log.d(TAG, "saveTop");
        try {
            db.delete(TABLES.TOP, null, null);
            for (CBNewsEntry item : list) {
                ContentValues values = new ContentValues();
                values.put(TopColumns.ARTICLE_ID, item.articleId);
                values.put(TopColumns.TITLE, item.title);
                values.put(TopColumns.COUNTER, item.counter);
                values.put(TopColumns.CMT_CLOSED, item.commentClosed);
                values.put(TopColumns.CMT_NUMBER, item.commentNumber);
                db.insert(TABLES.TOP, null, values);
            }
            return true;
        } catch (RuntimeException ex) {
            Log.e(TAG, "saveTop: RuntimeException", ex);
            return false;
        }
    }

    public static String readComments(long articleId, SQLiteDatabase db, boolean forceReLoad) {
//        Cursor c = null;
        String path = Configs.COMMENT_PATH + "/" + articleId;
        String url = Configs.COMMENT_URL + articleId;
//        boolean dbExist = false;
//        try {
//            c = db.query(TABLES.COMMENT, null,
//                    Columns.DATA_ID + "=" + articleId, null, null, null, null);
//            if (c != null && c.moveToFirst()) {
//                path = Configs.COMMENT_PATH + "/" + articleId;
//                dbExist = true;
//            }
//        } catch (RuntimeException ex) {
//            Log.e(TAG, "readComments: RuntimeException", ex);
//        } finally {
//            if (c != null) {
//                c.close();
//            }
//        }
        if (forceReLoad || !checkExist(path)) {
            Log.d(TAG, "readComments from url");
            String html = HttpUtil.getInstance().httpGet(url);
            if (saveComments(articleId, html, db)) {
                return html;
            }
        }
        Log.d(TAG, "readComments from file: " + path);
        return readFile(path);
    }

    private static boolean saveComments(long articleId, String result,
            SQLiteDatabase db) {
        if (TextUtils.isEmpty(result)) {
            Log.e(TAG, "saveComments: fail, comments empty");
            return false;
        }
        if (!ImageUtil.hasSdcard()) {
            Log.e(TAG, "saveComments: fail, no SDcard");
            return false;
        }
        OutputStream outputStream = null;

        File dir = new File(Configs.COMMENT_PATH);
        if (!dir.exists())
            dir.mkdirs();
        String path = Configs.COMMENT_PATH + "/" + articleId;
        Log.d(TAG, "saveComments: " + path);
        File file = new File(path);
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(result.getBytes());
            outputStream.close();
//            if (!dbExist) {
//                ContentValues values = new ContentValues();
//                values.put(Columns.DATA_ID, articleId);
//                db.insert(TABLES.COMMENT, null, values);
//            }
        } catch (FileNotFoundException ex) {
            Log.w(TAG, ex);
        } catch (IOException ex) {
            Log.w(TAG, ex);
        }
        return true;
    }

    public static String readNews(long articleId, SQLiteDatabase db) {
//        Cursor c = null;
        String path = Configs.NEWS_PATH + "/" + articleId;
        String url = Configs.NEWS_CONTENT_URL + articleId;
//        try {
//            c = db.query(TABLES.DATA, null,
//                    Columns.DATA_ID + "=" + articleId, null, null, null, null);
//            if (c != null && c.moveToFirst()) {
//                path = Configs.NEWS_PATH + "/" + articleId;
//                dbExist = true;
//            }
//        } catch (RuntimeException ex) {
//            Log.e(TAG, "readNews: RuntimeException", ex);
//        } finally {
//            if (c != null) {
//                c.close();
//            }
//        }
        if (!checkExist(path)) {
            Log.d(TAG, "readNews: not found, get from url");
            String html = HttpUtil.getInstance().httpGet(url);
            //String newhtml = JsoupUtil.parserArticle(html);
            saveNews(articleId, html, db);
            return html;
        } else {
            Log.d(TAG, "readNews: " + path);
            updateCache(articleId, db);
            return readFile(path);
        }
    }

    public static boolean removeNews(long articleId, SQLiteDatabase db) {
        String path = Configs.NEWS_PATH + "/" + articleId;
        File file = new File(path);
        if (file.exists()) {
            file.delete();
            Log.d(TAG, "News removed:" + path);
        }
//        db.delete(TABLES.DATA, Columns.DATA_ID + "=" + articleId, null);
        return true;
    }

    private static String saveNews(long articleId, String result,
            SQLiteDatabase db) {
        if (TextUtils.isEmpty(result)) {
            Log.e(TAG, "saveNews: fail, news empty");
            return null;
        }
        if (!ImageUtil.hasSdcard()) {
            Log.e(TAG, "saveNews: fail, no SDcard");
            return null;
        }
        OutputStream outputStream = null;

        File dir = new File(Configs.NEWS_PATH);
        if (!dir.exists())
            dir.mkdirs();
        String path = Configs.NEWS_PATH + "/" + articleId;
        Log.d(TAG, "saveNews: " + path);
        File file = new File(path);
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(result.getBytes());
            outputStream.close();
//            ContentValues v = new ContentValues();
//            v.put(Columns.DATA_ID, articleId);
//            if (!dbExist) {
//                db.insert(TABLES.DATA, null, v);
//            }
            updateCache(articleId, db);
        } catch (FileNotFoundException ex) {
            Log.w(TAG, ex);
        } catch (IOException ex) {
            Log.w(TAG, ex);
        }
        return path;
    }

    private static void updateCache(long articleId, SQLiteDatabase db) {
        try {
            ContentValues newsList = new ContentValues();
            newsList.put(NewsColumns.CACHED, 1);
            Log.d(TAG, "updateCache: update tobe cached");
            db.update(TABLES.NEWS_LIST, newsList,
                    NewsColumns.ARTICLE_ID + "=" + articleId, null);
        } catch (RuntimeException ex) {
            Log.e(TAG, "updateCache: RuntimeException", ex);
        }
    }

    public static void updateCommentNumber(long articleId, int number, SQLiteDatabase db) {
        try {
            ContentValues newsList = new ContentValues();
            newsList.put(NewsColumns.CMT_NUMBER, number);
            Log.d(TAG, "updateCommentNumber: update cmt number");
            db.update(TABLES.NEWS_LIST, newsList,
                    NewsColumns.ARTICLE_ID + "=" + articleId, null);
        } catch (RuntimeException ex) {
            Log.e(TAG, "updateCommentNumber: RuntimeException", ex);
        }
    }

    public static int readCommentNumber(long articleId, SQLiteDatabase db) {
        Cursor c = null;
        int result = -1;
        try {
            c = db.query(TABLES.NEWS_LIST, null, NewsColumns.ARTICLE_ID + "=" + articleId, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    int commentClosed = c.getInt(c.getColumnIndex(TopColumns.CMT_CLOSED));
                    int commentNumber = c.getInt(c.getColumnIndex(TopColumns.CMT_NUMBER));
                    //if (commentClosed == 0) {
                        result = commentNumber;
                    //}
                } while (c.moveToNext());
            }
            return result;
        } catch (RuntimeException ex) {
            return -1;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private static String readFile(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                Log.e(TAG, "read: file not exists");
                return null;
            } else {
                FileInputStream f = new FileInputStream(path);
                int length = f.available();
                byte[] buffer = new byte[length];
                f.read(buffer);
                f.close();
                return new String(buffer);
            }
        } catch (FileNotFoundException ex) {
            Log.w(TAG, ex);
            return null;
        } catch (IOException ex) {
            Log.w(TAG, ex);
            return null;
        }
    }

    private static boolean checkExist(String path) {
        if (TextUtils.isEmpty(path)) {
            Log.d(TAG, "checkExist: path is empty");
            return false;
        }
        File file = new File(path);
        boolean exist = file.exists();
        return exist;
    }

}
