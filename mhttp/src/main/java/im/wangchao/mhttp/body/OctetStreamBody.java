package im.wangchao.mhttp.body;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * <p>Description  : OctetStreamBody.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/3/8.</p>
 * <p>Time         : 下午4:31.</p>
 */
public final class OctetStreamBody extends RequestBody{
    private final static MediaType CONTENT_TYPE = MediaTypeUtils.OCTET;
    private final MediaType contentType;
    private final InputStream stream;
    private long contentLength = -1L;
    private Buffer buffer;

    public OctetStreamBody(InputStream stream){
        this(stream, null);
    }

    public OctetStreamBody(InputStream stream, String contentType){
        this.stream = stream;
        this.contentType = TextUtils.isEmpty(contentType) ? CONTENT_TYPE : MediaType.parse(contentType);
    }

    @Override public MediaType contentType() {
        return contentType;
    }

    @Override public long contentLength() throws IOException {
        long result = contentLength;
        if (result != -1L) return result;
        return contentLength = writeOrCountBytes(null, true);
    }

    @Override public void writeTo(@NonNull BufferedSink sink) throws IOException {
        contentLength = writeOrCountBytes(sink, false);
    }

    private long writeOrCountBytes(@Nullable BufferedSink sink, boolean countBytes) throws IOException{
        long byteCount = 0L;

        if (countBytes) {
            buffer = new Buffer();
            Source source = null;
            try {
                source = Okio.source(stream);
                buffer.writeAll(source);
            } finally {
                Util.closeQuietly(source);
            }

            byteCount = buffer.size();

            return byteCount;
        } else {
            if (sink == null){
                return byteCount;
            }
            Source source = null;
            try {
                source = Okio.source(buffer == null ? stream : buffer.inputStream());
                return sink.writeAll(source);
            } finally {
                if (buffer != null){
                    buffer.clear();
                }
                Util.closeQuietly(source);
            }
        }

    }

}
