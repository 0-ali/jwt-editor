/*
Author : Dolph Flynn

Copyright 2022 Dolph Flynn

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

package com.blackberry.jwteditor;

import com.blackberry.jwteditor.model.jose.JWE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.ParseException;
import java.util.stream.Stream;

import static com.blackberry.jwteditor.model.jose.JWEFactory.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JWEParserTests {
    private static Stream<String> validJwe() {
        return Stream.of(
                //JWE with encrypted key
                "eyJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiQTEyOEtXIn0.H3X6mT5HLgcFfzLoe4ku6Knhh9Ofv1eL.qF5-N_7K8VQ4yMSz.WXUNY6eg5fR4tc8Hqf5XDRM9ALGwcQyYG4IYwwg8Ctkx1UuxoV7t6UnemjzCj2sOYUqi3KYpDzrKVJpzokz0vcIem4lFe5N_ds8FAMpW0GSF9ePA8qvV99WaP0N2ECVPmgihvL6qwNhdptlLKtxcOpE41U5LnU22voPK55VF4_1j0WmTgWgZ7DwLDysp6EIDjrrt-DY.febBmP71KADmKRVfeSnv_g",
                //JWE with dir encryption
                "eyJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiZGlyIn0..FofRkmAUlKShyhYp.1AjXmQsKwV36LSxZ5YJq7xPPTTUS_e9FyLbd-CWdX72ESWMttHm2xGDWUl-Sp9grmcINWLNwsKezYnJVncfir2o9Uq9vcXENIypU2Qwmymn5q5gJwkR4Wx_RLae9Zm8xP76LJFQe8FssUVHx65Zzvd1I6GbV6FjfbkLF1Z_Ka-olubtWCilFDjIVN7WRUAxmV8syJaM.P0XjuL8_8nK50paY09mB6g",
                //JWE with encrypted key with preceding text
                "asdasdasdaseyJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiQTEyOEtXIn0.H3X6mT5HLgcFfzLoe4ku6Knhh9Ofv1eL.qF5-N_7K8VQ4yMSz.WXUNY6eg5fR4tc8Hqf5XDRM9ALGwcQyYG4IYwwg8Ctkx1UuxoV7t6UnemjzCj2sOYUqi3KYpDzrKVJpzokz0vcIem4lFe5N_ds8FAMpW0GSF9ePA8qvV99WaP0N2ECVPmgihvL6qwNhdptlLKtxcOpE41U5LnU22voPK55VF4_1j0WmTgWgZ7DwLDysp6EIDjrrt-DY.febBmP71KADmKRVfeSnv_g",
                //JWE with dir encryption with preceding text
                "asdasdasdeyJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiZGlyIn0..FofRkmAUlKShyhYp.1AjXmQsKwV36LSxZ5YJq7xPPTTUS_e9FyLbd-CWdX72ESWMttHm2xGDWUl-Sp9grmcINWLNwsKezYnJVncfir2o9Uq9vcXENIypU2Qwmymn5q5gJwkR4Wx_RLae9Zm8xP76LJFQe8FssUVHx65Zzvd1I6GbV6FjfbkLF1Z_Ka-olubtWCilFDjIVN7WRUAxmV8syJaM.P0XjuL8_8nK50paY09mB6g",
                //No header
                ".H3X6mT5HLgcFfzLoe4ku6Knhh9Ofv1eL.qF5-N_7K8VQ4yMSz.WXUNY6eg5fR4tc8Hqf5XDRM9ALGwcQyYG4IYwwg8Ctkx1UuxoV7t6UnemjzCj2sOYUqi3KYpDzrKVJpzokz0vcIem4lFe5N_ds8FAMpW0GSF9ePA8qvV99WaP0N2ECVPmgihvL6qwNhdptlLKtxcOpE41U5LnU22voPK55VF4_1j0WmTgWgZ7DwLDysp6EIDjrrt-DY.febBmP71KADmKRVfeSnv_g",
                //No IV
                "eyJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiQTEyOEtXIn0.H3X6mT5HLgcFfzLoe4ku6Knhh9Ofv1eL..WXUNY6eg5fR4tc8Hqf5XDRM9ALGwcQyYG4IYwwg8Ctkx1UuxoV7t6UnemjzCj2sOYUqi3KYpDzrKVJpzokz0vcIem4lFe5N_ds8FAMpW0GSF9ePA8qvV99WaP0N2ECVPmgihvL6qwNhdptlLKtxcOpE41U5LnU22voPK55VF4_1j0WmTgWgZ7DwLDysp6EIDjrrt-DY.febBmP71KADmKRVfeSnv_g",
                //No Ciphertext
                "eyJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiQTEyOEtXIn0.H3X6mT5HLgcFfzLoe4ku6Knhh9Ofv1eL.qF5-N_7K8VQ4yMSz..febBmP71KADmKRVfeSnv_g",
                //No Tag
                "eyJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiQTEyOEtXIn0.H3X6mT5HLgcFfzLoe4ku6Knhh9Ofv1eL.qF5-N_7K8VQ4yMSz.WXUNY6eg5fR4tc8Hqf5XDRM9ALGwcQyYG4IYwwg8Ctkx1UuxoV7t6UnemjzCj2sOYUqi3KYpDzrKVJpzokz0vcIem4lFe5N_ds8FAMpW0GSF9ePA8qvV99WaP0N2ECVPmgihvL6qwNhdptlLKtxcOpE41U5LnU22voPK55VF4_1j0WmTgWgZ7DwLDysp6EIDjrrt-DY.",
                //URL
                "1.2.www.blackberry.com"
        );
    }

    private static Stream<String> invalidJwe() {
        return Stream.of(
                ".",
                "..",
                "...",
                "....",
                "....."
        );
    }

    @ParameterizedTest
    @MethodSource("validJwe")
    void testValidJWE(String joseObjectString) throws ParseException {
        JWE jwe = parse(joseObjectString);

        assertThat(jwe.serialize()).isEqualTo(joseObjectString);
    }

    @ParameterizedTest
    @MethodSource("invalidJwe")
    void testInvalidJWE(String joseObjectString) {
        assertThrows(ParseException.class, () -> parse(joseObjectString));
    }
}
