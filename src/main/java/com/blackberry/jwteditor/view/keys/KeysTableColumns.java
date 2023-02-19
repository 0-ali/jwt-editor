/*
Author : Dolph Flynn

Copyright 2023 Dolph Flynn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.blackberry.jwteditor.view.keys;

import com.blackberry.jwteditor.utils.Utils;

import static java.util.Arrays.stream;

enum KeysTableColumns {
    ID("id", 30, String.class),
    TYPE("type", 10, String.class),
    PUBLIC_KEY("public_key", 10, Boolean.class),
    PRIVATE_KEY("private_key", 10, Boolean.class),
    SIGNING("signing", 10, Boolean.class),
    VERIFICATION("verification", 10, Boolean.class),
    ENCRYPTION("encryption", 10, Boolean.class),
    DECRYPTION("decryption", 10, Boolean.class);

    private final String label;
    private final int widthPercentage;
    private final Class<?> type;

    KeysTableColumns(String labelResourceId, int widthPercentage, Class<?> type) {
        this.label = Utils.getResourceString(labelResourceId);
        this.widthPercentage = widthPercentage;
        this.type = type;
    }

    static int[] columnWidthPercentages() {
        return stream(values()).mapToInt(c -> c.widthPercentage).toArray();
    }

    static KeysTableColumns fromIndex(int index) {
        return values()[index];
    }

    static String labelWithIndex(int index) {
        return values()[index].label;
    }

    static Class<?> typeForIndex(int index) {
        return values()[index].type;
    }
}
