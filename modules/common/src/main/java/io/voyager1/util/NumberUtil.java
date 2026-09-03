/*
 * Copyright (c) 2026 Voyager1
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.voyager1.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 数字工具{@code io.voyager1.util.NumberUtil}。
 */
public class NumberUtil {

    public static double div(Number v1, Number v2, int scale) {
        if (v1 == null || v2 == null || v2.doubleValue() == 0) {
            return 0;
        }
        return BigDecimal.valueOf(v1.doubleValue())
            .divide(BigDecimal.valueOf(v2.doubleValue()), scale, RoundingMode.HALF_UP)
            .doubleValue();
    }

    public static String formatPercent(double value, int scale) {
        return BigDecimal.valueOf(value * 100).setScale(scale, RoundingMode.HALF_UP) + "%";
    }

    public static boolean isNumber(CharSequence str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        try {
            Double.parseDouble(str.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
