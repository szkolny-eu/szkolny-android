package im.wangchao.mhttp.body;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import okhttp3.MediaType;

/**
 * <p>Description  : MediaType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2017/6/23.</p>
 * <p>Time         : 下午2:15.</p>
 */
public final class MediaTypeUtils {
    /** MediaType String */
    public static final String APPLICATION_OCTET_STREAM     = "application/octet-stream";
    public static final String APPLICATION_JSON             = "application/json; charset=utf-8";
    public static final String APPLICATION_FORM             = "application/x-www-form-urlencoded";
    public static final String APPLICATION_XML             = "application/xml";

    /** MediaType */
    public static final MediaType JSON = MediaType.parse(APPLICATION_JSON);
    public static final MediaType OCTET = MediaType.parse(APPLICATION_OCTET_STREAM);
    public static final MediaType FORM = MediaType.parse(APPLICATION_FORM);
    public static final MediaType XML = MediaType.parse(APPLICATION_XML);
    public static final MediaType DEFAULT = FORM;

    /**
     * 判断两个 MediaType 是否相等，只判断 type 和 subType。
     */
    public static boolean equals(@NonNull MediaType first, @NonNull MediaType second){
        String first_type_subType = first.type().concat(first.subtype());
        String second_type_subType = second.type().concat(second.subtype());

        return TextUtils.equals(first_type_subType, second_type_subType);
    }

    public static boolean isJSON(@NonNull MediaType mediaType){
        return equals(mediaType, JSON);
    }

    public static boolean isOCTET(@NonNull MediaType mediaType){
        return equals(mediaType, OCTET);
    }

    public static boolean isFORM(@NonNull MediaType mediaType){
        return equals(mediaType, FORM);
    }

    public static boolean isXML(@NonNull MediaType mediaType){
        return equals(mediaType, XML);
    }
}
