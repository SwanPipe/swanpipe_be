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
import com.swanpipe.actions.ActorLoginActions
import com.swanpipe.utils.AUTHORIZATION_HEADER
import com.swanpipe.utils.Db
import com.swanpipe.utils.HttpInfo
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.ext.web.client.WebClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName( "Test of open API 3 router" )
@ExtendWith( VertxExtension::class )
object AccountInfoHandlerTest {

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

    @DisplayName( "Test accountInfo" )
    @Test
    fun testAccountInfo( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val json = JsonObject()
            .put("id", "foo2")
            .put("password", "secret")
            .put("pun", "bar2")
        ActorLoginActions.createActorLogin(json)
            .flatMap {
                val web = WebClient.create( io.vertx.reactivex.core.Vertx( vertx ) )
                web.post( HttpInfo.actualPort, HttpInfo.host, "/spv1/login" )
                    .rxSendJsonObject(
                        json { obj(
                            "loginId" to "foo2",
                            "password" to "secret"
                        ) }
                    )
            }
            .flatMap { response ->
                val token = response.bodyAsJsonObject().getString( "token" )
                val web = WebClient.create( io.vertx.reactivex.core.Vertx( vertx ) )
                web.get( HttpInfo.actualPort, HttpInfo.host, "/spv1/account-info" )
                    .putHeader( AUTHORIZATION_HEADER, "Bearer ${token}" )
                    .rxSend()
            }
            .subscribe(
                { response ->
                    testContext.verify {
                        assertThat( response.statusCode() ).isEqualTo( 200 )
                        val body = response.bodyAsJsonObject()
                        assertThat( body.getString( "loginId" ) ).isEqualTo( "foo2" )
                        assertThat( body.getJsonArray( "actors" ).size() ).isEqualTo( 1 )
                        assertThat( body.getJsonArray( "actors" ).get<JsonObject>( 0 ).getString( "pun") ).isEqualTo( "bar2" )
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow( it )
                }
            )
    }

    @DisplayName( "Test AccountInfo with no user" )
    @Test
    fun testNoUserAccountInfo( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val json = JsonObject()
            .put("id", "foo3")
            .put("password", "secret")
            .put("pun", "bar3")
        ActorLoginActions.createActorLogin(json)
            .flatMap {
                val web = WebClient.create( io.vertx.reactivex.core.Vertx( vertx ) )
                web.get( HttpInfo.actualPort, HttpInfo.host, "/spv1/account-info" )
                    .rxSend()
            }
            .subscribe(
                { response ->
                    testContext.verify {
                        assertThat( response.statusCode() ).isEqualTo( 401 )
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow( it )
                }
            )

    }

}