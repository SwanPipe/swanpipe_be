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

package com.swanpipe.routers

import com.swanpipe.InitHttp
import com.swanpipe.InitPg
import com.swanpipe.utils.Db
import com.swanpipe.utils.HttpInfo
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.ext.auth.jwt.JWTAuth
import io.vertx.reactivex.ext.web.client.WebClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.time.OffsetDateTime

@DisplayName( "Test of open API 3 router" )
@ExtendWith( VertxExtension::class )
object OpenApi3RouterTest {

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

    @DisplayName( "Test NBF token" )
    @Test
    fun testNbfToken( vertx: Vertx, testContext: VertxTestContext ) {
        val jwtAuthConfig = json {
            obj(
                "keyStore" to
                        obj(
                            "type" to "jceks",
                            "path" to "jwt.jceks",
                            "password" to "secret"
                        )
            )
        }
        val jwt = JWTAuth.create(io.vertx.reactivex.core.Vertx(vertx), JWTAuthOptions(jwtAuthConfig))
        val token= jwt.generateToken(
            JsonObject()
                .put("iss", "swanpipe")
                .put("source", "127.0.0.1")
                .put("nbf", OffsetDateTime.now().plusSeconds(0).toEpochSecond())
                .put("exp", OffsetDateTime.now().plusMonths(1).toEpochSecond())
        )
        verifyNbfToken(jwt,token)
            .subscribe(
                {
                    testContext.completeNow()
                },
                {
                    testContext.failNow( it )
                }
            )
    }

    @DisplayName( "Test bad NBF token" )
    @Test
    fun testBadNbfToken( vertx: Vertx, testContext: VertxTestContext ) {
        val jwtAuthConfig = json {
            obj(
                "keyStore" to
                        obj(
                            "type" to "jceks",
                            "path" to "jwt.jceks",
                            "password" to "secret"
                        )
            )
        }
        val jwt = JWTAuth.create(io.vertx.reactivex.core.Vertx(vertx), JWTAuthOptions(jwtAuthConfig))
        val token= jwt.generateToken(
            JsonObject()
                .put("iss", "swanpipe")
                .put("source", "127.0.0.1")
                .put("nbf", OffsetDateTime.now().plusSeconds(10).toEpochSecond())
                .put("exp", OffsetDateTime.now().plusMonths(1).toEpochSecond())
        )
        verifyNbfToken(jwt,token)
            .subscribe(
                {
                    testContext.failNow( RuntimeException( "this should have failed" ) )
                },
                {
                    testContext.completeNow()
                }
            )
    }
}