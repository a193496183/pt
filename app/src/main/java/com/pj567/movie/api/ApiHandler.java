package com.pj567.movie.api;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.pj567.movie.ui.activity.ProjectionPlayActivity;
import com.pj567.movie.util.AdBlocker;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ApiHandler {
    private static final String TAG = "ResourceParser";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Boolean> loadedUrls = new HashMap<>();
    private final Object context; // 传入Activity或Context
    private boolean isSuccessParse = false;
    private String sourceUrl;
    private String downloadUrl;

    public ApiHandler(Object context,String sourceUrl) {
        this.context = context;
        this.sourceUrl = sourceUrl;
    }

    public void parseUrl(String url) {
        try {
            URI uri = new URI(url);
            downloadUrl = uri.getScheme() + "://" + uri.getHost();
        }catch (Exception e){

        }
        executorService.execute(() -> {
            try {
                OkHttpClient client = buildOkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String contentType = response.header("Content-Type");
                    if (contentType != null && contentType.contains("text/html")) {
                        // 解析HTML页面中的链接
                        ResponseBody body = response.body();
                        if (body != null) {
                            String html = body.string();
                            parseHtml(html);
                        }
                    } else {
                        // 处理其他类型的响应
                        String responseUrl = response.request().url().toString();
                        checkResourceUrl(responseUrl);
                    }
                } else {
                    showToast("请求失败: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "解析错误", e);
                showToast("解析错误");
            }
        });
    }

    private OkHttpClient buildOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    String url = request.url().toString();

                    // 广告拦截逻辑
                    boolean ad;
                    if (!loadedUrls.containsKey(url)) {
                        ad = AdBlocker.isAd(url);
                        loadedUrls.put(url, ad);
                    } else {
                        ad = loadedUrls.get(url);
                    }

                    if (ad) {
                        // 拦截广告请求
                        return new Response.Builder()
                                .request(request)
                                .protocol(okhttp3.Protocol.HTTP_1_1)
                                .code(200)
                                .message("Blocked")
                                .body(ResponseBody.create(null, new byte[0]))
                                .build();
                    }

                    // 检查资源URL
                    checkResourceUrl(url);

                    return chain.proceed(request);
                })
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    private void parseHtml(String html) {
        Document doc = Jsoup.parse(html);

        // 查找所有的链接
        Elements links = doc.select("a[href]");
        for (int i = 0; i < links.size(); i++) {
            String href = links.get(i).attr("href");
            checkResourceUrl(href);
        }

        Elements scriptsTmp = doc.select("script");
        for (int i = 0; i < scriptsTmp.size(); i++) {
            String scriptContent = scriptsTmp.get(i).html();
            checkResourceUrl(scriptContent);
        }

        // 查找所有的脚本
        Elements scripts = doc.select("script[src]");
        for (int i = 0; i < scripts.size(); i++) {
            String src = scripts.get(i).attr("src");
            checkResourceUrl(src);
        }

        // 查找所有的视频资源
        Elements videos = doc.select("video");
        for (int i = 0; i < videos.size(); i++) {
            String src = videos.get(i).attr("src");
            checkResourceUrl(src);

            // 查找视频标签内的source标签
            Elements sources = videos.get(i).select("source");
            for (int j = 0; j < sources.size(); j++) {
                String sourceSrc = sources.get(j).attr("src");
                checkResourceUrl(sourceSrc);
            }
        }
    }

    private void checkResourceUrl(String url) {
        if (url == null || url.isEmpty()) return;

        Log.e(TAG, "数据 = " + url);

        try {
            // 处理URL参数中的嵌套URL
            String processedUrl = url;

            // 定义正则表达式
            String regex = "const\\s+url\\s*=\\s*[\"']([^\"']+)[\"']";
            Pattern pattern = Pattern.compile(regex);

            Matcher matcher = pattern.matcher(url);

            if (matcher.find()) {
                url = matcher.group(1);
            }

            processedUrl = url;

            // 检查是否为视频资源
            if (processedUrl.contains(".m3u8?") || processedUrl.endsWith(".m3u8") ||
                    processedUrl.contains(".mp4?") || processedUrl.endsWith(".mp4")) {

                if (!isSuccessParse) {
                    Log.e(TAG, "解析地址 = " + processedUrl);
                    isSuccessParse = true;

                    // 使用反射调用Activity的方法
                    String finalProcessedUrl = processedUrl;
                    mainHandler.post(() -> {
                        try {
                            if (context instanceof android.app.Activity) {
                                android.app.Activity activity = (android.app.Activity) context;

                                // 创建Bundle并传递播放URL
                                Bundle bundle = new Bundle();
                                bundle.putString("playUrl", downloadUrl + finalProcessedUrl);

                                // 通过反射调用jumpActivity方法
                                java.lang.reflect.Method method = activity.getClass().getMethod(
                                        "jumpActivity", Class.class, Bundle.class);
                                method.invoke(activity, ProjectionPlayActivity.class, bundle);

                                // 通过反射调用finish方法
                                activity.finish();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "调用Activity方法失败", e);
                            showToast("解析错误");
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "处理URL时出错", e);
            showToast("解析错误");
        }
    }

    private void showToast(String message) {
        mainHandler.post(() -> {
            if (context instanceof android.content.Context) {
                Toast.makeText((android.content.Context) context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}