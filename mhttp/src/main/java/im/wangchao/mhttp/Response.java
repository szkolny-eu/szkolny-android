package im.wangchao.mhttp;

import okhttp3.Headers;
import okhttp3.Protocol;

/**
 * <p>Description  : MResponse.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/6/3.</p>
 * <p>Time         : 下午3:03.</p>
 */
public final class Response {
    public final static int IO_EXCEPTION_CODE   = 1000;

    public static Response error(Request request, int code, String message){
        if (message == null){
            message = "unknown exception.";
        }
        return new Response(request, new okhttp3.Response.Builder()
                .request(request.raw())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(message)
                .build());
    }

    public static Response newResponse(Request request, okhttp3.Response raw){
        return new Response(request, raw);
    }

    private final Request request;
    private final okhttp3.Response rawResponse;
    public String parserErrorBody = null;

    private Response(Request request, okhttp3.Response rawResponse){
        this.request = request;
        this.rawResponse = rawResponse;
    }

    public Request request() {
        return request;
    }

    public okhttp3.Response raw() {
        return rawResponse;
    }

    /** HTTP status code. */
    public int code() {
        return rawResponse.code();
    }

    /** HTTP status message or null if unknown. */
    public String message() {
        return rawResponse.message();
    }

    /** HTTP headers. */
    public Headers headers() {
        return rawResponse.headers();
    }

}
