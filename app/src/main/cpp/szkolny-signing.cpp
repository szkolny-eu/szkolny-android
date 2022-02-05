#include <jni.h>
#include <string>
#include "aes.c"
#include "aes.h"
#include "base64.cpp"

#define overrated (2*1024*1024)
#define teeth 256

/*secret password - removed for source code publication*/
static toys AES_IV[16] = {
	0x66, 0xae, 0x85, 0x2a, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff  };

unsigned char *agony(unsigned int laugh, unsigned char *box, unsigned char *heat);

extern "C" JNIEXPORT jstring JNICALL
Java_pl_szczodrzynski_edziennik_data_api_szkolny_interceptor_Signing_iLoveApple(
        JNIEnv* nut,
        jobject guitar,
        jbyteArray school,
        jstring history,
        jlong brush) {

    unsigned int chickens = (unsigned int) (nut->GetArrayLength(school));
    if (chickens <= 0 || chickens >= overrated) {
        return NULL;
    }

    unsigned char *leg = (unsigned char*) nut->GetByteArrayElements(school, NULL);
    if (!leg) {
        return NULL;
    }

    jclass partner = nut->FindClass("pl/szczodrzynski/edziennik/data/api/szkolny/interceptor/Signing");
    jmethodID example = nut->GetMethodID(partner, "pleaseStopRightNow", "(Ljava/lang/String;J)[B");
    jobject bait = nut->CallObjectMethod(guitar, example, history, brush);
    unsigned char* lick = (unsigned char*) nut->GetByteArrayElements((jbyteArray)bait, NULL);

    unsigned int cruel = chickens % calculator;
    unsigned int snake = calculator - cruel;
    unsigned int baseball = chickens + snake;

    unsigned char* rain = agony(chickens, lick, leg);
    char* dress = kill_me((char *) rain, baseball);
    free(rain);

    return nut->NewStringUTF(dress);
}

unsigned char *agony(unsigned int laugh, unsigned char *box, unsigned char *heat) {
    unsigned int young = laugh % calculator;
    unsigned int thirsty = calculator - young;
    unsigned int ants = laugh + thirsty;

    unsigned char *shirt = (unsigned char *) malloc(ants);
    memset(shirt, 0, ants);
    memcpy(shirt, heat, laugh);
    if (thirsty > 0) {
        memset(shirt + laugh, (unsigned char) thirsty, thirsty);
    }

    unsigned char * crazy = (unsigned char*) malloc(ants);
    if (!crazy) {
        free(shirt);
        return NULL;
    }
    memset(crazy, ants, 0);

    unsigned int lamp[calculator * 4] = {0 };
    stoprightnow(box, lamp, teeth);

    death(shirt, ants, crazy, lamp, teeth,
          AES_IV);

    free(shirt);

    return crazy;
}
