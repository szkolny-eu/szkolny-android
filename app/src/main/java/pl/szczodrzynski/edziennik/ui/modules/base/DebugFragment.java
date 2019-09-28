package pl.szczodrzynski.edziennik.ui.modules.base;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.yuyh.jsonviewer.library.JsonRecyclerView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.utils.Themes;

public class DebugFragment extends Fragment {
    App app;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        app = (App)getActivity().getApplication();
        getContext().getTheme().applyStyle(Themes.INSTANCE.getAppTheme(), true);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_debug, container, false);
    }

    private static class SimpleObj {
        Object value;
    }
    private static Object target;
    private static Field targetField = null;
    private static Object temp;

    private static Object getByPath(App app, Object parent, String path, boolean setGlobal) throws NoSuchMethodException, NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        Object target = parent;
        Field targetField = null;
        List<String> pathList = new ArrayList<>();
        Matcher matcher = Pattern.compile("(.+?(?:\\(.*?\\))?)\\.").matcher(path+".");
        while (matcher.find()) {
            pathList.add(matcher.group(1));
        }
        for (String el: pathList) {
            if (targetField != null) {
                target = targetField.get(target);
                targetField = null;
            }
            if (el.equals("temp")) {
                target = temp;
                continue;
            }
            if (el.endsWith(")")) {
                String methodName = el.substring(0, el.indexOf("("));
                String parameters = el.substring(el.indexOf("(")+1, el.lastIndexOf(")"));
                Class[] paramTypes = new Class[]{};
                Object[] paramValues = new Object[]{};
                if (!parameters.isEmpty()) {
                    List<Class> paramTypeList = new ArrayList<>();
                    List<Object> paramValueList = new ArrayList<>();
                    for (String parameter: parameters.split(",\\s?")) {
                        if (parameter.equals("null")) {
                            paramTypeList.add(null);
                            paramValueList.add(null);
                        }
                        if (parameter.equals("temp") || (parameter.contains(".") && !parameter.startsWith("\""))) {
                            // this parameter is a path
                            if (parameter.startsWith("app.")) {
                                parameter = parameter.replaceFirst("app\\.", "");
                            }
                            Object param = getByPath(app, app, parameter, false);
                            paramTypeList.add(param.getClass());
                            paramValueList.add(param);
                            continue;
                        }
                        String forceType = null;
                        if (parameter.toLowerCase().endsWith("d")) {
                            forceType = "d";
                            parameter = parameter.substring(0, parameter.length()-1);
                        } else if (parameter.toLowerCase().endsWith("l")) {
                            forceType = "l";
                            parameter = parameter.substring(0, parameter.length()-1);
                        } else if (parameter.toLowerCase().endsWith("f")) {
                            forceType = "f";
                            parameter = parameter.substring(0, parameter.length()-1);
                        } else if (parameter.toLowerCase().endsWith("i")) {
                            forceType = "i";
                            parameter = parameter.substring(0, parameter.length()-1);
                        }
                        SimpleObj obj = app.gson.fromJson("{\"value\":"+parameter+"}", SimpleObj.class);
                        Class type = obj.value.getClass();
                        Object value = obj.value;
                        if ("d".equals(forceType)) {
                            value = Double.valueOf(parameter);
                            type = Double.TYPE;
                        } else if ("l".equals(forceType)) {
                            value = Long.valueOf(parameter);
                            type = Long.TYPE;
                        } else if ("f".equals(forceType)) {
                            value = Float.valueOf(parameter);
                            type = Float.TYPE;
                        } else if ("i".equals(forceType)) {
                            value = Integer.valueOf(parameter);
                            type = Integer.TYPE;
                        }
                        paramTypeList.add(type);
                        paramValueList.add(value);
                    }
                    paramTypes = paramTypeList.toArray(new Class[0]);
                    paramValues = paramValueList.toArray(new Object[0]);
                }
                try {
                    Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
                    method.setAccessible(true);
                    target = method.invoke(target, paramValues);
                }
                catch (NoSuchMethodException e) {
                    Method method = target.getClass().getMethod(methodName, paramTypes);
                    target = method.invoke(target, paramValues);
                }
            }
            else {
                try {
                    targetField = target.getClass().getDeclaredField(el);
                    targetField.setAccessible(true);
                }
                catch (NoSuchFieldException e) {
                    targetField = target.getClass().getField(el);
                }
            }
        }
        if (setGlobal) {
            DebugFragment.target = target;
            DebugFragment.targetField = targetField;
        }
        return target;
    }

    public static String runCommand(App app, String cmdBatch) {
        for (String cmd: cmdBatch.split(";(?:\\r|\\n|\\r\\n|\\n\\r)?")) {
            cmd = cmd.trim();
            if (cmd.isEmpty())
                continue;

            String[] data;
            String path;
            String valueToSet;
            if (cmd.contains(" = ")) {
                data = cmd.replaceFirst("run ", "").split(" = ");
                path = data[0];
                valueToSet = data[1];
            } else {
                path = cmd.replaceFirst("run ", "");
                valueToSet = null;
            }

            target = app;
            try {
                boolean setToTemp = false;
                if (path.startsWith("temp=")) {
                    path = path.replaceFirst("temp=", "");
                    setToTemp = true;
                }
                if (path.startsWith("app.")) {
                    path = path.replaceFirst("app\\.", "");
                }
                getByPath(app, target, path, true);
                // set the value if specified
                if (valueToSet != null) {
                    targetField.set(target, app.gson.fromJson(valueToSet, targetField.getGenericType()));
                    targetField = null;
                }
                if (targetField != null) {
                    target = targetField.get(target);
                }
                if (setToTemp) {
                    temp = target;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Log.getStackTraceString(e);
            }
        }
        return app.gson.toJson(target);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        getView().findViewById(R.id.debugRegister).setOnClickListener(v -> {
            String text = ((TextInputEditText) getView().findViewById(R.id.runText)).getText().toString();
            JsonRecyclerView mRecyclerView = getView().findViewById(R.id.rv_json);
            String result = runCommand(app, "run " + text);
            try {
                mRecyclerView.bindJson(result);
            }
            catch (Exception e) {
                new MaterialDialog.Builder(getActivity())
                        .title("Result")
                        .content(result)
                        .positiveText(R.string.ok)
                        .show();
            }
            mRecyclerView.setTextSize(20);
        });
        getView().findViewById(R.id.debugAppconfig).setOnClickListener(v -> {
            JsonRecyclerView mRecyclerView = getView().findViewById(R.id.rv_json);
            // bind json
            mRecyclerView.bindJson(new Gson().toJson(app.appConfig));
            mRecyclerView.setTextSize(20);
        });
        getView().findViewById(R.id.debugAppprofile).setOnClickListener(v -> {
            JsonRecyclerView mRecyclerView = getView().findViewById(R.id.rv_json);
            // bind json
            mRecyclerView.bindJson(new Gson().toJson(app.profile));
            mRecyclerView.setTextSize(20);
        });
    }

}
