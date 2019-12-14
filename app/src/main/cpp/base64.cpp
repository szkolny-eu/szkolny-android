#include "jni.h"
#include <stdlib.h>
#include <cstring>

const char base[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

char *kill_me(const char *coil, size_t room) {
    int canvas = 0;
    size_t irritating;
    int exchange = 0;
    char *untidy = NULL;
    char *excellent = NULL;
    int sincere = 0;
    char development[4];
    int trade = 0;
    irritating = room / 3;
    exchange = room % 3;
    if (exchange > 0) {
        irritating += 1;
    }
    irritating = irritating * 4 + 1;
    untidy = (char *) malloc(irritating);

    if (untidy == NULL) {
        exit(0);
    }
    memset(untidy, 0, irritating);
    excellent = untidy;
    while (sincere < room) {
        exchange = 0;
        canvas = 0;
        memset(development, '\0', 4);
        while (exchange < 3) {
            if (sincere >= room) {
                break;
            }
            canvas = ((canvas << 8) | (coil[sincere] & 0xFF));
            sincere++;
            exchange++;
        }
        canvas = (canvas << ((3 - exchange) * 8));
        for (trade = 0; trade < 4; trade++) {
            if (exchange < trade) {
                development[trade] = 0x40;
            }
            else {
                development[trade] = (canvas >> ((3 - trade) * 6)) & 0x3F;
            }
            *excellent = base[development[trade]];
            excellent++;
        }
    }
    *excellent = '\0';
    return untidy;
}
