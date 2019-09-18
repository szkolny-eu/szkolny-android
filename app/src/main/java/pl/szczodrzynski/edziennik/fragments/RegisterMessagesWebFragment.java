package pl.szczodrzynski.edziennik.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.StackingBehavior;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.databinding.FragmentRegisterMessagesWebBinding;
import pl.szczodrzynski.edziennik.utils.Anim;
import pl.szczodrzynski.edziennik.utils.Themes;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.DOWNLOAD_SERVICE;
import static pl.szczodrzynski.edziennik.utils.Utils.readableFileSize;

public class RegisterMessagesWebFragment extends Fragment {

    private static final String TAG = "RegisterMessagesWeb";
    private App app = null;
    private Activity activity = null;
    private FragmentRegisterMessagesWebBinding b = null;

    private WebView webView;
    private ProgressBar progressBar;
    private TextView error;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        if (getActivity() == null || getContext() == null)
            return null;
        app = (App) activity.getApplication();
        getContext().getTheme().applyStyle(Themes.INSTANCE.getAppTheme(), true);
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false);
        // activity, context and profile is valid
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_register_messages_web, container, false);
        return b.getRoot();
    }

    private boolean isStoragePermissionGranted(Activity a) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (a.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(a, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    private String getFileNameFromDisposition(String header)
    {
        return header.substring(header.indexOf("\"") + 1, header.lastIndexOf("\""));
    }

    private File downloadingFile;

    private void enqueueFile(String url, String filename, File downloadingFile, String cookieString) {

        this.downloadingFile = downloadingFile;

        long downloadReference;

        DownloadManager downloadManager;
        downloadManager = (DownloadManager)app.getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(filename);
        request.setDescription(getString(R.string.downloading));
        request.addRequestHeader("Cookie", cookieString);
        try {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        }
        catch (IllegalStateException e)
        {
            e.printStackTrace();
            Toast.makeText(app, "Failed to get external storage files directory", Toast.LENGTH_SHORT).show();
        }
        downloadReference = downloadManager.enqueue(request);
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private void openFile(File url) {
        Toast.makeText(app, getString(R.string.opening_file, url.getName()), Toast.LENGTH_SHORT).show();
        try {
            Uri uri = FileProvider.getUriForFile(app, app.getApplicationContext().getPackageName() + ".provider", url);
            //Uri uri = Uri.fromFile(url);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (url.toString().contains(".doc") || url.toString().contains(".docx")) {
                // Word document
                intent.setDataAndType(uri, "application/msword");
            } else if (url.toString().contains(".pdf")) {
                // PDF file
                intent.setDataAndType(uri, "application/pdf");
            } else if (url.toString().contains(".ppt") || url.toString().contains(".pptx")) {
                // Powerpoint file
                intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
            } else if (url.toString().contains(".xls") || url.toString().contains(".xlsx")) {
                // Excel file
                intent.setDataAndType(uri, "application/vnd.ms-excel");
            } else if (url.toString().contains(".zip") || url.toString().contains(".rar")) {
                // WAV audio file
                intent.setDataAndType(uri, "application/x-wav");
            } else if (url.toString().contains(".rtf")) {
                // RTF file
                intent.setDataAndType(uri, "application/rtf");
            } else if (url.toString().contains(".wav") || url.toString().contains(".mp3")) {
                // WAV audio file
                intent.setDataAndType(uri, "audio/x-wav");
            } else if (url.toString().contains(".gif")) {
                // GIF file
                intent.setDataAndType(uri, "image/gif");
            } else if (url.toString().contains(".jpg") || url.toString().contains(".jpeg") || url.toString().contains(".png")) {
                // JPG file
                intent.setDataAndType(uri, "image/jpeg");
            } else if (url.toString().contains(".txt")) {
                // Text file
                intent.setDataAndType(uri, "text/plain");
            } else if (url.toString().contains(".3gp") || url.toString().contains(".mpg") ||
                    url.toString().contains(".mpeg") || url.toString().contains(".mpe") || url.toString().contains(".mp4") || url.toString().contains(".avi")) {
                // Video files
                intent.setDataAndType(uri, "video/*");
            } else {
                intent.setDataAndType(uri, "*/*");
            }

            intent.setDataAndType(uri, getMimeType(uri.toString()));

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            app.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(app, R.string.opening_file_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadFile(Activity a, String url, String filename, long contentLength, String cookieString) {
        if (!isStoragePermissionGranted(a))
            return;
        new MaterialDialog.Builder(a)
                .title(R.string.downloading_file)
                .content(getString((R.string.download_file_question), filename, readableFileSize(contentLength)))
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .onPositive((dialog, which) -> {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File existingFile = new File(downloadsDir, filename);
                    if (existingFile.exists()) {
                        new MaterialDialog.Builder(a)
                                .title(R.string.downloading_file)
                                .content(getString(R.string.downloading_file_exists_choose, filename))
                                .stackingBehavior(StackingBehavior.ADAPTIVE)
                                .positiveText(R.string.downloading_file_exists_overwrite)
                                .negativeText(R.string.downloading_file_exists_open)
                                .neutralText(R.string.downloading_file_exists_create_new)
                                .onPositive(((dialog1, which1) -> {
                                    if (!existingFile.delete())
                                    {
                                        new MaterialDialog.Builder(a)
                                                .title(R.string.downloading_file)
                                                .content(R.string.downloading_file_cannot_remove)
                                                .positiveText(R.string.ok)
                                                .negativeText(R.string.cancel)
                                                .onPositive(((dialog2, which2) -> enqueueFile(url, filename, existingFile, cookieString)))
                                                .show();
                                        return;
                                    }
                                    enqueueFile(url, filename, existingFile, cookieString);
                                }))
                                .onNegative(((dialog1, which1) -> openFile(existingFile)))
                                .onNeutral((dialog1, which1) -> enqueueFile(url, filename, existingFile, cookieString))
                                .show();

                        return;
                    }
                    enqueueFile(url, filename, existingFile, cookieString);
                })
                .show();
    }

    private String photoPath;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> fileCallback;
    private final static int REQUEST_FILE_CHOOSER = 1;
    private boolean justLoaded = false;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(Build.VERSION.SDK_INT >= 21){
            Uri[] results = null;
            //Check if response is positive
            if(resultCode == RESULT_OK){
                if(requestCode == REQUEST_FILE_CHOOSER){
                    if(null == fileCallback){
                        return;
                    }
                    if(data == null){
                        //Capture Photo if no image available
                        if (photoPath != null) {
                            results = new Uri[]{Uri.parse(photoPath)};
                        }
                    }else{
                        String dataString = data.getDataString();
                        if(dataString != null){
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            }
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        } else {
            if (requestCode == REQUEST_FILE_CHOOSER) {
                if(null == mUM) return;
                Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    private File createImageFile() throws IOException{
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        webView = b.messagesWebView;
        progressBar = b.messagesWebProgressBar;
        error = b.messagesWebError;

        justLoaded = true;

        new Handler().postDelayed(() -> activity.runOnUiThread(() -> {
            if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                return;

            BroadcastReceiver onComplete = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (downloadingFile != null) {
                        openFile(downloadingFile);
                        downloadingFile = null;
                    }
                }
            };
            activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            app.apiEdziennik.initMessagesWebView(webView, app, /*app.profile.messagesWebFullVersion*/false, false);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setAllowFileAccess(true);
            webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
                String filename = getFileNameFromDisposition(contentDisposition);
                try {
                    URL urlObj = new URL(url);
                    Log.d(TAG, "Host "+urlObj.getProtocol()+"://"+urlObj.getHost());
                    Log.d(TAG, "Cookies "+CookieManager.getInstance().getCookie(urlObj.getProtocol()+"://"+urlObj.getHost()));

                    downloadFile(getActivity(), url, filename, contentLength, CookieManager.getInstance().getCookie(urlObj.getProtocol()+"://"+urlObj.getHost()));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                Anim.fadeOut(progressBar, 400, null);
            });

            webView.setWebViewClient(new WebViewClient() {
                boolean loadingFinished = true;
                boolean redirect = false;
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String request) {
                    if (!loadingFinished) {
                        redirect = true;
                    }

                    loadingFinished = false;
                    webView.loadUrl(request);
                    return true;
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    RegisterMessagesWebFragment.this.error.setVisibility(View.GONE);
                    loadingFinished = false;
                    //SHOW LOADING IF IT ISNT ALREADY VISIBLE
                    if (progressBar.getVisibility() != View.VISIBLE)
                        Anim.fadeIn(progressBar, 400, null);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    if (!redirect) {
                        loadingFinished = true;
                    }

                    if (loadingFinished && !redirect) {
                        //HIDE LOADING IT HAS FINISHED
                        //String cookies = CookieManager.getInstance().getCookie(url);
                        //Log.d(TAG, "All the cookies in a string:" + cookies);
                        Anim.fadeOut(progressBar, 400, null);
                        /*if (app.profile.messagesWebFullVersion && justLoaded && app.profile.loginType == Register.LOGIN_TYPE_MOBIDZIENNIK) {
                            if (!webView.getUrl().contains("wiadomosci")) {
                                // redirect to messages view
                                webView.loadUrl("https://" + app.getLoginData("serverName", "") + ".mobidziennik.pl/mobile/wiadomosci");
                            }
                            justLoaded = false;
                        }*/
                    } else {
                        redirect = false;
                    }
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                        return;

                    RegisterMessagesWebFragment.this.error.setVisibility(View.VISIBLE);
                    RegisterMessagesWebFragment.this.error.setText(getString(R.string.error_occured_format, error.toString()));
                    super.onReceivedError(view, request, error);
                }
            });

            if(Build.VERSION.SDK_INT >= 21) {
                webView.getSettings().setMixedContentMode(0);
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else if(Build.VERSION.SDK_INT >= 19) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }

            webView.setWebChromeClient(new WebChromeClient(){
                //For Android 5.0+
                public boolean onShowFileChooser(
                        WebView webView, ValueCallback<Uri[]> filePathCallback,
                        WebChromeClient.FileChooserParams fileChooserParams){
                    if(fileCallback != null){
                        fileCallback.onReceiveValue(null);
                    }
                    fileCallback = filePathCallback;
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if(takePictureIntent.resolveActivity(app.getPackageManager()) != null){
                        File photoFile = null;
                        try{
                            photoFile = createImageFile();
                            takePictureIntent.putExtra("PhotoPath", photoPath);
                        }catch(IOException ex){
                            Log.e(TAG, "Image file creation failed", ex);
                        }
                        if(photoFile != null){
                            photoPath = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        }else{
                            takePictureIntent = null;
                        }
                    }
                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType("*/*");
                    Intent[] intentArray;
                    if(takePictureIntent != null){
                        intentArray = new Intent[]{takePictureIntent};
                    }else{
                        intentArray = new Intent[0];
                    }

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, R.string.choose_file);
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                    startActivityForResult(chooserIntent, REQUEST_FILE_CHOOSER);
                    return true;
                }
            });


        }), 200);
    }

    /*public void loadVersion(boolean fullVersion) {
        if (app.profile.messagesWebFullVersion != fullVersion) {
            app.profile.messagesWebFullVersion = fullVersion;
            app.profile.savePending = true;
            justLoaded = true;
            app.apiEdziennik.initMessagesWebView(webView, app, fullVersion, true);
        }
    }*/

    public void performReload() {
        webView.reload();
    }

    public boolean processBackKey()
    {
        if (webView.canGoBack())
        {
            webView.goBack();
            return true;
        }
        return false;
    }
}
