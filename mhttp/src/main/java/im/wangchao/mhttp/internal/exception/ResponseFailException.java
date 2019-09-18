package im.wangchao.mhttp.internal.exception;

/**
 * <p>Description  : ResponseFailException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/4/25.</p>
 * <p>Time         : 下午4:06.</p>
 */
public class ResponseFailException extends Exception{
    //Response Non Successful
    public ResponseFailException(){
        super("Response failure exception.");
    }
}
