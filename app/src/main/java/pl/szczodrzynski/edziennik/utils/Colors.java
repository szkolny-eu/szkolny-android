package pl.szczodrzynski.edziennik.utils;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import androidx.core.graphics.ColorUtils;
import pl.szczodrzynski.edziennik.datamodels.Grade;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

public class Colors {

    public static int legibleTextColor(int bgColor){
        return (ColorUtils.calculateLuminance(bgColor) > 0.5 ? 0xff000000 : 0xffffffff);
    }

    private static int hsvToColor(float h, float s, float v) {
        float[] hsv = new float[]{h*360, s, v};
        return Color.HSVToColor(hsv);
    }

    public static int stringToColor(String s) {
        long seed = 1;
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(s.getBytes());
            byte[] bytes = digest.digest();
            for (byte aByte : bytes) {
                if (aByte % 7 == 0) {
                    seed += aByte;
                }
                if (aByte % 3 == 0) {
                    seed -= aByte;
                }
                if (aByte % 4 == 0) {
                    seed *= aByte;
                }
                if (aByte % 5 == 0) {
                    seed /= aByte;
                }
            }
        } catch (Exception e) {
            seed = 1234;
            //Log.d("StringToColor", "Failed.");
        }
        //Log.d("StringToColor", "Seed "+seed);
        float hue = new Random(seed).nextFloat();
        hue += 0.618033988749895f; // golden_ratio_conjugate
        hue %= 1.0f;
        //Log.d("StringToColor", "Hue "+hue);
        return hsvToColor(hue, 0.5f, 0.95f);
    }

    private static int getRandomNumberInRange(int min, int max, long seed) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random(seed);
        return r.nextInt((max - min) + 1) + min;
    }

    public static final int[] materialColors = {
            0xFFE57373, 0xFFEF5350, 0xFFF44336, 0xFFE53935, 0xFFD32F2F, 0xFFC62828, 0xFFFF5252, 0xFFFF1744, 0xFFD50000, 0xFFF06292, 0xFFEC407A, 0xFFE91E63, 0xFFD81B60, 0xFFC2185B, 0xFFAD1457,
            0xFFFF4081, 0xFFF50057, 0xFFC51162, 0xFFBA68C8, 0xFFAB47BC, 0xFF9C27B0, 0xFF8E24AA, 0xFF7B1FA2, 0xFF6A1B9A, 0xFFE040FB, 0xFFD500F9, 0xFFAA00FF, 0xFF9575CD, 0xFF7E57C2, 0xFF673AB7,
            0xFF5E35B1, 0xFF512DA8, 0xFF4527A0, 0xFF7C4DFF, 0xFF651FFF, 0xFF6200EA, 0xFF7986CB, 0xFF5C6BC0, 0xFF3F51B5, 0xFF3949AB, 0xFF303F9F, 0xFF283593, 0xFF536DFE, 0xFF3D5AFE, 0xFF304FFE,
            0xFF64B5F6, 0xFF42A5F5, 0xFF2196F3, 0xFF1E88E5, 0xFF1976D2, 0xFF1565C0, 0xFF448AFF, 0xFF2979FF, 0xFF2962FF, 0xFF4FC3F7, 0xFF29B6F6, 0xFF03A9F4, 0xFF039BE5, 0xFF0288D1, 0xFF0277BD,
            0xFF40C4FF, 0xFF00B0FF, 0xFF0091EA, 0xFF4DD0E1, 0xFF26C6DA, 0xFF00BCD4, 0xFF00ACC1, 0xFF0097A7, 0xFF00838F, 0xFF18FFFF, 0xFF00E5FF, 0xFF00B8D4, 0xFF4DB6AC, 0xFF26A69A, 0xFF009688,
            0xFF00897B, 0xFF00796B, 0xFF00695C, 0xFF64FFDA, 0xFF1DE9B6, 0xFF00BFA5, 0xFF81C784, 0xFF66BB6A, 0xFF4CAF50, 0xFF43A047, 0xFF388E3C, 0xFF2E7D32, 0xFF69F0AE, 0xFF00E676, 0xFF00C853,
            0xFFAED581, 0xFF9CCC65, 0xFF8BC34A, 0xFF7CB342, 0xFF689F38, 0xFF558B2F, 0xFFB2FF59, 0xFF76FF03, 0xFF64DD17, 0xFFDCE775, 0xFFD4E157, 0xFFCDDC39, 0xFFC0CA33, 0xFFAFB42B, 0xFF9E9D24,
            0xFFEEFF41, 0xFFC6FF00, 0xFFAEEA00, 0xFFFFF176, 0xFFFFEE58, 0xFFFFEB3B, 0xFFFDD835, 0xFFFBC02D, 0xFFF9A825, 0xFFF57F17, 0xFFFFFF00, 0xFFFFEA00, 0xFFFFD600, 0xFFFFD54F, 0xFFFFCA28,
            0xFFFFC107, 0xFFFFB300, 0xFFFFA000, 0xFFFF8F00, 0xFFFF6F00, 0xFFFFD740, 0xFFFFC400, 0xFFFFAB00, 0xFFFFB74D, 0xFFFFA726, 0xFFFF9800, 0xFFFB8C00, 0xFFF57C00, 0xFFEF6C00, 0xFFE65100,
            0xFFFFAB40, 0xFFFF9100, 0xFFFF6D00, 0xFFFF8A65, 0xFFFF7043, 0xFFFF5722, 0xFFF4511E, 0xFFE64A19, 0xFFD84315, 0xFFBF360C, 0xFFFF6E40, 0xFFFF3D00, 0xFFDD2C00, 0xFF8D6E63, 0xFF795548,
            0xFF6D4C41
    };

    public static int stringToMaterialColor(String s) {
        long seed = 1;
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(s.getBytes());
            byte[] bytes = digest.digest();
            for (byte aByte : bytes) {
                if (aByte % 7 == 0) {
                    seed += aByte;
                }
                if (aByte % 3 == 0) {
                    seed -= aByte;
                }
                if (aByte % 4 == 0) {
                    seed *= aByte;
                }
                if (aByte % 5 == 0) {
                    seed /= aByte;
                }
            }
        } catch (Exception e) {
            seed = 1234;
        }
        return materialColors[getRandomNumberInRange(0, materialColors.length-1, seed)];
    }

    public static int gradeToColor(Grade grade)
    {
        if (grade.type == Grade.TYPE_BEHAVIOUR) {
            return grade.value < 0 ? 0xfff44336 : grade.value > 0 ? 0xff4caf50 : 0xffbdbdbd;
        }
        else if (grade.type == Grade.TYPE_POINT) {
            return Color.parseColor("#"+gradeValueToColorStr(grade.value/grade.valueMax*100));
        }
        else if (grade.type == Grade.TYPE_DESCRIPTIVE || grade.type == Grade.TYPE_TEXT) {
            return grade.color;
        }
        else {
            return Color.parseColor("#"+gradeNameToColorStr(grade.name));
        }
    }

    public static int gradeNameToColor(String grade)
    {
        return Color.parseColor("#"+gradeNameToColorStr(grade));
    }

    private static String gradeNameToColorStr(String grade) {
        switch (grade.toLowerCase()) {
            case "+":
            case "++":
            case "+++":
                return "4caf50";
            case "-":
            case "-,":
            case "-,-,":
            case "np":
            case "np.":
            case "npnp":
            case "np,":
            case "np,np,":
            case "bs":
            case "nk":
                return "ff7043";

            case "1-":
            case "1":
            case "f":
                return "ff0000";
            case "1+":
            case "ef":
                return "ff3d00";

            case "2-":
            case "2":
            case "e":
                return "ff9100";
            case "2+":
            case "de":
                return "ffab00";

            case "3-":
            case "3":
            case "d":
                return "ffff00";
            case "3+":
            case "cd":
                return "c6ff00";

            case "4-":
            case "4":
            case "c":
                return "76ff03";
            case "4+":
            case "bc":
                return "64dd17";

            case "5-":
            case "5":
            case "b":
                return "00c853";
            case "5+":
            case "ab":
                return "00bfa5";

            case "6-":
            case "6":
            case "a":
                return "2196f3";
            case "6+":
            case "a+":
                return "0091ea";
        }
        return "bdbdbd";
    }


    public static int gradeValueToColor(float grade)
    {
        return Color.parseColor("#"+gradeValueToColorStr(grade));
    }
    private static String gradeValueToColorStr(float grade) {
        if (grade < 30) // 1
            return "d50000";
        else if (grade < 50) // 2
            return "ff5722";
        else if (grade < 75) // 3
            return "ff9100";
        else if (grade < 90) // 4
            return "ffd600";
        else if (grade < 98) // 5
            return "00c853";
        else if (grade <= 100) // 6
            return "0091ea";
        else // 6+
            return "2962ff";
    }


    public static Drawable getAdaptiveBackgroundDrawable(int normalColor, int pressedColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(ColorStateList.valueOf(pressedColor),
                    null, getRippleMask(normalColor));
        } else {
            return getStateListDrawable(Color.TRANSPARENT, pressedColor);
        }
    }

    private static Drawable getRippleMask(int color) {
        float[] outerRadii = new float[8];
        // 3 is radius of final ripple,
        // instead of 3 you can give required final radius
        Arrays.fill(outerRadii, 3);

        RoundRectShape r = new RoundRectShape(outerRadii, null, null);
        ShapeDrawable shapeDrawable = new ShapeDrawable(r);
        shapeDrawable.getPaint().setColor(color);
        return shapeDrawable;
    }

    private static StateListDrawable getStateListDrawable(
            int normalColor, int pressedColor) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},
                new ColorDrawable(pressedColor));
        states.addState(new int[]{android.R.attr.state_focused},
                new ColorDrawable(pressedColor));
        states.addState(new int[]{android.R.attr.state_activated},
                new ColorDrawable(pressedColor));
        states.addState(new int[]{},
                new ColorDrawable(normalColor));
        return states;
    }
}
