package pl.szczodrzynski.edziennik.network;

import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.util.Pair;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import im.wangchao.mhttp.Request;
import im.wangchao.mhttp.Response;
import im.wangchao.mhttp.ThreadMode;
import im.wangchao.mhttp.callback.JsonCallbackHandler;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull;
import pl.szczodrzynski.edziennik.utils.Utils;

import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_ENABLED;

public class ServerRequest {
    private App app;
    private String url;
    private List<Pair<String, Object>> params;
    private String username = "";
    private String source = "";

    public ServerRequest(App app, String url, String source) {
        this(app, url, source, app.profile);
    }

    public ServerRequest(App app, String url, String source, ProfileFull profileFull) {
        this.app = app;
        this.url = url;
        this.params = new ArrayList<>();
        this.username = (profileFull != null && profileFull.getRegistration() == REGISTRATION_ENABLED ? profileFull.getUsernameId() : app.deviceId);
        this.source = source;
        if (profileFull != null && profileFull.getRegistration() == REGISTRATION_ENABLED) {
            this.setBodyParameter("login_type", Integer.toString(profileFull.getLoginStoreType()));
            this.setBodyParameter("name_long", profileFull.getStudentNameLong());
            this.setBodyParameter("name_short", profileFull.getStudentNameShort());
            //if (Looper.myLooper() == Looper.getMainLooper()) {
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                this.setBodyParameter("team_ids", "UI_THREAD");
            }
            else {
                this.setBodyParameter("team_ids", app.gson.toJson(app.db.teamDao().getAllCodesNow(profileFull.getId())));
            }
        }
    }

    public ServerRequest withUsername(String username) {
        this.username = username;
        return this;
    }

    public ServerRequest setBodyParameter(String name, String value) {
        params.add(new Pair<>(name, value));
        return this;
    }

    public interface JsonCallback {
        void onCallback(Exception e, JsonObject result);
    }

    private String sign(String signature, long timestamp) {
        String password = "bmllX21hX3Rha19sYXR3bw=="+ BuildConfig.VERSION_CODE + timestamp;
        return Utils.HmacMD5(password, signature);
    }

    public void run(JsonCallback callback) {
        long timestamp = System.currentTimeMillis() / 1000;
        Request.builder()
                .url(url)
                .callbackThreadMode(ThreadMode.MAIN)
                .userAgent("Szkolny.eu/"+BuildConfig.VERSION_NAME+" (Android "+Build.VERSION.RELEASE+"; "+Build.MANUFACTURER+" "+Build.MODEL+")")
                .addParams(params)
                .addParameter("username", username)
                .addParameter("app_version_build_type", BuildConfig.BUILD_TYPE)
                .addParameter("app_version_code", Integer.toString(BuildConfig.VERSION_CODE))
                .addParameter("app_version", BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE + " (" + BuildConfig.VERSION_CODE + ")")
                .addParameter("device_id", Settings.Secure.getString(app.getContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                .addParameter("device_model", Build.MANUFACTURER+" "+Build.MODEL)
                .addParameter("device_os_version", Build.VERSION.RELEASE)
                .addParameter("fcm_token", app.appConfig.fcmToken)
                .addParameter("signature", sign(app.signature, timestamp))
                .addParameter("signature_timestamp", timestamp)
                .addParameter("package_name", "pl.szczodrzynski.edziennik")
                .addParameter("source", source)
                .addParameter("update_frequency", app.appConfig.registerSyncEnabled ? app.appConfig.registerSyncInterval : -1)
                .post()
                .callback(new JsonCallbackHandler() {
                    @Override
                    public void onSuccess(JsonObject data, im.wangchao.mhttp.Response response) {
                        super.onSuccess(data, response);
                        callback.onCallback(null, data);
                    }

                    @Override
                    public void onFailure(im.wangchao.mhttp.Response response, Throwable throwable) {
                        super.onFailure(response, throwable);
                        callback.onCallback(throwable instanceof Exception ? (Exception) throwable : null, null);
                    }
                })
                .build()
                .enqueue();
    }
    public JsonObject runSync() throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        Response response = Request.builder()
                .url(url)
                .callbackThreadMode(ThreadMode.MAIN)
                .userAgent("Szkolny.eu/"+BuildConfig.VERSION_NAME+" (Android "+Build.VERSION.RELEASE+"; "+Build.MANUFACTURER+" "+Build.MODEL+")")
                .addParams(params)
                .addParameter("username", username)
                .addParameter("app_version_build_type", BuildConfig.BUILD_TYPE)
                .addParameter("app_version_code", Integer.toString(BuildConfig.VERSION_CODE))
                .addParameter("app_version", BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE + " (" + BuildConfig.VERSION_CODE + ")")
                .addParameter("device_id", Settings.Secure.getString(app.getContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                .addParameter("device_model", Build.MANUFACTURER+" "+Build.MODEL)
                .addParameter("device_os_version", Build.VERSION.RELEASE)
                .addParameter("fcm_token", app.appConfig.fcmToken)
                .addParameter("signature", sign(app.signature, timestamp))
                .addParameter("signature_timestamp", timestamp)
                .addParameter("package_name", "pl.szczodrzynski.edziennik")
                .addParameter("source", source)
                .addParameter("update_frequency", app.appConfig.registerSyncEnabled ? app.appConfig.registerSyncInterval : -1)
                .post()
                .build()
                .execute();
        return new JsonCallbackHandler().backgroundParser(response);
    }
}
