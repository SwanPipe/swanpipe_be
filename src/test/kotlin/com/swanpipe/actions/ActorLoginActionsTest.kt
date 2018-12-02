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

package com.swanpipe.actions

import com.github.kittinunf.result.success
import com.swanpipe.InitPg
import com.swanpipe.utils.Db
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName("Test of actor login actions")
@ExtendWith(VertxExtension::class)
object ActorLoginActionsTest {

    @DisplayName("Prepare the database")
    @BeforeAll
    @JvmStatic
    fun prepare(testContext: VertxTestContext) {
        InitPg.startPg()
        testContext.completeNow()
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

    @DisplayName("Test create actor login action")
    @Test
    fun testCreateActorLogin(vertx: Vertx, testContext: VertxTestContext) {
        InitPg.pool(vertx)
        val json = JsonObject()
            .put("id", "foo")
            .put("password", "secret")
            .put("pun", "bar")
        ActorLoginActions.createActorLogin(json)
            .subscribe(
                { result ->
                    testContext.verify {
                        result.fold(
                            {
                                val (login,actor,owner) = it
                                assertThat(login.id).isEqualTo("foo")
                                assertThat(actor.pun).isEqualTo("bar")
                                assertThat(owner).isTrue()
                            },
                            {
                                fail( it )
                            }
                        )
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow(it)
                }
            )
    }

    @DisplayName("Test create actor login action with token")
    @Test
    fun testCreateActorLoginWithToken(vertx: Vertx, testContext: VertxTestContext) {
        InitPg.pool(vertx)
        val json = JsonObject()
            .put("id", "foo")
            .put("password", "secret")
            .put("pun", "bar")
            .put("token", "test_token" )
        ActorLoginActions.createActorLoginWithToken(json)
            .subscribe(
                { result ->
                    testContext.verify {
                        result.fold(
                            {
                                val (login,actor,owner) = it
                                assertThat(login.id).isEqualTo("foo")
                                assertThat(actor.pun).isEqualTo("bar")
                                assertThat(owner).isTrue()
                            },
                            {
                                fail( it )
                            }
                        )
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow(it)
                }
            )
    }

    @DisplayName("Test create actor login action with bad token")
    @Test
    fun testCreateActorLoginWithBadToken(vertx: Vertx, testContext: VertxTestContext) {
        InitPg.pool(vertx)
        val json = JsonObject()
            .put("id", "foo1")
            .put("password", "secret")
            .put("pun", "bar1")
            .put("token", "test_token1" )
        ActorLoginActions.createActorLoginWithToken(json)
            .flatMap {
                val json2 = JsonObject()
                    .put("id", "foo2")
                    .put("password", "secret")
                    .put("pun", "bar2")
                    .put("token", "test_token1" ) //same token as above... should fail
                ActorLoginActions.createActorLoginWithToken(json2)
            }
            .subscribe(
                { result ->
                    testContext.verify {
                        result.success {
                            fail( "the same token cannot be used twice")
                        }
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow(it)
                }
            )
    }

}

