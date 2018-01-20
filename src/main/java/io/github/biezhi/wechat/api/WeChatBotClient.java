package io.github.biezhi.wechat.api;

import com.google.gson.Gson;
import io.github.biezhi.wechat.constant.Constant;
import io.github.biezhi.wechat.request.ApiRequest;
import io.github.biezhi.wechat.response.ApiResponse;
import io.github.biezhi.wechat.response.FileResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 微信机器人 HTTP 发送端
 *
 * @author biezhi
 * @date 2018/1/18
 */
@Slf4j
public class WeChatBotClient {

    private final OkHttpClient client;
    private       OkHttpClient clientWithTimeout;
    private final Gson         gson;
    private static final ConcurrentHashMap<String, List<Cookie>> cookieStore = new ConcurrentHashMap<String, List<Cookie>>();

    public WeChatBotClient(OkHttpClient client, Gson gson) {
        this.client = client;
        this.gson = gson;
        System.setProperty("https.protocols", "TLSv1");
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    public <T extends ApiRequest, R extends ApiResponse> void send(final T request, final io.github.biezhi.wechat.callback.Callback<T, R> callback) {
        OkHttpClient client = getOkHttpClient(request);
        client.newCall(createRequest(request)).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String body = response.body().string();
                    if (log.isDebugEnabled()) {
                        log.debug("Response: {}", body);
                    }
                    if (ApiResponse.class.equals(request.getResponseType())) {
                        callback.onResponse(request, (R) new ApiResponse(body));
                    } else {
                        R result = gson.fromJson(body, request.getResponseType());
                        result.setRawBody(body);
                        callback.onResponse(request, result);
                    }
                } catch (Exception e) {
                    IOException ioEx = e instanceof IOException ? (IOException) e : new IOException(e);
                    callback.onFailure(request, ioEx);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(request, e);
            }
        });
    }

    public <T extends ApiRequest, R extends ApiResponse> R send(final ApiRequest<T, R> request) {
        try {
            OkHttpClient client        = getOkHttpClient(request);
            Request      okHttpRequest = createRequest(request);
            Response     response      = client.newCall(okHttpRequest).execute();
            String       body          = response.body().string();

            if (log.isDebugEnabled()) {
                log.debug("Response : {}", body);
            }

            // 获取头部的Cookie,注意：可以通过Cooke.parseAll()来获取
            List<Cookie> cookies = Cookie.parseAll(okHttpRequest.url(), response.headers());
            // 防止header没有Cookie的情况
            if (cookies != null && cookies.size() > 0) {
                cookieStore.put(okHttpRequest.url().host(), cookies);
                if (!"webpush.web.wechat.com".equals(okHttpRequest.url().host())) {
                    cookieStore.put("webpush.web.wechat.com", cookies);
                }
            }

            if (ApiResponse.class.equals(request.getResponseType())) {
                return (R) new ApiResponse(body);
            }
            R result = gson.fromJson(body, request.getResponseType());
            result.setRawBody(body);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends ApiRequest, R extends ApiResponse> R download(final ApiRequest<T, R> request) {
        try {
            OkHttpClient client   = getOkHttpClient(request);
            Response     response = client.newCall(createRequest(request)).execute();
            return (R) new FileResponse(response.body().byteStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OkHttpClient getOkHttpClient(ApiRequest request) {
        OkHttpClient client = timeout(request);
        if (request.isNoRedirect()) {
            return client.newBuilder().followRedirects(false).followSslRedirects(false).build();
        }
        return cookie(client);
    }

    /**
     * 设置超时
     *
     * @param request
     * @return
     */
    private OkHttpClient timeout(ApiRequest request) {
        int timeoutMillis = request.getTimeout() * 1000;
        if (client.readTimeoutMillis() == 0 || client.readTimeoutMillis() > timeoutMillis) {
            return client;
        }
        if (null != clientWithTimeout && clientWithTimeout.readTimeoutMillis() > timeoutMillis) {
            return clientWithTimeout;
        }
        clientWithTimeout = client.newBuilder().readTimeout(timeoutMillis + 1000, TimeUnit.MILLISECONDS).build();
        return clientWithTimeout;
    }

    private OkHttpClient cookie(OkHttpClient client) {
        return client.newBuilder().cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl httpUrl, List<Cookie> cookies) {
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                List<Cookie> cookies = cookieStore.get(httpUrl.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        }).build();
    }

    public List<Cookie> cookies() {
        List<Cookie>             cookies = new ArrayList<Cookie>();
        Collection<List<Cookie>> values  = cookieStore.values();
        for (List<Cookie> value : values) {
            cookies.addAll(value);
        }
        return cookies;
    }

    public String cookie(String name) {
        for (Cookie cookie : cookies()) {
            if (cookie.name().equalsIgnoreCase(name)) {
                return cookie.value();
            }
        }
        return null;
    }

    private Request createRequest(ApiRequest request) {
        Request.Builder builder = new Request.Builder();
        if (Constant.GET.equalsIgnoreCase(request.getMethod())) {
            builder.get();
            if (null != request.getParameters() && request.getParameters().size() > 0) {
                Set<String>   keys = request.getParameters().keySet();
                StringBuilder sbuf = new StringBuilder(request.getUrl());
                if (request.getUrl().contains("=")) {
                    sbuf.append("&");
                } else {
                    sbuf.append("?");
                }
                for (String key : keys) {
                    sbuf.append(key).append('=').append(request.getParameters().get(key)).append('&');
                }
                request.url(sbuf.substring(0, sbuf.length() - 1));
            }
        } else {
            builder.method(request.getMethod(), createRequestBody(request));
        }
        builder.url(request.getUrl());
        if (log.isDebugEnabled()) {
            log.debug("Request : {}", request.getUrl());
        }
        if (null != request.getHeaders()) {
            builder.headers(request.getHeaders());
        }
        return builder.build();
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private RequestBody createRequestBody(ApiRequest<?, ?> request) {
        if (request.isMultipart()) {
            MediaType             contentType = MediaType.parse(request.getContentType());
            MultipartBody.Builder builder     = new MultipartBody.Builder().setType(MultipartBody.FORM);
            for (Map.Entry<String, Object> parameter : request.getParameters().entrySet()) {
                String name  = parameter.getKey();
                Object value = parameter.getValue();
                if (value instanceof byte[]) {
                    builder.addFormDataPart(name, request.getFileName(), RequestBody.create(contentType, (byte[]) value));
                } else if (value instanceof File) {
                    builder.addFormDataPart(name, request.getFileName(), RequestBody.create(contentType, (File) value));
                } else {
                    builder.addFormDataPart(name, String.valueOf(value));
                }
            }
            return builder.build();
        } else {
            if (request.isJsonBody()) {
                String json = gson.toJson(request.getParameters());
                if (log.isDebugEnabled()) {
                    log.debug("Request Body: {}", json);
                }
                return RequestBody.create(JSON, json);
            } else {
                FormBody.Builder builder = new FormBody.Builder();
                for (Map.Entry<String, Object> parameter : request.getParameters().entrySet()) {
                    builder.add(parameter.getKey(), String.valueOf(parameter.getValue()));
                }
                return builder.build();
            }

        }
    }

}