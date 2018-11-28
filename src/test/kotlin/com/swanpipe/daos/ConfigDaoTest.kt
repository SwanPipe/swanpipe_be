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

package com.swanpipe.daos

import com.swanpipe.InitPg
import com.swanpipe.utils.Db
import io.reactiverse.pgclient.data.Json
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName( "Test of config dao")
@ExtendWith( VertxExtension::class )
object ConfigDaoTest {

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

    @DisplayName( "test of get and set" )
    @Test
    fun testSetAndGet( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool(vertx)
        LoginDao.createLogin("fuzzy", "secret", null )
            .flatMap { login ->
                LoginDao.setLoginData( login.id, arrayOf("roles"), Json.create( JsonArray().add( "admin" ) ) )
            }
            .flatMap { _ ->
                val json = json { obj( "foo" to "bar" ) }
                ConfigDao.setConfig( "aconfig", json, "fuzzy", "admin" )
            }
            .flatMapMaybe { _ ->
                ConfigDao.getConfig( "aconfig" )
            }
            .subscribe(
                { config ->
                    testContext.verify {
                        assertThat( config.data.getString( "foo" ) ).isEqualTo( "bar" )
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow( it )
                },
                {
                    testContext.failNow( RuntimeException( "set and get config did not succeed" ) )
                }
            )
    }

    @DisplayName( "test of get and set no admin" )
    @Test
    fun testSetAndGetNoAdmin( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool(vertx)
        LoginDao.createLogin("furry", "secret", null)
            .flatMap { _ ->
                val json = json { obj( "foo" to "bar" ) }
                ConfigDao.setConfig( "aconfig2", json, "furry", "admin" )
            }
            .subscribe(
                { _ ->
                    testContext.failNow( RuntimeException( "input of config should fail but it succeeded"))
                },
                {
                    testContext.completeNow()
                }
            )
    }

    @DisplayName( "test of update" )
    @Test
    fun testUpdateAndGet( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool(vertx)
        LoginDao.createLogin("fickle", "secret", null)
            .flatMap { login ->
                LoginDao.setLoginData( login.id, arrayOf("roles"), Json.create( JsonArray().add( "admin" ) ) )
            }
            .flatMap { _ ->
                val json = json { obj( "foo" to "bar" ) }
                ConfigDao.setConfig( "aconfig3", json, "fickle", "admin" )
            }
            .flatMap { _ ->
                val json = json { obj( "foo" to "baz" ) }
                ConfigDao.setConfig( "aconfig3", json, "fickle", "admin" )
            }
            .flatMapMaybe { _ ->
                ConfigDao.getConfig( "aconfig3" )
            }
            .subscribe(
                { config ->
                    testContext.verify {
                        assertThat( config.data.getString( "foo" ) ).isEqualTo( "baz" )
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow( it )
                },
                {
                    testContext.failNow( RuntimeException( "update and get config did not succeed" ) )
                }
            )
    }
}
