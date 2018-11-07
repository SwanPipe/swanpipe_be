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

package com.swanpipe.daos

import com.swanpipe.InitPg
import com.swanpipe.utils.Db
import com.swanpipe.utils.genRsa2048
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName( "Test of actor/login daos" )
@ExtendWith( VertxExtension::class )
object ActorLoginDaoTest {

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

    @DisplayName( "Test link login/actor" )
    @Test
    fun testLinkActorLogin(vertx : Vertx, testContext: VertxTestContext) {

        InitPg.pool( vertx )
        LoginDao.createLogin( "fizzlebottom", "secret" )
                .flatMap { _ ->
                    val keypair = genRsa2048()
                    ActorDao.createActor( "fizzy", keypair )
                }
                .flatMap { _ ->
                    ActorLoginDao.linkActorLogin( "fizzlebottom", "fizzy", false )
                }
                .flatMap { link ->
                    testContext.verify {
                        assertThat(link.third).isEqualTo( false )
                    }
                    ActorLoginDao.linkActorLogin( "fizzlebottom", "fizzy", true )
                }
                .subscribe(
                        { triple ->
                            testContext.verify {
                                assertThat(triple.first).isEqualTo("fizzlebottom")
                                assertThat(triple.second).isEqualTo("fizzy")
                                assertThat(triple.third).isEqualTo( true )
                            }
                            testContext.completeNow()
                        },
                        {
                            testContext.failNow(it)
                        }
                )
    }

    @DisplayName( "Test create login/actor" )
    @Test
    fun createActorLogin( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        val keypair = genRsa2048()
        ActorLoginDao.createActorLogin(
                loginId = "furry",
                password = "secret",
                pun = "fuzzy",
                owner = true,
                keypair = keypair
        )
                .flatMap {
                    testContext.verify {
                        assertThat( it.third ).isTrue()
                    }
                    LoginDao.getLogin( "furry" ).toSingle()
                }
                .flatMap { login ->
                    testContext.verify {
                        assertThat(login.id).isEqualTo("furry")
                        assertThat(login.enabled).isEqualTo( true )
                    }
                    ActorDao.getActor( "fuzzy" ).toSingle()
                }
                .subscribe(
                        { actor ->
                            testContext.verify {
                                assertThat(actor.pun).isEqualTo("fuzzy")
                                assertThat(actor.publicKeyPem).isNotBlank()
                            }
                            testContext.completeNow()
                        },
                        {
                            testContext.failNow( it )
                        }
                )
    }

}

