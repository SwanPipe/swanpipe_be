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
import com.swanpipe.dao.LoginDao
import com.swanpipe.utils.Db
import com.swanpipe.utils.ValidationException
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mindrot.jbcrypt.BCrypt

@DisplayName( "Test of login dao" )
@ExtendWith( VertxExtension::class )
object LoginActionsTest {

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

    @DisplayName( "Test create login action" )
    @Test
    fun testCreateLogin( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val json = JsonObject()
                .put( "id", "foo" )
                .put( "password", "secret" )
        LoginActions.createLogin( json )
                .subscribe(
                        { login ->
                            testContext.verify {
                                assertThat(login.id).isEqualTo("foo")
                                assertThat(BCrypt.checkpw( "secret", login.password)).isTrue()
                            }
                            testContext.completeNow()
                        },
                        {
                            testContext.failNow(it)
                        }
                )
    }

    @DisplayName( "Test create login action bad id" )
    @Test
    fun testCreateLoginBadId( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val json = JsonObject()
                .put( "id", "" )
                .put( "password", "secret" )
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

    @DisplayName( "Test create login action bad password" )
    @Test
    fun testCreateLoginBadPassword( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val json = JsonObject()
                .put( "id", "foo" )
                .put( "password", "" )
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

    @DisplayName( "Check Login test" )
    @Test
    fun testCheckLogin( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val json = JsonObject()
                .put( "id", "foo" )
                .put( "password", "secret" )
        LoginActions.createLogin( json )
                .flatMapMaybe {
                    LoginActions.checkLogin( "foo", "secret" )
                }
                .subscribe(
                        { login ->
                            LoginDao.getLogin( login.id )
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
    @Test()
    fun testCheckBadLogin( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val json = JsonObject()
                .put( "id", "foo" )
                .put( "password", "secret" )
        LoginActions.createLogin( json )
                .flatMapMaybe {
                    LoginActions.checkLogin( "foo", "wrongsecret" )
                }
                .subscribe(
                        {
                            testContext.failNow( RuntimeException( "bad login failed"))
                        },
                        {
                            testContext.failNow( it )
                        },
                        {
                            LoginDao.getLogin( "foo" )
                                    .subscribe { login ->
                                        testContext.verify {
                                            assertThat( login.data.getString( "lastFailedLogin" ) ).isNotBlank()
                                        }
                                        testContext.completeNow()
                                    }
                        }
                )
    }

    @DisplayName( "Check Disabled Login test" )
    @Test()
    fun testCheckDisabledLogin( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val json = JsonObject()
                .put( "id", "foo" )
                .put( "password", "secret" )
        LoginActions.createLogin( json )
                .flatMap { _ ->
                    LoginDao.enableLogin( "foo", false )
                }
                .flatMapMaybe {
                    LoginActions.checkLogin( "foo", "secret" )
                }
                .subscribe(
                        {
                            testContext.failNow( RuntimeException( "bad login failed"))
                        },
                        {
                            testContext.failNow( it )
                        },
                        {
                            LoginDao.getLogin( "foo" )
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

