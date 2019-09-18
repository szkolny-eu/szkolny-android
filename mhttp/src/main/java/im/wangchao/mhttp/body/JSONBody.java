package im.wangchao.mhttp.body;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * <p>Description  : JSONBody.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/3/8.</p>
 * <p>Time         : 下午4:31.</p>
 */
public final class JSONBody extends RequestBody{
    private final static MediaType CONTENT_TYPE = MediaTypeUtils.JSON;
    private final MediaType contentType;
    private final byte[] bytes;

    public JSONBody(String content){
        this(content, null);
    }

    public JSONBody(String content, String charset){
        if (TextUtils.isEmpty(content)){
            throw new NullPointerException("content == null");
        }
        if (TextUtils.isEmpty(charset)){
            charset = "utf-8";
        }
        this.contentType = TextUtils.isEmpty(charset) ? CONTENT_TYPE : MediaType.parse("application/json; charset=" + charset);
        bytes = content.getBytes(contentType.charset());
    }

    @Override public MediaType contentType() {
        return contentType;
    }

    @Override public long contentLength() throws IOException {
        return bytes.length;
    }

    @Override public void writeTo(@NonNull BufferedSink sink) throws IOException {
        sink.write(bytes, 0, bytes.length);
    }

}
