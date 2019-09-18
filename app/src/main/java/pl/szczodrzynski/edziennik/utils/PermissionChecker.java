package pl.szczodrzynski.edziennik.utils;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public class PermissionChecker {

    private Context mContext;

    public PermissionChecker(Context context) {
        mContext = context;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean canDrawOverOtherApps() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(mContext);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void requestDrawOverOtherApps() {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mContext.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + mContext.getPackageName())));
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public Intent intentDrawOverOtherApps() {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + mContext.getPackageName()));
        }
        return null;
    }

    public boolean canGetUsageStats() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppOpsManager appOps = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
            int mode = 0;
            try {
                mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(), mContext.getPackageName());
            } catch (java.lang.IllegalArgumentException e) {
                e.printStackTrace();
            }
            boolean granted = false;
            if (mode == AppOpsManager.MODE_DEFAULT) {
                granted = (mContext.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
            } else {
                granted = (mode == AppOpsManager.MODE_ALLOWED);
            }
            return granted;
        }
        else
        {
            return true;
        }
    }

    public void requestUsageStatsPermission() {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mContext.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    public Intent intentUsageStatsPermission() {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        }
        return null;
    }

    public boolean isAccessibilityEnabled() {

        AccessibilityManager am = (AccessibilityManager) mContext
                .getSystemService(Context.ACCESSIBILITY_SERVICE);

        List<AccessibilityServiceInfo> runningServices = am
                .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : runningServices) {
            if ("pl.szczodrzynski.topd/.OverlayAccessibilityService".equals(service.getId())) {
                return true;
            }
        }

        return false;
    }

    public void requestAccessibilityService() {
        mContext.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    public Intent intentAccessibilityService() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    public boolean isNotificationListenerEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String pkgName = mContext.getPackageName();
            final String flat = Settings.Secure.getString(mContext.getContentResolver(),
                    "enabled_notification_listeners");
            if (!TextUtils.isEmpty(flat)) {
                final String[] names = flat.split(":");
                for (String name : names) {
                    final ComponentName cn = ComponentName.unflattenFromString(name);
                    if (cn != null) {
                        if (TextUtils.equals(pkgName, cn.getPackageName())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        else
        {
            return false;
        }
    }

    public void requestNotificationListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mContext.startActivity(intentNotificationListener());
        }
    }

    public Intent intentNotificationListener()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        }
        return null;
    }

    public boolean canRequestApkInstall() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || mContext.getPackageManager().canRequestPackageInstalls();
    }

    public void requestApkInstall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startActivity(intentApkInstall());
        }
    }

    public Intent intentApkInstall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES",
                    Uri.parse("package:" + mContext.getPackageName()));
        }
        return null;
    }

}