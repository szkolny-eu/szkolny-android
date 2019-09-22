package pl.szczodrzynski.edziennik.api;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import im.wangchao.mhttp.Request;
import im.wangchao.mhttp.Response;
import pl.szczodrzynski.edziennik.R;


public class AppError {
    public static final int CODE_OTHER = 0;
    public static final int CODE_OK = 1;
    public static final int CODE_NO_INTERNET = 10;
    public static final int CODE_SSL_ERROR = 13;
    public static final int CODE_ARCHIVED = 5;
    public static final int CODE_MAINTENANCE = 6;
    public static final int CODE_LOGIN_ERROR = 7;
    public static final int CODE_ACCOUNT_MISMATCH = 8;
    public static final int CODE_APP_SERVER_ERROR = 9;
    public static final int CODE_MULTIACCOUNT_SETUP = 12;
    public static final int CODE_TIMEOUT = 11;
    public static final int CODE_PROFILE_NOT_FOUND = 14;
    public static final int CODE_ATTACHMENT_NOT_AVAILABLE = 28;
    // user's fault
    public static final int CODE_INVALID_LOGIN = 2;
    public static final int CODE_INVALID_SERVER_ADDRESS = 21;
    public static final int CODE_INVALID_SCHOOL_NAME = 22;
    public static final int CODE_INVALID_DEVICE = 23;
    public static final int CODE_OLD_PASSWORD = 4;
    public static final int CODE_INVALID_TOKEN = 24;
    public static final int CODE_EXPIRED_TOKEN = 27;
    public static final int CODE_INVALID_SYMBOL = 25;
    public static final int CODE_INVALID_PIN = 26;
    public static final int CODE_LIBRUS_NOT_ACTIVATED = 29;
    public static final int CODE_SYNERGIA_NOT_ACTIVATED = 32;
    public static final int CODE_LIBRUS_DISCONNECTED = 31;
    public static final int CODE_PROFILE_ARCHIVED = 30;


    public static final int CODE_INTERNAL_MISSING_DATA = 100;
    // internal errors - not for user's information.
    // these error codes are processed in API main classes

    public String TAG;
    public int line;
    public int errorCode;
    public String errorText;
    public Response response;
    public Request request;
    public Throwable throwable;
    public String apiResponse;

    public AppError(String TAG, int line, int errorCode, String errorText, Response response, Request request, Throwable throwable, String apiResponse) {
        this.TAG = TAG;
        this.line = line;
        this.errorCode = errorCode;
        this.errorText = errorText;
        this.response = response;
        this.request = request;
        this.throwable = throwable;
        this.apiResponse = apiResponse;
    }

    public AppError(String TAG, int line, int errorCode) {
        this(TAG, line, errorCode, null, null, null, null, null);
    }
    public AppError(String TAG, int line, int errorCode, Response response, Throwable throwable) {
        this(TAG, line, errorCode, null, response, response == null ? null : response.request(), throwable, null);
    }
    public AppError(String TAG, int line, int errorCode, Response response) {
        this(TAG, line, errorCode, null, response, response == null ? null : response.request(), null, null);
    }
    public AppError(String TAG, int line, int errorCode, Throwable throwable, String apiResponse) {
        this(TAG, line, errorCode, null, null, null, throwable, apiResponse);
    }
    public AppError(String TAG, int line, int errorCode, Throwable throwable, JsonObject apiResponse) {
        this(TAG, line, errorCode, null, null, null, throwable, apiResponse == null ? null : apiResponse.toString());
    }
    public AppError(String TAG, int line, int errorCode, String errorText, Response response, JsonObject apiResponse) {
        this(TAG, line, errorCode, errorText, response, response == null ? null : response.request(), null, apiResponse == null ? null : apiResponse.toString());
    }
    public AppError(String TAG, int line, int errorCode, String errorText, Response response, String apiResponse) {
        this(TAG, line, errorCode, errorText, response, response == null ? null : response.request(), null, apiResponse);
    }
    public AppError(String TAG, int line, int errorCode, String errorText, String apiResponse) {
        this(TAG, line, errorCode, errorText, null, null, null, apiResponse);
    }
    public AppError(String TAG, int line, int errorCode, String errorText, JsonObject apiResponse) {
        this(TAG, line, errorCode, errorText, null, null, null, apiResponse == null ? null : apiResponse.toString());
    }
    public AppError(String TAG, int line, int errorCode, String errorText) {
        this(TAG, line, errorCode, errorText, null, null, null, null);
    }
    public AppError(String TAG, int line, int errorCode, JsonObject apiResponse) {
        this(TAG, line, errorCode, null, null, null, null, apiResponse.toString());
    }
    public AppError(String TAG, int line, int errorCode, Response response, Throwable throwable, JsonObject apiResponse) {
        this(TAG, line, errorCode, null, response, response == null ? null : response.request(), throwable, apiResponse == null ? null : apiResponse.toString());
    }
    public AppError(String TAG, int line, int errorCode, Response response, Throwable throwable, String apiResponse) {
        this(TAG, line, errorCode, null, response, response == null ? null : response.request(), throwable, apiResponse);
    }
    public AppError(String TAG, int line, int errorCode, Response response, String apiResponse) {
        this(TAG, line, errorCode, null, response, response == null ? null : response.request(), null, apiResponse);
    }
    public AppError(String TAG, int line, int errorCode, Response response, JsonObject apiResponse) {
        this(TAG, line, errorCode, null, response, response == null ? null : response.request(), null, apiResponse == null ? null : apiResponse.toString());
    }

    public String getDetails(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append(stringErrorCode(context, errorCode, errorText)).append("\n");
        sb.append("(").append(stringErrorType(errorCode)).append("#").append(errorCode).append(")\n");
        sb.append("at ").append(TAG).append(":").append(line).append("\n");
        sb.append("\n");
        if (throwable == null)
            sb.append("Throwable is null");
        else
            sb.append(Log.getStackTraceString(throwable));
        sb.append("\n");
        sb.append(Build.MANUFACTURER).append(" ").append(Build.BRAND).append(" ").append(Build.MODEL).append(" ").append(Build.DEVICE).append("\n");

        return sb.toString();
    }

    public interface GetApiResponseCallback {
        void onSuccess(String apiResponse);
    }
    /**
     *
     * @param context a Context
     * @param apiResponseCallback a callback executed on a worker thread
     */
    public void getApiResponse(Context context, GetApiResponseCallback apiResponseCallback) {
        StringBuilder sb = new StringBuilder();
        sb.append("Request:\n");
        if (request != null) {
            sb.append(request.method()).append(" ").append(request.url().toString()).append("\n");
            sb.append(request.headers().toString()).append("\n");
            sb.append("\n");
            sb.append(request.bodyToString()).append("\n\n");
        }
        else
            sb.append("null\n\n");

        if (apiResponse == null && response != null)
            apiResponse = response.parserErrorBody;

        sb.append("Response:\n");
        if (response != null) {
            sb.append(response.code()).append(" ").append(response.message()).append("\n");
            sb.append(response.headers().toString()).append("\n");
            sb.append("\n");
            if (apiResponse == null) {
                if (Thread.currentThread().getName().equals("main")) {
                    AsyncTask.execute(() -> {
                        if (response.raw().body() != null) {
                            try {
                                sb.append(response.raw().body().string());
                            } catch (Exception e) {
                                sb.append("Exception while getting response body:\n").append(Log.getStackTraceString(e));
                            }
                        }
                        else {
                            sb.append("null");
                        }
                        apiResponseCallback.onSuccess(sb.toString());
                    });
                }
                else {
                    if (response.raw().body() != null) {
                        try {
                            sb.append(response.raw().body().string());
                        } catch (Exception e) {
                            sb.append("Exception while getting response body:\n").append(Log.getStackTraceString(e));
                        }
                    }
                    else {
                        sb.append("null");
                    }
                    apiResponseCallback.onSuccess(sb.toString());
                }
                return;
            }
        }
        else
            sb.append("null\n\n");

        sb.append("API Response:\n");
        if (apiResponse != null) {
            sb.append(apiResponse).append("\n\n");
        }
        else {
            sb.append("null\n\n");
        }

        apiResponseCallback.onSuccess(sb.toString());
    }

    public AppError changeIfCodeOther() {
        if (errorCode != CODE_OTHER && errorCode != CODE_MAINTENANCE)
            return this;
        if (throwable instanceof UnknownHostException)
            errorCode = CODE_NO_INTERNET;
        else if (throwable instanceof SSLException)
            errorCode = CODE_SSL_ERROR;
        else if (throwable instanceof SocketTimeoutException)
            errorCode = CODE_TIMEOUT;
        else if (throwable instanceof InterruptedIOException)
            errorCode = CODE_NO_INTERNET;
        else if (response != null &&
                (response.code() == 424
                        || response.code() == 400
                        || response.code() == 401
                        || response.code() == 500
                        || response.code() == 503
                        || response.code() == 404))
            errorCode = CODE_MAINTENANCE;
        return this;
    }

    public String asReadableString(Context context) {
        return stringErrorCode(context, errorCode, errorText) + (errorCode == CODE_MAINTENANCE && errorText != null && !errorText.isEmpty() ? " ("+errorText+")" : "");
    }

    public static String stringErrorCode(Context context, int errorCode, String errorText)
    {
        switch (errorCode) {
            case CODE_OK:
                return context.getString(R.string.sync_error_ok);
            case CODE_INVALID_LOGIN:
                return context.getString(R.string.sync_error_invalid_login);
            case CODE_LOGIN_ERROR:
                return context.getString(R.string.sync_error_login_error);
            case CODE_INVALID_DEVICE:
                return context.getString(R.string.sync_error_invalid_device);
            case CODE_OLD_PASSWORD:
                return context.getString(R.string.sync_error_old_password);
            case CODE_ARCHIVED:
                return context.getString(R.string.sync_error_archived);
            case CODE_MAINTENANCE:
                return context.getString(R.string.sync_error_maintenance);
            case CODE_NO_INTERNET:
                return context.getString(R.string.sync_error_no_internet);
            case CODE_ACCOUNT_MISMATCH:
                return context.getString(R.string.sync_error_account_mismatch);
            case CODE_APP_SERVER_ERROR:
                return context.getString(R.string.sync_error_app_server);
            case CODE_TIMEOUT:
                return context.getString(R.string.sync_error_timeout);
            case CODE_SSL_ERROR:
                return context.getString(R.string.sync_error_ssl);
            case CODE_INVALID_SERVER_ADDRESS:
                return context.getString(R.string.sync_error_invalid_server_address);
            case CODE_INVALID_SCHOOL_NAME:
                return context.getString(R.string.sync_error_invalid_school_name);
            case CODE_PROFILE_NOT_FOUND:
                return context.getString(R.string.sync_error_profile_not_found);
            case CODE_INVALID_TOKEN:
                return context.getString(R.string.sync_error_invalid_token);
            case CODE_ATTACHMENT_NOT_AVAILABLE:
                return context.getString(R.string.sync_error_attachment_not_available);
            case CODE_LIBRUS_NOT_ACTIVATED:
                return context.getString(R.string.sync_error_librus_not_activated);
            case CODE_PROFILE_ARCHIVED:
                return context.getString(R.string.sync_error_profile_archived);
            case CODE_LIBRUS_DISCONNECTED:
                return context.getString(R.string.sync_error_librus_disconnected);
            case CODE_SYNERGIA_NOT_ACTIVATED:
                return context.getString(R.string.sync_error_synergia_not_activated);
            default:
            case CODE_MULTIACCOUNT_SETUP:
            case CODE_OTHER:
                return errorText != null ? errorText : context.getString(R.string.sync_error_unknown);
        }
    }
    public static String stringErrorType(int errorCode)
    {
        switch (errorCode) {
            default:
            case CODE_OTHER: return "CODE_OTHER";
            case CODE_OK: return "CODE_OK";
            case CODE_NO_INTERNET: return "CODE_NO_INTERNET";
            case CODE_SSL_ERROR: return "CODE_SSL_ERROR";
            case CODE_ARCHIVED: return "CODE_ARCHIVED";
            case CODE_MAINTENANCE: return "CODE_MAINTENANCE";
            case CODE_LOGIN_ERROR: return "CODE_LOGIN_ERROR";
            case CODE_ACCOUNT_MISMATCH: return "CODE_ACCOUNT_MISMATCH";
            case CODE_APP_SERVER_ERROR: return "CODE_APP_SERVER_ERROR";
            case CODE_MULTIACCOUNT_SETUP: return "CODE_MULTIACCOUNT_SETUP";
            case CODE_TIMEOUT: return "CODE_TIMEOUT";
            case CODE_PROFILE_NOT_FOUND: return "CODE_PROFILE_NOT_FOUND";
            case CODE_INVALID_LOGIN: return "CODE_INVALID_LOGIN";
            case CODE_INVALID_SERVER_ADDRESS: return "CODE_INVALID_SERVER_ADDRESS";
            case CODE_INVALID_SCHOOL_NAME: return "CODE_INVALID_SCHOOL_NAME";
            case CODE_INVALID_DEVICE: return "CODE_INVALID_DEVICE";
            case CODE_OLD_PASSWORD: return "CODE_OLD_PASSWORD";
            case CODE_INVALID_TOKEN: return "CODE_INVALID_TOKEN";
            case CODE_EXPIRED_TOKEN: return "CODE_EXPIRED_TOKEN";
            case CODE_INVALID_SYMBOL: return "CODE_INVALID_SYMBOL";
            case CODE_INVALID_PIN: return "CODE_INVALID_PIN";
            case CODE_ATTACHMENT_NOT_AVAILABLE: return "CODE_ATTACHMENT_NOT_AVAILABLE";
            case CODE_LIBRUS_NOT_ACTIVATED: return "CODE_LIBRUS_NOT_ACTIVATED";
            case CODE_PROFILE_ARCHIVED: return "CODE_PROFILE_ARCHIVED";
            case CODE_LIBRUS_DISCONNECTED: return "CODE_LIBRUS_DISCONNECTED";
            case CODE_SYNERGIA_NOT_ACTIVATED: return "CODE_SYNERGIA_NOT_ACTIVATED";
        }
    }
}
