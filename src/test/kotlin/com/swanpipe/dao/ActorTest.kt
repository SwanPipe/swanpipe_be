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
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName( "Test of actor dao" )
@ExtendWith( VertxExtension::class )
object ActorTest {

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

    @DisplayName( "Test create actor" )
    @Test
    fun testCreateActor(vertx : Vertx, testContext: VertxTestContext) {

        InitPg.pool( vertx )
        ActorDao.createActor( "fugly" )
                .flatMap { actor ->
                    assertThat( actor.pun ).isEqualTo( "fugly" )
                    ActorDao.getActor( actor.pun ).toSingle()
                }
                .subscribe(
                        { actor ->
                            testContext.verify {
                                assertThat(actor.pun).isEqualTo("fugly")
                                assertThat(actor.publicKeyPem).isNotBlank()
                            }
                            testContext.completeNow()
                        },
                        {
                            testContext.failNow(it)
                        }
                )
    }

    @DisplayName( "Test non existent actor" )
    @Test
    fun testNonExistentActor( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        ActorDao.getActor( "nobody" )
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

    @DisplayName( "Test set actor data" )
    @Test
    fun testSetActorData( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        ActorDao.createActor( "foo" )
                .flatMap { actor ->
                   ActorDao.setActorData( actor.pun, arrayOf( "name" ), "a fun user" )
                }
                .subscribe { data ->
                    testContext.verify {
                        assertThat( data.getString( "name") ).isEqualTo( "a fun user" )
                    }
                    testContext.completeNow()
                }
    }

}

