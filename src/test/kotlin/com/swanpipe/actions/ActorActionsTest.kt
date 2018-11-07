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

import com.swanpipe.InitPg
import com.swanpipe.utils.Db
import com.swanpipe.utils.ValidationException
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName( "Test of login daos" )
@ExtendWith( VertxExtension::class )
object ActorActionsTest {

    @DisplayName( "Prepare the database" )
    @BeforeAll
    @JvmStatic
    fun prepare( testContext: VertxTestContext) {
        InitPg.startPg()
        testContext.completeNow()
    }

    @DisplayName( "close database" )
    @AfterAll
    @JvmStatic
    fun cleanUp( testContext: VertxTestContext ) {
        Db.pgPool.close()
        testContext.completeNow()
    }

    @DisplayName( "Setup data" )
    @BeforeEach
    fun prepareEach( testContext: VertxTestContext ) {
        InitPg.clean().migrate()
        testContext.completeNow()
    }

    @DisplayName( "Test create actor action" )
    @Test
    fun testCreateActor( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val json = JsonObject()
                .put( "pun", "foo" )
        ActorActions.createActor( json )
                .subscribe(
                        { actor ->
                            testContext.verify {
                                assertThat(actor.pun).isEqualTo("foo")
                                assertThat(actor.publicKeyPem).isNotBlank()
                            }
                            testContext.completeNow()
                        },
                        {
                            testContext.failNow(it)
                        }
                )
    }

    @DisplayName( "Test create actor action bad pun" )
    @Test
    fun testCreateActorBadPun( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val json = JsonObject()
                .put( "pun", "bobby@foo.com" )
        LoginActions.createLogin( json )
                .subscribe(
                        {
                            testContext.failNow( RuntimeException( "test failed" ))
                        },
                        {
                            if( it is ValidationException ) {
                                testContext.verify {
                                    assertThat( it.issues.isEmpty() ).isFalse()
                                }
                                testContext.completeNow()
                            }
                            else {
                                testContext.failNow( RuntimeException( "test failed" ))
                            }
                        }
                )
    }

}

