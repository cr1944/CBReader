package cheng.app.cnbeta.util;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cheng.app.cnbeta.data.CBNewsEntry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JsoupUtil {
    private static final String NUM_REX = "[^0-9]";

    /*public static void parserHome(String html, Context context) {
        CBSQLiteHelper dbHelper = new CBSQLiteHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Document doc = Jsoup.parse(html);
        Element flash = doc.getElementById("c_flash").getElementsByTag("script").get(0);
        Log.e(TAG, "flash:"+flash.data());
        parserFlash(flash.childNode(0).toString(), db);
        Element focus = doc.getElementById("c_focus");
        parserFocus(focus, db);
        Element hot = doc.getElementById("c_hot");
        parserHot(hot, db);
        Elements news = doc.getElementsByClass("newslist");
        parserNews(news, db);
    }

    private static void parserFlash(String script, SQLiteDatabase db) {
        int start = script.indexOf("linkarr[1]");
        int end = script.indexOf("for(i=1");
        String subString = script.substring(start, end).replaceAll("/n|/t|/r", "");
        Log.e(TAG, "subString:"+subString);
        String[] items = subString.split(";");
        String link = null;
        String pic = null;
        String text = null;
        db.delete(CBSQLiteHelper.TABLES.FLASH, null, null);
        for (String item : items) {
            if (!TextUtils.isEmpty(item)) {
                if (item.startsWith("linkarr")) {
                    String[] s = item.replaceAll("\"", "").split("=");
                    link = s[1];
                } else if (item.startsWith("picarr")) {
                    String[] s = item.replaceAll("\"", "").split("=");
                    pic = s[1];
                } else if (item.startsWith("textarr")) {
                    String[] s = item.replaceAll("\"", "").split("=");
                    text = s[1];
                }
                if (!TextUtils.isEmpty(link) && !TextUtils.isEmpty(pic) && !TextUtils.isEmpty(text)) {
                    ContentValues values = new ContentValues();
                    values.put(CBSQLiteHelper.FlashColumns.LINK, link);
                    values.put(CBSQLiteHelper.FlashColumns.PIC, pic);
                    values.put(CBSQLiteHelper.FlashColumns.TEXT, text);
                    Log.e(TAG, "insert: "+values);
                    db.insert(CBSQLiteHelper.TABLES.FLASH, null, values);
                    link = null;
                    pic = null;
                    text = null;
                }
            }
        }
    }

    private static void parserFocus(Element focus, SQLiteDatabase db) {
        db.delete(CBSQLiteHelper.TABLES.HOT, null, null);
        Element a = focus.getElementsByTag("dt").get(0).getElementsByTag("a").get(0);
        Element dd = focus.getElementsByTag("dd").get(0);
        ContentValues values = new ContentValues();
        values.put(CBSQLiteHelper.HotColumns.LINK, a.attr("href"));
        values.put(CBSQLiteHelper.HotColumns.TITLE, a.text());
        values.put(CBSQLiteHelper.HotColumns.TEXT, dd.text());
        Log.e(TAG, "insert: "+values);
        db.insert(CBSQLiteHelper.TABLES.HOT, null, values);
    }

    private static void parserHot(Element hot, SQLiteDatabase db) {
        Elements hots = hot.select("a[href]");
        for (Element e : hots) {
            ContentValues values = new ContentValues();
            values.put(CBSQLiteHelper.HotColumns.LINK, e.attr("href"));
            values.put(CBSQLiteHelper.HotColumns.TITLE, e.text());
            Log.e(TAG, "insert: "+values);
            db.insert(CBSQLiteHelper.TABLES.HOT, null, values);
        }
    }
    private static void parserNews(Elements news, SQLiteDatabase db) {
        for (Element e : news) {
            ContentValues values = new ContentValues();
            Element title = e.getElementsByClass("topic").get(0).getElementsByTag("a").get(0);
            values.put(CBSQLiteHelper.HomeColumns._ID, getId(title.attr("href")));
            values.put(CBSQLiteHelper.HomeColumns.LINK, title.attr("href"));
            values.put(CBSQLiteHelper.HomeColumns.TITLE, title.text());
            Element author = e.getElementsByClass("author").get(0);
            values.put(CBSQLiteHelper.HomeColumns.AUTHOR, author.text());
            Element text = e.getElementsByClass("desc").get(0);
            values.put(CBSQLiteHelper.HomeColumns.TEXT, text.text());
            Element detial = e.getElementsByClass("detail").get(0);
            values.put(CBSQLiteHelper.HomeColumns.DETAIL, detial.text());
            int topicId = parserTopic(text.getElementsByTag("a").get(0), db);
            values.put(CBSQLiteHelper.HomeColumns.TOPIC, topicId);
            Log.e(TAG, "insert News: "+values);
            db.insert(CBSQLiteHelper.TABLES.HOME, null, values);
        }
    }
    private static int parserTopic(Element topic, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        int id = getId(topic.attr("href"));
        values.put(CBSQLiteHelper.TopicsColumns._ID, id);
        values.put(CBSQLiteHelper.TopicsColumns.LINK, topic.attr("href"));
        Element img = topic.getElementsByTag("img").get(0);
        values.put(CBSQLiteHelper.TopicsColumns.TITLE, img.attr("alt"));
        values.put(CBSQLiteHelper.TopicsColumns.PIC, img.attr("src"));
        Log.e(TAG, "insert Topic: "+values);
        db.insert(CBSQLiteHelper.TABLES.TOPICS, null, values);
        return id;
    }*/

    public static LinkedList<CBNewsEntry> parserHome(String html) {
        Document doc = Jsoup.parse(html);
        Elements els = doc.getElementsByTag("a");
        LinkedList<CBNewsEntry> result = new LinkedList<CBNewsEntry>();
        for (Element e : els) {
            String href = e.attr("href");
            if (href.startsWith("marticle.php?sid=")) {
                CBNewsEntry value = new CBNewsEntry();
                value.articleId = getId(href);
                value.title = e.text();
                result.add(value);
            }
        }
        return result;
    }

    private static int getId(String text) {
        Pattern p = Pattern.compile(NUM_REX);
        Matcher m = p.matcher(text);
        return Integer.valueOf(m.replaceAll("").trim());
    }
    public static String parserArticle(String html) {
        //html.replaceAll("<img[^>]*?src\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')[^>]+>", "<a href=\"$1\"><img width=\"100%\" src=\"$1\" /></a>")
        //.replaceAll("<embed src=\"(.+?)\"[^>]*>", "<a href=\"$1\"><img src=\"file:///android_asset/play.jpg\" width=\"100%\" /></a>")
        //.replaceAll("<div[^>]*>(.+?)</div>", "$1")
        //.replaceAll("<p[^>]*>(.+?)</p>", "$1")
        //.replaceAll("<li[^>]*>(.+?)</li>", "$1");
        html.replace("<br /><br />", "<br />");
        Document doc = Jsoup.parse(html);
        Elements els = doc.select("a[href]");
        for (Element e : els) {
            String href = e.attr("href");
            if (href.startsWith("http://m.cnbeta.com")
                    || href.startsWith("hcomment.php?sid")
                    || href.startsWith("http://redir.oupeng.com")) {
                e.remove();
            }
        }
        Elements imgs = doc.select("img[src]");
        for (Element e : imgs) {
            e.attr("width", "100%");
        }
        Elements embeds = doc.select("embed[src]");
        for (Element e : embeds) {
            String src = e.attr("src");
            e.parent().html(("<a href=\"" + src + "\"><img src=\"file:///android_asset/play.png\" width=\"100%\" /></a>"));
        }
        return doc.html().replace("<p></p>", "");
    }
}
