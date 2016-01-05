package com.czbix.peanutlink.network;

import com.squareup.okhttp.Response;

public class RequestException extends Exception {
    private String mErrCode = null;

    public RequestException() {
        super();
    }

    public RequestException(String detailMessage) {
        super(detailMessage);
    }

    public RequestException(String detailMessage, String errCode) {
        super(detailMessage);
        mErrCode = errCode;
    }

    public RequestException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public RequestException(Throwable throwable) {
        super(throwable);
    }

    public String getErrCode() {
        return mErrCode;
    }
}
