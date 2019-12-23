package pl.szczodrzynski.edziennik.utils;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.hypertrack.hyperlog.HyperLog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import pl.szczodrzynski.edziennik.App;

public class Utils {
    private static final String TAG = "Utils";

    public static String bs(@Nullable String s) {
        return ns("", s);
    }
    public static String bs(String prefix, @Nullable String s) {
        return ns("", prefix, s);
    }
    public static String bs(@Nullable String prefix, @Nullable String s, String suffix) {
        return ns("", prefix, s, suffix);
    }

    public static String ns(String ifNull, @Nullable String s) {
        return s == null || s.trim().isEmpty() ? ifNull : s;
    }
    public static String ns(String ifNull, String prefix, @Nullable String s) {
        return s == null || s.trim().isEmpty() ? ifNull : prefix+s;
    }
    public static String ns(String ifNull, @Nullable String prefix, @Nullable String s, String suffix) {
        return s == null || s.trim().isEmpty() ? ifNull : (prefix == null ? "" : prefix)+s+suffix;
    }

    public static boolean contains(@Nullable List<?> list, @NonNull Object o) {
        if (list == null)
            return false;
        return list.contains(o);
        /*for (Object o2: list) {
            if (o.equals(o2))
                return true;
        }
        return false;*/
    }

    public static List<String> debugLog = new ArrayList<>();

    public static void d(String TAG, String message) {
        if (App.devMode) {
            HyperLog.d("Szkolny/"+TAG, message);
            //debugLog.add(TAG+": "+message);
        }
    }
    public static void c(String TAG, String message) {
        /*if (App.devMode) {
            Log.d(TAG, "// " + message);
            ///debugLog.add(TAG+": // "+message);
        }*/
    }

    /**
     * Returns the first set (high) bit position, from right to left.
     * @param n the number
     * @return a 0-indexed position
     */
    public static int rightmostSetBit(int n) {
        return (int)((Math.log10(n & -n)) / Math.log10(2));
    }
    /**
     * Returns the last set (high) bit position, from left to right.
     * @param n the number
     * @return a 0-indexed position
     */
    public static int leftmostSetBit(int n) {
        return (int)(Math.log(n) / Math.log(2));
    }

    /**
     * Returns number of cells needed for given size of the widget.
     *
     * @param size Widget size in dp.
     * @return Size in number of cells.
     */
    public static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < size) {
            ++n;
        }
        return n - 1;
    }

    // EXCEPTION: thread does not change
    /*public static void exception(String TAG, Response response, @NonNull Throwable throwable, String initialApiResponse, SyncCallback callback, Context activityContext) {
        //d(TAG, "Thread/exception1/"+Thread.currentThread().getName());
        if (Thread.currentThread().getName().equals("main")) {
            AsyncTask.execute(() -> {
                exceptionRun(TAG, response, throwable, initialApiResponse, callback, activityContext, true);
            });
        }
        else {
            exceptionRun(TAG, response, throwable, initialApiResponse, callback, activityContext, false);
        }
    }
    private static void exceptionRun(String TAG, Response response, @NonNull Throwable throwable, String initialApiResponse, SyncCallback callback, Context activityContext, boolean finishOnMainThread) {
        //d(TAG, "Thread/exception2/"+Thread.currentThread().getName());
        String apiResponse = initialApiResponse;
        throwable.printStackTrace();
        if (response != null) {
            //d(TAG, response.code() + " " + response.message());
            if (apiResponse == null) {
                apiResponse = response.code()+" "+response.message()+"\n";
                if (response.raw().body() != null) {
                    try {
                        apiResponse += response.raw().body().string();
                    } catch (Exception e) {
                        e.printStackTrace();
                        apiResponse += "Exception while getting response body:\n"+Log.getStackTraceString(e);
                    }
                }
                else {
                    apiResponse += "Response body is null.";
                }
            }
            String apiResponsePrefix = "Request:\n";
            apiResponsePrefix += response.request().url().toString()+"\n";
            apiResponsePrefix += response.request().headers().toString()+"\n";
            apiResponsePrefix += response.request().bodyToString();
            apiResponsePrefix += "\n\nResponse:\n";
            apiResponse = apiResponsePrefix + apiResponse;
        }
        if (callback != null) {
            String finalApiResponse = apiResponse;
            if (!finishOnMainThread) {
                exceptionFinish(TAG, response, throwable, finalApiResponse, callback, activityContext);
                return;
            }
            new Handler(activityContext != null ? activityContext.getMainLooper() : Looper.getMainLooper()).post(() -> {
                exceptionFinish(TAG, response, throwable, finalApiResponse, callback, activityContext);
            });
        }
    }
    private static void exceptionFinish(String TAG, Response response, @NonNull Throwable throwable, String finalApiResponse, SyncCallback callback, Context activityContext) {
        //d(TAG, "Thread/exception3/"+Thread.currentThread().getName());
        if (throwable instanceof UnknownHostException) {
            callback.onError(activityContext, Error.CODE_NO_INTERNET, "", throwable, finalApiResponse);
        }
        else if (throwable instanceof SSLException) {
            callback.onError(activityContext, Error.CODE_SSL_ERROR, "", throwable, finalApiResponse);
        }
        else if (throwable instanceof SocketTimeoutException) {
            callback.onError(activityContext, Error.CODE_TIMEOUT, "", throwable, finalApiResponse);
        }
        else if (throwable instanceof InterruptedIOException) {
            callback.onError(activityContext, Error.CODE_NO_INTERNET, "", throwable, finalApiResponse);
        }
        else {
            if (response != null) {
                if (response.code() == 424 || response.code() == 400 || response.code() == 401 || response.code() == 500 || response.code() == 503 || response.code() == 404) {
                    callback.onError(activityContext, CODE_MAINTENANCE, response.code() + " " + response.message() + " " + throwable.getMessage(), throwable, finalApiResponse);
                }
                else {
                    callback.onError(activityContext, CODE_OTHER, response.code() + " " + response.message() + " " + throwable.getMessage(), throwable, finalApiResponse);
                }
            }
            else {
                callback.onError(activityContext, CODE_OTHER, throwable.getMessage(), throwable, finalApiResponse);
            }
        }
    }*/

    public static String hexFromColorInt(@ColorInt int color) {
        return String.format("%06X", (0xFFFFFF & color));
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index;
        String result = null;
        try {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
            cursor.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int strToInt(String s, int defaultValue)
    {
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static int strToInt(String s)
    {
        return Integer.parseInt(s);
    }
    public static String intToStr(Integer i)
    {
        return Integer.toString(i);
    }

    public static int crc16(final byte[] buffer) {
        int crc = 0xFFFF;
        for (byte aBuffer : buffer) {
            crc = ((crc >>> 8) | (crc << 8)) & 0xffff;
            crc ^= (aBuffer & 0xff); // byte to int, trunc sign
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;
        }
        crc &= 0xffff;
        return crc+32768;
    }

    public static long crc32(byte[] buffer) {
        CRC32 crc = new CRC32();
        crc.update(buffer);
        return crc.getValue();
    }

    public static String toPascalCase(String source) {
        return source.toUpperCase().charAt(0)+source.toLowerCase().substring(1);
    }

    @ColorInt
    public static int getAttr(Context context, @AttrRes int resourceId) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(resourceId, typedValue, true);
        return typedValue.data;
    }

    public static int dpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px)
    {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static void openGooglePlay(Context context) {
        openGooglePlay(context, context.getPackageName());
    }

    public static void openGooglePlay(Context context, String packageName) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (android.content.ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public static void openUrl(Context context, String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        context.startActivity(i);
    }

    public static String checkAppExists(Context context, String packageName) {
        try {
            PackageInfo packageInfo;
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                Log.d(TAG, packageInfo.toString());
                Log.d(TAG, packageInfo.packageName+" "+packageInfo.versionName+" "+packageInfo.versionCode);
                return packageInfo.versionName;
            }
            else {
                return null;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public static class VulcanRequestEncryptionUtils {
        private static final String ALGORITHM_NAME = "SHA1withRSA";
        private static final String CERT_TYPE = "pkcs12";
        private static final String CONTAINER_NAME = "LoginCert";
        private static final String PASSWORD = "CE75EA598C7743AD9B0B7328DED85B06";

        public static String signContent(byte[] contents, final InputStream cert) throws IOException, GeneralSecurityException, NullPointerException {
            final KeyStore instance = KeyStore.getInstance(CERT_TYPE);
            instance.load(cert, PASSWORD.toCharArray());
            final PrivateKey privateKey = (PrivateKey) instance.getKey(CONTAINER_NAME, PASSWORD.toCharArray());
            final Signature instance2 = Signature.getInstance(ALGORITHM_NAME);
            instance2.initSign(privateKey);
            instance2.update(contents);
            return Base64.encodeToString(instance2.sign(), Base64.DEFAULT).replace("\n", "");
        }
    }

    public static class VulcanQrEncryptionUtils {
        private static final String PASSWORD = "tDVS4ykCBBAeN33h";

        public static String decode(String content) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, ShortBufferException, BadPaddingException, IllegalBlockSizeException {
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            byte[] input = Base64.decode(content, Base64.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(PASSWORD.getBytes(), "AES"));
            byte[] plainText = new byte[cipher.getOutputSize(input.length)];
            int ptLength = cipher.update(input, 0, input.length, plainText, 0);
            ptLength += cipher.doFinal(plainText, ptLength);
            return new String(plainText);
        }
    }

    public static class AESCrypt
    {
        private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

        public static String encrypt(String value, String key) throws Exception
        {
            Key keyObj = generateKey(key);
            Cipher cipher = Cipher.getInstance(AESCrypt.ALGORITHM);
            byte[] iv = new byte[16];
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keyObj, ivSpec);
            byte [] encryptedByteValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            String encryptedValue64 = Base64.encodeToString(encryptedByteValue, Base64.DEFAULT);
            return encryptedValue64;

        }

        public static String decrypt(String value, String key) throws Exception
        {
            Key keyObj = generateKey(key);
            Cipher cipher = Cipher.getInstance(AESCrypt.ALGORITHM);
            byte[] iv = new byte[16];
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keyObj, ivSpec);
            byte[] decryptedValue64 = Base64.decode(value, Base64.DEFAULT);
            byte [] decryptedByteValue = cipher.doFinal(decryptedValue64);
            String decryptedValue = new String(decryptedByteValue, StandardCharsets.UTF_8);
            return decryptedValue;

        }

        private static Key generateKey(String key) throws Exception
        {
            Key keyObj = new SecretKeySpec(key.getBytes(),AESCrypt.ALGORITHM);
            return keyObj;
        }
    }

    private static String Base64UrlSafe(byte[] data)
    {
        String enc = Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_WRAP);
        return enc.replace("=", "");
    }

    private final static char[] hexArray = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String HmacSHA512(String message, String secret)
    {
        try {
            Mac sha_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA512");
            sha_HMAC.init(secret_key);

            return Base64UrlSafe(sha_HMAC.doFinal(message.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String HmacMD5(String message, String secret)
    {
        try {
            Mac sha_HMAC = Mac.getInstance("HmacMD5");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacMD5");
            sha_HMAC.init(secret_key);

            return bytesToHex(sha_HMAC.doFinal(message.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }



    public static String MD5(String message)
    {
        try {
            // Create MD5 Hash
            MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            return Base64UrlSafe(md.digest(message.getBytes()));
        }catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static float getGradeValue(String grade) {
        switch (grade) {
            case "1-":
                return 0.75f;
            case "1":
                return 1.00f;
            case "1+":
                return 1.50f;
            case "2-":
                return 1.75f;
            case "2":
                return 2.00f;
            case "2+":
                return 2.50f;
            case "3-":
                return 2.75f;
            case "3":
                return 3.00f;
            case "3+":
                return 3.50f;
            case "4-":
                return 3.75f;
            case "4":
                return 4.00f;
            case "4+":
                return 4.50f;
            case "5-":
                return 4.75f;
            case "5":
                return 5.00f;
            case "5+":
                return 5.50f;
            case "6-":
                return 5.75f;
            case "6":
                return 6.00f;
            case "6+":
                return 6.50f;
        }
        return 0.00f;
    }
    
    public static int getVulcanGradeColor(String name) {
        switch (name) {
            case "1-":
            case "1":
            case "1+":
                return 0xffd65757;
            case "2-":
            case "2":
            case "2+":
                return 0xff9071b3;
            case "3-":
            case "3":
            case "3+":
                return 0xffd2ab24;
            case "4-":
            case "4":
            case "4+":
                return 0xff50b6d6;
            case "5-":
            case "5":
            case "5+":
                return 0xff2cbd92;
            case "6-":
            case "6":
            case "6+":
                return 0xff91b43c;
            default:
                return 0xff3D5F9C;
        }
    }

    public static int getWordGradeValue(String grade) {
        switch (grade) {
            case "niedostateczny":
                return 1;
            case "dopuszczający":
                return 2;
            case "dostateczny":
                return 3;
            case "dobry":
                return 4;
            case "bardzo dobry":
                return 5;
            case "celujący":
                return 6;
            default:
                return 0;
        }
    }

    public static boolean zipFileAtPath(String sourcePath, String toLocation) {
        final int BUFFER = 2048;

        File sourceFile = new File(sourcePath);
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            if (sourceFile.isDirectory()) {
                zipSubFolder(out, sourceFile, sourceFile.getParent().length());
            } else {
                byte[] data = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(sourcePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(getLastPathComponent(sourcePath));
                entry.setTime(sourceFile.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*
     *
     * Zips a subfolder
     *
     */

    private static void zipSubFolder(ZipOutputStream out, File folder,
                              int basePathLength) throws IOException {

        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte[] data = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                entry.setTime(file.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    /*
     * gets the last path component
     *
     * Example: getLastPathComponent("downloads/example/fileToZip");
     * Result: "fileToZip"
     */
    public static String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        String lastPathComponent = segments[segments.length - 1];
        return lastPathComponent;
    }

    public static String readableFileSize(long size) {
        if(size <= 0) return "0B";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB", "PB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + units[digitGroups];
    }

    public static int monthFromName(String name) {
        int month = 1;
        switch (name) {
            case "stycznia":
                month = 1;
                break;
            case "lutego":
                month = 2;
                break;
            case "marca":
                month = 3;
                break;
            case "kwietnia":
                month = 4;
                break;
            case "maja":
                month = 5;
                break;
            case "czerwca":
                month = 6;
                break;
            case "lipca":
                month = 7;
                break;
            case "sierpnia":
                month = 8;
                break;
            case "września":
                month = 9;
                break;
            case "października":
                month = 10;
                break;
            case "listopada":
                month = 11;
                break;
            case "grudnia":
                month = 12;
                break;
        }
        return month;
    }

    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        boolean first = true;
        while ((line = reader.readLine()) != null) {
            if (!(first && !(first = !first)))
                sb.append("\n");
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile (File fl) throws Exception {
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    public static void openFile(Context context, File file) {
        try {
            Uri uri = Uri.fromFile(file);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            }

            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtensionFromFileName(file.toString()));

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType == null ? "*/*" : mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Nie ma aplikacji która otwiera ten typ pliku", Toast.LENGTH_LONG).show();
        }
    }

    private static File storageDir = null;
    public static File getStorageDir() {
        if (storageDir != null)
            return storageDir;
        storageDir = Environment.getExternalStoragePublicDirectory("Szkolny.eu");
        storageDir.mkdirs();
        return storageDir;
    }

    public static void writeStringToFile(File file, String data) throws IOException {
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
        outputStreamWriter.write(data);
        outputStreamWriter.close();
    }

    public static String getExtensionFromFileName(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".")+1).toLowerCase();
    }


    public static String getCurrentTimeUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date=cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(date);
    }

    public static String getCurrentSchoolYear() {
        pl.szczodrzynski.edziennik.utils.models.Date today = pl.szczodrzynski.edziennik.utils.models.Date.getToday();
        if (today.month >= 9) return today.year + "/" + (today.year + 1);
        else return (today.year - 1) + "/" + today.year;
    }
}
