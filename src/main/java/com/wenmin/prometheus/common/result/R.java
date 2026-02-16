package com.wenmin.prometheus.common.result;

import lombok.Data;

@Data
public class R<T> {
    private int code;
    private String msg;
    private T data;

    private R() {}

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.msg = ResultCode.SUCCESS.getMsg();
        r.data = data;
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(String msg, T data) {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.msg = msg;
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        R<T> r = new R<>();
        r.code = resultCode.getCode();
        r.msg = resultCode.getMsg();
        return r;
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.code = ResultCode.ERROR.getCode();
        r.msg = msg;
        return r;
    }
}
