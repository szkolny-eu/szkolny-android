package im.wangchao.mhttp.internal.exception;

/**
 * <p>Description  : ParserException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/4/26.</p>
 * <p>Time         : 下午8:38.</p>
 */
public class ParserException extends Exception{
    public ParserException(){
        super("Response parse exception.");
    }

    public ParserException(Throwable cause) {
        super("Response parse exception.", cause);
    }
}
