package it.smartcommunitylab.playandgo.engine.exception;

public class ErrorInfo {
    public final String url;
    public final String ex;
    public final String code;

    public ErrorInfo(String url, String code, Exception ex) {
        this.url = url;
        this.code = code;
        //this.ex = ex.getClass().getCanonicalName() + " - " + ex.getLocalizedMessage();
        this.ex = ex.getLocalizedMessage();
    }
}