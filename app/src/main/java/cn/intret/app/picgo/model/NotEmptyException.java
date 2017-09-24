package cn.intret.app.picgo.model;

/**
 * Created by intret on 2017/9/23.
 */

public class NotEmptyException extends Exception {

    public NotEmptyException() {
    }

    public NotEmptyException(String message) {
        super(message);
    }

    public NotEmptyException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEmptyException(Throwable cause) {
        super(cause);
    }

    public NotEmptyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
