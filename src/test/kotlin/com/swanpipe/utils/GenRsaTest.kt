/*
 * Copyright (c) 2018. Andrew Newton
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

package com.swanpipe.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest

@DisplayName( "Test RSA Generation" )
object GenRsaTest {

    @DisplayName( "Test RSA 2048 Keypair generation" )
    @RepeatedTest( 10 )
    fun testGenRsa2048() {
        val pair = genRsa2048()
        assertThat( pair.first ).startsWith( "-----BEGIN PUBLIC KEY-----\n")
        assertThat( pair.first ).endsWith( "\n-----END PUBLIC KEY-----")
    }

}