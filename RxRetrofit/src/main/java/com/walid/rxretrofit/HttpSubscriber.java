package com.walid.rxretrofit;

import android.content.Context;
import android.net.ParseException;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonParseException;
import com.walid.rxretrofit.exception.ExceptionCode;
import com.walid.rxretrofit.exception.ServerResultException;
import com.walid.rxretrofit.interfaces.IHttpCallback;
import com.walid.rxretrofit.interfaces.IHttpCancelListener;
import com.walid.rxretrofit.utils.RxRetrogitLog;

import org.json.JSONException;

import java.net.SocketTimeoutException;

import retrofit2.adapter.rxjava.HttpException;
import rx.Subscriber;

/**
 * Author   : walid
 * Data     : 2016-08-18  15:59
 * Describe : http 观察者(订阅者)
 */
public class HttpSubscriber<T> extends Subscriber<T> implements IHttpCancelListener {

    private static final String TAG = "HttpSubscriber";

    //对应HTTP的状态码
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;
    private static final int REQUEST_TIMEOUT = 408;
    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final int BAD_GATEWAY = 502;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final int GATEWAY_TIMEOUT = 504;

    private Context context;
    private IHttpCallback<T> httpCallback;
    private boolean showError;

    public HttpSubscriber(Context context, IHttpCallback<T> httpCallback) {
        this(context, httpCallback, true);
    }

    public HttpSubscriber(Context context, IHttpCallback<T> httpCallback, boolean showError) {
        this.context = context;
        this.httpCallback = httpCallback;
        this.showError = showError;
    }

    // 订阅开始时调用
    @Override
    public void onStart() {
    }

    // 加载成功
    @Override
    public void onCompleted() {
        Log.d(TAG, "onCompleted");
    }

    // 对错误进行统一处理
    @Override
    public void onError(Throwable e) {

        Throwable throwable = e;
        //获取最根源的异常
        while (throwable.getCause() != null) {
            e = throwable;
            throwable = throwable.getCause();
        }

        //HTTP错误
        if (e instanceof HttpException) {
            HttpException httpException = (HttpException) e;
            switch (httpException.code()) {
                //权限错误，需要实现
                case UNAUTHORIZED:
                case FORBIDDEN:
                    callError(ExceptionCode.PERMISSION_ERROR, "权限错误~");
                    break;
                //均视为网络错误
                case NOT_FOUND:
                case REQUEST_TIMEOUT:
                case GATEWAY_TIMEOUT:
                case INTERNAL_SERVER_ERROR:
                case BAD_GATEWAY:
                case SERVICE_UNAVAILABLE:
                default:
                    callError(ExceptionCode.HTTP_EXCEPTION, "网络错误,请检查网络后再试~");
                    break;
            }
        } else if (e instanceof JsonParseException || e instanceof JSONException || e instanceof ParseException) {
            //均视为解析错误
            callError(ExceptionCode.PARSE_ERROR, "数据解析异常~");
        } else if (e instanceof SocketTimeoutException) {
            callError(ExceptionCode.SOCKET_TIMEOUT_EXCEPTION, "网络请求超时~");
        } else if (e instanceof ServerResultException) {
            ServerResultException apiException = (ServerResultException) e;
            callError(apiException.getCode(), apiException.getMessage());
        } else {
            callError(ExceptionCode.UNKNOWN_ERROR, "服务器正在开小灶,请稍后再试~");
        }

        RxRetrogitLog.e(e.getMessage());

    }

    private void callError(int code, String message) {
        if (showError) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
        if (httpCallback != null) {
            httpCallback.onError(code, message);
        }
    }

    // 将onNext方法中的返回结果交给Activity或Fragment自己处理
    @Override
    public void onNext(T t) {
        if (httpCallback == null) {
            return;
        }
        httpCallback.onNext(t);
    }

    // 取消ProgressDialog的时候，取消对observable的订阅，同时也取消了http请求
    @Override
    public void onCancel() {
        if (!this.isUnsubscribed()) {
            this.unsubscribe();
        }
    }

}