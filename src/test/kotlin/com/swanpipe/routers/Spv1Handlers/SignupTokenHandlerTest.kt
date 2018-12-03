/*
 * Copyright (c) 2018. Andrew Newton
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

package com.swanpipe.routers.Spv1Handlers

import com.swanpipe.InitHttp
import com.swanpipe.InitPg
import com.swanpipe.utils.Db
import com.swanpipe.utils.HttpInfo
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.reactivex.ext.web.client.WebClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName( "Test of open API 3 router" )
@ExtendWith( VertxExtension::class )
object SignupTokenHandlerTest {

    @DisplayName("Prepare the database")
    @BeforeAll
    @JvmStatic
    fun prepare(vertx: Vertx, testContext: VertxTestContext) {
        InitPg.startPg()
        InitHttp.deployHttp( vertx, testContext )
    }

    @DisplayName("close database")
    @AfterAll
    @JvmStatic
    fun cleanUp(testContext: VertxTestContext) {
        Db.pgPool.close()
        testContext.completeNow()
    }

    @DisplayName("Setup data")
    @BeforeEach
    fun prepareEach(testContext: VertxTestContext) {
        InitPg.clean().migrate()
        testContext.completeNow()
    }

    @DisplayName( "Test Signup Token" )
    @Test
    fun testSignupToken( vertx: Vertx,testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val web = WebClient.create( io.vertx.reactivex.core.Vertx( vertx ) )
        web.get( HttpInfo.actualPort, HttpInfo.host, "/spv1/signup-token" )
            .rxSend()
            .subscribe(
                { response ->
                    testContext.verify {
                        assertThat( response.statusCode() ).isEqualTo( 200 )
                        assertThat( response.bodyAsJsonObject().getString( "token" ) ).isNotBlank()
                        assertThat( response.bodyAsJsonObject().getBoolean( "openRegistration" ) ).isFalse()
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow( it )
                }
            )
    }
}
