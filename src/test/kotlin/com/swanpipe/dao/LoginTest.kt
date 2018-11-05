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

package com.swanpipe.dao

import com.swanpipe.InitPg
import com.swanpipe.utils.Db
import io.reactiverse.pgclient.data.Json
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mindrot.jbcrypt.BCrypt

@DisplayName( "Test of login dao" )
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
        LoginDao.createLogin( "fizzlebottom", "secret" )
                .flatMap { login ->
                    assertThat( login.id ).isEqualTo( "fizzlebottom" )
                    LoginDao.getLogin( login.id ).toSingle()
                }
                .subscribe(
                        { login ->
                            testContext.verify {
                                assertThat(login.id).isEqualTo("fizzlebottom")
                                assertThat(login.enabled).isEqualTo( true )
                                assertThat(login.password).isEqualTo("secret")
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
        LoginDao.getLogin( "nobody" )
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

    @DisplayName( "Test setting login data" )
    @Test
    fun testSetLoginData( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        LoginDao.createLogin( "foo", "secret" )
                .flatMap { login ->
                   LoginDao.setLoginData( login.id, arrayOf( "loginType" ), "normal" )
                }
                .subscribe { data ->
                    testContext.verify {
                        assertThat( data.getString( "loginType" ) ).isEqualTo( "normal" )
                    }
                    testContext.completeNow()
                }
    }

    @DisplayName( "Test setting login data as object" )
    @Test
    fun testSetLoginDataObject( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        LoginDao.createLogin( "foo", "secret" )
                .flatMap { login ->
                    LoginDao.setLoginData( login.id, arrayOf( "loginCount" ), Json.create( JsonObject().put( "bar", "4000" ) ) )
                }
                .subscribe { data ->
                    testContext.verify {
                        assertThat( data.getJsonObject( "loginCount" ).getString("bar") ).isEqualTo( "4000" )
                    }
                    testContext.completeNow()
                }
    }

    @DisplayName( "Test setting login data as int" )
    @Test
    fun testSetLoginDataInt( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        LoginDao.createLogin( "foo", "secret" )
                .flatMap { login ->
                    LoginDao.setLoginData( login.id, arrayOf( "loginCount" ), Integer( 4000 ) )
                }
                .subscribe { data ->
                    testContext.verify {
                        assertThat( data.getInteger( "loginCount" ) ).isEqualTo( 4000 )
                    }
                    testContext.completeNow()
                }
    }

    @DisplayName( "Test setting login data as boolean" )
    @Test
    fun testSetLoginDataBoolean( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        LoginDao.createLogin( "foo", "secret" )
                .flatMap { login ->
                    LoginDao.setLoginData( login.id, arrayOf( "acceptsFollowers" ), false )
                }
                .subscribe { data ->
                    testContext.verify {
                        assertThat( data.getBoolean( "acceptsFollowers" ) ).isFalse()
                    }
                    testContext.completeNow()
                }
    }

}

