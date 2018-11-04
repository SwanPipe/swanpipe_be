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

package com.swanpipe.actions

import com.swanpipe.InitPg
import com.swanpipe.utils.Db
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mindrot.jbcrypt.BCrypt

@DisplayName( "Test of login actions" )
@ExtendWith( VertxExtension::class )
object LoginTest {

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

    @DisplayName( "Test create login" )
    @Test
    fun testCreateActor(vertx : Vertx, testContext: VertxTestContext) {

        InitPg.pool( vertx )
        createLogin( "fizzlebottom", "secret" )
                .flatMap { login ->
                    assertThat( login.id ).isEqualTo( "fizzlebottom" )
                    getLogin( login.id ).toSingle()
                }
                .subscribe(
                        { login ->
                            testContext.verify {
                                assertThat(login.id).isEqualTo("fizzlebottom")
                                assertThat(login.enabled).isEqualTo( true )
                                assertThat(BCrypt.checkpw( "secret", login.password)).isTrue()
                            }
                            testContext.completeNow()
                        },
                        {
                            testContext.failNow(it)
                        }
                )
    }

    @DisplayName( "Test non existent login" )
    @Test
    fun testNonExistentActor( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        getLogin( "nobody" )
                .subscribe(
                        { _ ->
                            testContext.verify {
                                fail( "got back a result")
                            }
                            testContext.completeNow()
                        },
                        {
                            testContext.failNow( it )
                        },
                        {
                            testContext.completeNow()
                        }
                )
    }

    @DisplayName( "Check Login test" )
    @Test
    fun testCheckLogin( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        createLogin( "foo", "secret" )
                .flatMapMaybe {
                    checkLogin( "foo", "secret" )
                }
                .subscribe(
                        { login ->
                            getLogin( login.id )
                                    .subscribe { fresLogin ->
                                        testContext.verify {
                                            assertThat(login.id).isEqualTo("foo")
                                            assertThat( fresLogin.data.getString( "lastSuccessfulLogin" ) ).isNotBlank()
                                        }
                                        testContext.completeNow()
                                    }
                        },
                        {
                            testContext.failNow( it )
                        },
                        {
                            testContext.failNow( RuntimeException( "good login failed"))
                        }
                )
    }

    @DisplayName( "Check Bad Login test" )
    @Test
    fun testCheckBadLogin( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        createLogin( "foo", "secret" )
                .flatMapMaybe {
                    checkLogin( "foo", "wrongsecret" )
                }
                .subscribe(
                        {
                            testContext.failNow( RuntimeException( "bad login failed"))
                        },
                        {
                            testContext.failNow( it )
                        },
                        {
                            getLogin( "foo" )
                                    .subscribe { login ->
                                        testContext.verify {
                                            assertThat( login.data.getString( "lastFailedLogin" ) ).isNotBlank()
                                        }
                                        testContext.completeNow()
                                    }
                        }
                )
    }

}

