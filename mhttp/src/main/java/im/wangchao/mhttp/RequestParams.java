package im.wangchao.mhttp;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import im.wangchao.mhttp.body.FileBody;
import im.wangchao.mhttp.body.JSONBody;
import im.wangchao.mhttp.body.MediaTypeUtils;
import im.wangchao.mhttp.body.OctetStreamBody;
import im.wangchao.mhttp.body.ProgressRequestBody;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import static im.wangchao.mhttp.body.MediaTypeUtils.APPLICATION_OCTET_STREAM;

/**
 * <p>Description  : RequestParams.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 15/8/17.</p>
 * <p>Time         : 下午2:16.</p>
 */
public class RequestParams{
    final private static String  UTF_8_STR   = "utf-8";
    final private static Charset UTF_8       = Charset.forName(UTF_8_STR);

    //params
    final private List<Pair<String, Object>>         urlParams       = new ArrayList<>();
    final private ConcurrentHashMap<String, StreamWrapper>  streamParams    = new ConcurrentHashMap<>();
    final private ConcurrentHashMap<String, FileWrapper>    fileParams      = new ConcurrentHashMap<>();

    //default
    private String contentEncoding  = UTF_8_STR;

    public RequestParams(){
        this((Map<String, Object>)null);
    }

    public RequestParams(Map<String, Object> params){
        if (params != null){
            for(Map.Entry<String, Object> entry : params.entrySet()){
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public RequestParams(Object...keysAndValues){
        int len = keysAndValues.length;
        if (len % 2 != 0){
            throw new IllegalArgumentException("Supplied arguments must be even.");
        }
        for (int i = 0; i < len; i += 2){
            String key = String.valueOf(keysAndValues[i]);
            String val = String.valueOf(keysAndValues[i + 1]);
            put(key, val);
        }
    }

    public RequestParams(RequestParams params){
        if (params == null){
            return;
        }
        this.urlParams.addAll(params.getUrlParams());
        this.streamParams.putAll(params.getStreamParams());
        this.fileParams.putAll(params.getFileParams());
        this.contentEncoding = params.contentEncoding();
    }

    RequestBody requestBody(MediaType mediaType, AbsCallbackHandler callback){
        if (isJSON(mediaType)){
            Charset charset = mediaType.charset();
            if (charset == null){
                charset = UTF_8;
            }
            return new JSONBody(parseJSON(), charset.name());
        }

        if (isForm(mediaType)){
            return formBody();
        }

        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        //form
        for (Pair<String, Object> entry: urlParams){
            builder.addFormDataPart(entry.first, String.valueOf(entry.second));
        }
        //stream
        for (Map.Entry<String, RequestParams.StreamWrapper> streamEntry: streamParams.entrySet()){
            builder.addPart(
                    okhttp3.Headers.of("Content-Disposition",
                            String.format("form-data;name=\"%s\";filename=\"%s\"", streamEntry.getKey(), streamEntry.getValue().name),
                            "Content-Transfer-Encoding", "binary"),
                    uploadProgressRequestBody(
                            new OctetStreamBody(streamEntry.getValue().inputStream, streamEntry.getValue().contentType),
                            callback));
        }
        //file
        for (Map.Entry<String, RequestParams.FileWrapper> file: fileParams.entrySet()){
            builder.addPart(uploadProgressRequestBody(
                    new FileBody(file.getValue().file, file.getValue().contentType),
                    callback
            ));
        }

        return builder.build();
    }

    private ProgressRequestBody uploadProgressRequestBody(RequestBody requestBody, AbsCallbackHandler callback){
        return new ProgressRequestBody(requestBody, callback);
    }

    private FormBody formBody(){
        FormBody.Builder builder = new FormBody.Builder();
        for (Pair<String, Object> entry: urlParams){
            builder.add(entry.first, String.valueOf(entry.second));
        }
        return builder.build();
    }

    /**
     * @return Request body media type is JSON.
     */
    private boolean isJSON(MediaType mediaType){
        return MediaTypeUtils.isJSON(mediaType) && streamParams.size() == 0
                && fileParams.size() == 0;
    }

    /**
     * @return Request body media type is Form.
     */
    private boolean isForm(MediaType mediaType){
        return MediaTypeUtils.isFORM(mediaType) && streamParams.size() == 0
                && fileParams.size() == 0;
    }

    /**
     * Request body encoding.
     */
    public RequestParams contentEncoding(@NonNull String encoding) {
        if (Charset.isSupported(encoding)){
            this.contentEncoding = encoding;
        }
        return this;
    }

    public String contentEncoding(){
        return contentEncoding;
    }

    public void put(Map<String, String> params){
        if (params != null){
            for(Map.Entry<String, String> entry : params.entrySet()){
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void put(List<Pair<String, Object>> params){
        if (params != null){
            urlParams.addAll(params);
        }
    }

    public void put(String key, String value) {
        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
            urlParams.add(new Pair<>(key, value));
        }
    }

    public void put(String key, Object value) {
        if (!TextUtils.isEmpty(key) && (value != null)) {
            if (value instanceof File){
                try {
                    put(key, (File)value);
                } catch (FileNotFoundException e) {
                    // Silent
                }
            } else {
                urlParams.add(new Pair<>(key, value));
            }
        }
    }



    public void put(String key, InputStream stream){
        put(key, stream, null);
    }

    public void put(String key, InputStream stream, String name){
        put(key, stream, name, null);
    }

    public void put(String key, InputStream stream, String name, String contentType){
        if (!TextUtils.isEmpty(key) && stream != null){
            streamParams.put(key, StreamWrapper.newInstance(stream, name, contentType));
        }
    }

    public void put(File file) throws FileNotFoundException {
        put(file, null);
    }

    public void put(File file, String contentType) throws FileNotFoundException {
        put(RequestParams.class.getSimpleName(), file, contentType);
    }

    public void put(String key, File file) throws FileNotFoundException {
        put(key, file, null);
    }

    public void put(String key, File file, String contentType) throws FileNotFoundException {
        if (file == null || !file.exists()){
            throw new FileNotFoundException();
        }
        if (!TextUtils.isEmpty(key)){
            fileParams.put(key, new FileWrapper(file, contentType));
        }
    }

    public void remove(String key){
        Pair<String, Object> removing = null;
        for (Pair<String, Object> entry: urlParams) {
            if (entry.first.equals(key)) {
                removing = entry;
            }
        }
        urlParams.remove(removing);
        streamParams.remove(key);
        fileParams.remove(key);
    }

    public boolean has(String key){
        for (Pair<String, Object> entry: urlParams) {
            if (entry.first.equals(key)) {
                return true;
            }
        }
        return (streamParams.containsKey(key) || fileParams.containsKey(key));
    }

    public boolean isEmpty(){
        return urlParams.isEmpty() && streamParams.isEmpty() && fileParams.isEmpty();
    }

    private String parseJSON(){
        JSONObject json = new JSONObject();

        String key;
        Object value;
        for (Pair<String, Object> entry: urlParams){
            key = entry.first;
            value = entry.second;

            if (key.isEmpty() || value == null){
                continue;
            }

            try {
                json.put(key, value);
            } catch (JSONException e) {
                // Silent
            }
        }

        return json.toString();
    }

    public ConcurrentHashMap<String, StreamWrapper> getStreamParams() {
        return streamParams;
    }

    public ConcurrentHashMap<String, FileWrapper> getFileParams() {
        return fileParams;
    }

    public List<Pair<String, Object>> getUrlParams() {
        return urlParams;
    }

    public HttpUrl formatURLParams(HttpUrl url) {
        HttpUrl.Builder builder = url.newBuilder();
        if (urlParams.size() != 0) {
            for (Pair<String, Object> entry : urlParams) {
                try {
                    builder.addEncodedQueryParameter(URLEncoder.encode(entry.first, contentEncoding),
                            URLEncoder.encode(String.valueOf(entry.second), contentEncoding));
                } catch (UnsupportedEncodingException e) {
                    //Silent
                }
            }
        }
        return builder.build();
    }

    /**
     * Format get params.
     */
    public String formatURLParams() {
        StringBuilder sb = new StringBuilder();
        if (urlParams.size() != 0) {
            for (Pair<String, Object> entry : urlParams) {
                String encode = "";
                try {
                    encode = URLEncoder.encode(String.valueOf(entry.second), contentEncoding);
                } catch (UnsupportedEncodingException e) {
                    //Silent
                }
                sb.append(entry.first).append("=").append(encode);
                sb.append("&");
            }
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString().replace(" ", "%20");
    }

    /**
     * Url params convert to {@link List}.
     */
    public List<Pair<String, Object>> getParamsList(){
        List<Pair<String, Object>> params = new LinkedList<>();

        for (Pair<String, Object> entry : urlParams) {
            params.add(new Pair<>(entry.first, entry.second));
        }

        return params;
    }

    public static class FileWrapper {
        public final File file;
        public final String contentType;

        public FileWrapper(File file, String contentType) {
            this.file = file;
            this.contentType = contentType;
        }
    }

    public static class StreamWrapper {
        public final InputStream inputStream;
        public final String name;
        public final String contentType;

        public StreamWrapper(InputStream inputStream, String name, String contentType) {
            this.inputStream = inputStream;
            this.name = name;
            this.contentType = contentType;
        }

        static StreamWrapper newInstance(InputStream inputStream, String name, String contentType) {
            return new StreamWrapper(
                    inputStream,
                    name,
                    TextUtils.isEmpty(contentType) ? APPLICATION_OCTET_STREAM : contentType);
        }
    }
}