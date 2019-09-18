package com.danimahardhika.cafebar;

/*
 * CafeBar
 *
 * Copyright (c) 2017 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.Color;

import androidx.annotation.ColorInt;

@SuppressWarnings("unused")
public enum CafeBarTheme {

    LIGHT(Color.parseColor("#F5F5F5")),
    DARK(Color.parseColor("#323232")),
    CLEAR_BLACK(Color.BLACK);

    private int mColor;

    CafeBarTheme(@ColorInt int color) {
        mColor = color;
    }

    @ColorInt
    int getColor() {
        return mColor;
    }

    public static Custom Custom(@ColorInt int color) {
        return new Custom(color);
    }

    public static class Custom {

        private int mColor;

        private Custom(int color) {
            mColor = color;
        }

        @ColorInt
        int getColor() {
            return mColor;
        }

        @ColorInt
        int getTitleColor() {
            return CafeBarUtil.getTitleTextColor(mColor);
        }

        @ColorInt
        int getSubTitleColor() {
            return CafeBarUtil.getSubTitleTextColor(mColor);
        }
    }
}
