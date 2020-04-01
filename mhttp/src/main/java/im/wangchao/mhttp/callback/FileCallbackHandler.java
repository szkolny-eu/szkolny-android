package im.wangchao.mhttp.callback;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import im.wangchao.mhttp.AbsCallbackHandler;
import im.wangchao.mhttp.Accept;
import im.wangchao.mhttp.Response;
import okhttp3.internal.Util;

/**
 * <p>Description  : FileResponseHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 15/10/18.</p>
 * <p>Time         : 下午2:39.</p>
 */
public class FileCallbackHandler extends AbsCallbackHandler<File> {
    private File file;
    final private static int BUFFER_SIZE = 4096;

    public FileCallbackHandler(Context context){
        this.file = getTempFile(context);
    }

    public FileCallbackHandler(File file){
        this.file = file;
    }

    protected File getFile(){
        return file;
    }

    @Override public void onSuccess(File file, Response response){

    }

    @Override public void onFailure(Response response, Throwable throwable) {

    }

    @Override public File backgroundParser(Response response) throws IOException{
        writeFile(response.raw(), file);
        return file;
    }

    @Override public String accept() {
        return Accept.ACCEPT_FILE;
    }

    private File getTempFile(Context context){
        try {
            return File.createTempFile("temp", "_handled", context.getCacheDir());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * write file , send progress message
     */
    protected void writeFile(okhttp3.Response response, File file) throws IOException {
        if (file == null){
            throw new IllegalArgumentException("File == null");
        }
        if (this.file.isDirectory()) {
            String contentDisposition = response.header("content-disposition");
            if (contentDisposition != null) {
                String filename = contentDisposition.substring(contentDisposition.indexOf("\"")+1, contentDisposition.lastIndexOf("\""));
                this.file = new File(file, filename);
                file = this.file;
            }
        }
        InputStream instream = response.body().byteStream();
        long contentLength = response.body().contentLength();
        FileOutputStream buffer = new FileOutputStream(file);
        if (instream != null) {
            try {
                byte[] tmp = new byte[BUFFER_SIZE];
                int l;
                long count = 0;
                while ((l = instream.read(tmp)) != -1 && !Thread.currentThread().isInterrupted()) {
                    count += l;
                    buffer.write(tmp, 0, l);

                    sendProgressEvent(count, contentLength);
                }
            } finally {
                Util.closeQuietly(instream);
                buffer.flush();
                Util.closeQuietly(buffer);
            }
        }
    }
}
