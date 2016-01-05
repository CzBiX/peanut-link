package com.czbix.peanutlink.network;

import android.os.RemoteException;
import android.support.v4.util.ArrayMap;

import com.czbix.peanutlink.network.model.LoginParams;
import com.czbix.peanutlink.network.model.RegResult;
import com.czbix.peanutlink.network.model.User;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;

public class RequestHelper {
    private static final String BASE_URL = "http://mem.wode20.com/api2/wifiapp";
    private static final String SIGN_KEY = "6723A3DA72281F435DDA0D87393CDDE4";

    private static final String GET_REG_CODE_API = BASE_URL + "/getregcode";
    private static final String REG_USER_API = BASE_URL + "/reguser";
    private static final String GET_UTIME_API = BASE_URL + "/getutime";
    private static final String LOGIN_USER_API = BASE_URL + "/loginuser";
    private static final String FRESH_USER_API = BASE_URL + "/freshuser";
    private static final String OPEN_NET_API = BASE_URL + "/opennetpd";

    private static final OkHttpClient CLIENT;
    private static String mSession;

    static {
        CLIENT = new OkHttpClient();

        CLIENT.setConnectTimeout(10, TimeUnit.SECONDS);
        CLIENT.setWriteTimeout(10, TimeUnit.SECONDS);
        CLIENT.setReadTimeout(30, TimeUnit.SECONDS);
        CLIENT.networkInterceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                final Request.Builder builder = chain.request().newBuilder();
                builder.removeHeader("User-Agent");
                return chain.proceed(builder.build());
            }
        });
    }

    public static Observable<Void> getRegCode(final String phone) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                final ArrayMap<String, String> map = new ArrayMap<>(1);
                map.put("phoneno", phone);

                final Request request = new Request.Builder().url(GET_REG_CODE_API)
                        .post(newPostBody(2, map)).build();
                try {
                    sendRequest(request);
                    subscriber.onNext(null);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static Observable<RegResult> regUser(final String phone, final String regCode) {
        return Observable.create(new Observable.OnSubscribe<RegResult>() {
            @Override
            public void call(Subscriber<? super RegResult> subscriber) {
                final ArrayMap<Object, Object> map = new ArrayMap<>(2);
                map.put("phoneno", phone);
                map.put("regcode", regCode);

                final Request request = new Request.Builder().url(REG_USER_API)
                        .post(newPostBody(2, map)).build();
                try {
                    final JSONObject json = sendRequest(request);
                    final String ucode = json.getString("ucode");
                    final String uuid = json.getString("uuid");

                    subscriber.onNext(new RegResult(uuid, ucode));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static Observable<Long> getUTime() {
        return Observable.create(new Observable.OnSubscribe<Long>() {
            @Override
            public void call(Subscriber<? super Long> subscriber) {
                final Request request = new Request.Builder().url(GET_UTIME_API)
                        .post(newPostBody(2, new ArrayMap<>())).build();
                try {
                    final JSONObject json = sendRequest(request);

                    subscriber.onNext(json.getLong("utime"));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static Observable<User> loginUser(final LoginParams params) {
        return Observable.create(new Observable.OnSubscribe<User>() {
            @Override
            public void call(Subscriber<? super User> subscriber) {
                final String upwd = encode(params.ucode + params.utime);
                final ArrayMap<String, String> map = new ArrayMap<>(4);
                map.put("uuid", params.uuid);
                map.put("upwd", upwd);
                map.put("utime", params.utime);
                map.put("equid", params.mac);


                final Request request = new Request.Builder().url(LOGIN_USER_API)
                        .post(newPostBody(2, map)).build();

                try {
                    final JSONObject json = sendRequest(request);
                    final String session = json.getString("sessid");

                    subscriber.onNext(new User(params.uuid, params.ucode, session));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static Observable<Void> freshUser() {
        if (mSession == null) {
            throw new RuntimeException("session is empty");
        }

        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                final Request request = new Request.Builder().url(FRESH_USER_API)
                        .post(newPostBody(2, new ArrayMap<>())).build();
                try {
                    sendRequest(request);

                    subscriber.onNext(null);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static Observable<Void> openNet(final int flag, final String mac) {
        if (mSession == null) {
            throw new RuntimeException("session is empty");
        }

        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                final ArrayMap<String, Object> map = new ArrayMap<>();
                map.put("mac", mac);
                map.put("flag", flag);

                final Request request = new Request.Builder().url(OPEN_NET_API)
                        .post(newPostBody(1, map)).build();
                try {
                    sendRequest(request);

                    subscriber.onNext(null);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static Observable<Void> checkNetwork() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                final Request request = new Request.Builder().url("http://g.cn/generate_204").build();

                try {
                    final Response response = CLIENT.newCall(request).execute();
                    if (response.code() != 204) {
                        throw new RemoteException("invalid response code");
                    }

                    subscriber.onNext(null);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static void updateSession(String session) {
        mSession = session;
    }

    private static RequestBody newPostBody(int protocolVersion, Map<?, ?> params) {
        final ArrayMap<String, Object> map = new ArrayMap<>(2);
        {
            final ArrayMap<String, Object> hdata = new ArrayMap<>(8);
            map.put("hdata", hdata);

            hdata.put("ver", protocolVersion);
            hdata.put("aid", 1);
            hdata.put("aver", "2.0.21");
            hdata.put("ostype", 1);
            hdata.put("dcode", "dcode");
            hdata.put("seqno", Long.toString(getUtcTime()));
            hdata.put("sessid", mSession == null ? "" : mSession);
            hdata.put("resv", 1);
        }

        map.put("ddata", params);

        final JSONObject json = new JSONObject(map);
        final String data = json.toString();
        final String sign = encode(data + SIGN_KEY);

        return new FormEncodingBuilder()
                .add("_data", data)
                .add("_sign", sign)
                .build();
    }

    private static long getUtcTime() {
        return new Date().getTime();
    }

    private static String encode(String data) {
        try
        {
            byte[] bytes = MessageDigest.getInstance("MD5").digest(data.getBytes("utf-8"));
            StringBuilder sb = new StringBuilder();

            for (byte b : bytes) {
                int k = b & 0xFF;
                if (k < 16) {
                    sb.append(0);
                }
                sb.append(Integer.toHexString(k));
            }

            return sb.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static JSONObject sendRequest(Request request) throws RequestException, JSONException {
        try {
            final Response response = CLIENT.newCall(request).execute();
            if (response.code() != 200) {
                throw new RequestException("status code is: " + response.code());
            }
            final JSONObject json = readJson(response);
            final JSONObject ddata = json.getJSONObject("ddata");
            final String code = ddata.getString("code");
            if (!code.equals("99")) {
                throw new RequestException(String.format("code: %s, msg: %s", code, ddata.get("codemsg")), code);
            }

            return ddata;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONObject readJson(Response response) throws IOException, JSONException {
        return new JSONObject(response.body().string());
    }
}
