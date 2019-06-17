package com.weining.eosplaylotteryhelper.web;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by baidu on 16/5/15.
 */
public class OkHttpUtils {
    private static final String CHARSET_NAME = "UTF-8";
    private static final OkHttpClient sOkHttpClient;

    static {
        sOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public static Response doGet(String url, Map<String, String> params) {
        Request request = new Request.Builder().url(attachHttpGetParams(url, params)).build();
        return sendRequest(request);
    }

    public static Response doPostJson(String url, String json) {
        RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-ring.mp3"), json);
        Request request = new Request.Builder()
                .url(url)//请求的url
                .post(requestBody)
                .build();
        return sendRequest(request);
    }

    public static Response doPost(String url, Map<String, String> params) {
        FormBody.Builder builder = new FormBody.Builder();
        Set<Map.Entry<String, String>> entries = params.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String name = entry.getKey();
            String value = entry.getValue();
            builder.add(name, value == null ? "" : value);
        }
        Request request = new Request.Builder().post(builder.build()).url(url).build();
        return sendRequest(request);
    }

    public static Response doPostWithFile(String url, Map<String, String> params, Map<String, File> fileParams) {

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (fileParams.size() > 0) {
            File file = null;
            for (Map.Entry<String, File> entry : fileParams.entrySet()) {
                file = entry.getValue();
                Headers.Builder headersBuilder = new Headers.Builder();
                headersBuilder.add("Content-Disposition",
                        "form-data; name=\"" + entry.getKey() + "\";filename=\"" + entry.getValue().getName() + "\"");
                headersBuilder.add("Content-Transfer-Encoding", "binary");
                Headers headers = headersBuilder.build();
                builder.addPart(headers, RequestBody.create(MediaType.parse("application/octet-stream"), file));
            }
        }
        Set<Map.Entry<String, String>> entries = params.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String name = entry.getKey();
            String value = entry.getValue();
            builder.addFormDataPart(name, null, RequestBody.create(MediaType.parse
                    ("text/plain; charset=UTF-ring.mp3"), value));
        }
        Request request = new Request.Builder().post(builder.build()).url(url).build();
        return sendRequest(request);
    }

    private static Response sendRequest(Request request) {
        try {
            return sOkHttpClient.newCall(request).execute();
        } catch (IOException e) {
            return null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public static String formatParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        StringBuilder query = new StringBuilder();
        Set<Map.Entry<String, String>> entries = params.entrySet();
        boolean hasParam = false;
        for (Map.Entry<String, String> entry : entries) {
            String name = entry.getKey();
            String value = entry.getValue();
            value = value == null ? "" : value;
            if (!TextUtils.isEmpty(name)) {
                if (hasParam) {
                    query.append("&");
                } else {
                    hasParam = true;
                }
                try {
                    query.append(name).append("=")
                            .append(URLEncoder.encode(value, CHARSET_NAME));
                } catch (UnsupportedEncodingException e) {
                }
            }

        }
        return query.toString();
    }

    public static String attachHttpGetParams(String url, Map<String, String> params) {
        return url + "?" + formatParams(params);
    }
}
