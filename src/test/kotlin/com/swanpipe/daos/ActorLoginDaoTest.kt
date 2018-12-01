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
import io.vertx.core.json.JsonArray
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName("Test of actor/login daos")
@ExtendWith(VertxExtension::class)
object ActorLoginDaoTest {

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

    @DisplayName("Test link login/actor")
    @Test
    fun testLinkActorLogin(vertx: Vertx, testContext: VertxTestContext) {

        InitPg.pool(vertx)
        LoginDao.createLogin("fizzlebottom", "secret", null)
            .flatMap { _ ->
                val keypair = genRsa2048()
                ActorDao.createActor("fizzy", keypair, null)
            }
            .flatMapSingle { _ ->
                ActorLoginDao.linkActorLogin("fizzlebottom", "fizzy", false)
            }
            .flatMap { link ->
                testContext.verify {
                    assertThat(link.third).isEqualTo(false)
                }
                ActorLoginDao.linkActorLogin("fizzlebottom", "fizzy", true)
            }
            .subscribe(
                { triple ->
                    testContext.verify {
                        assertThat(triple.first).isEqualTo("fizzlebottom")
                        assertThat(triple.second).isEqualTo("fizzy")
                        assertThat(triple.third).isEqualTo(true)
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow(it)
                }
            )
    }

    @DisplayName("Test create login/actor")
    @Test
    fun testCreateActorLogin(vertx: Vertx, testContext: VertxTestContext) {
        InitPg.pool(vertx)
        val keypair = genRsa2048()
        ActorLoginDao.createActorLogin(
            loginId = "furry",
            password = "secret",
            loginData = null,
            pun = "fuzzy",
            owner = true,
            keypair = keypair,
            actorData = null
        )
            .flatMap {
                testContext.verify {
                    assertThat(it.third).isTrue()
                }
                LoginDao.getLogin("furry").toSingle()
            }
            .flatMap { login ->
                testContext.verify {
                    assertThat(login.id).isEqualTo("furry")
                    assertThat(login.enabled).isEqualTo(true)
                }
                ActorDao.getActor("fuzzy").toSingle()
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
                    testContext.failNow(it)
                }
            )
    }

    @DisplayName("Test create login/actor transaction")
    @Test
    fun testCreateActorLoginTx(vertx: Vertx, testContext: VertxTestContext) {
        InitPg.pool(vertx)
        val keypair = genRsa2048()
        ActorLoginDao.createActorLoginTx(
            loginId = "furry2",
            password = "secret",
            loginData = null,
            pun = "fuzzy2",
            owner = true,
            keypair = keypair,
            actorData = null
        )
            .flatMap {
                testContext.verify {
                    assertThat(it.result!!.third).isTrue()
                }
                LoginDao.getLogin("furry2").toSingle()
            }
            .flatMap { login ->
                testContext.verify {
                    assertThat(login.id).isEqualTo("furry2")
                    assertThat(login.enabled).isEqualTo(true)
                }
                ActorDao.getActor("fuzzy2").toSingle()
            }
            .subscribe(
                { actor ->
                    testContext.verify {
                        assertThat(actor.pun).isEqualTo("fuzzy2")
                        assertThat(actor.publicKeyPem).isNotBlank()
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow(it)
                }
            )
    }


    @DisplayName( "Test get Login Actor Link" )
    @Test
    fun testGetLoginActorLink( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool(vertx)
        val keypair = genRsa2048()
        ActorLoginDao.createActorLoginTx(
            loginId = "furry",
            password = "secret",
            loginData = null,
            pun = "fuzzy",
            owner = true,
            keypair = keypair,
            actorData = null
        )
            .flatMap {
                ActorLoginDao.createActorLoginTx(
                    loginId = "burry",
                    password = "secret",
                    loginData = null,
                    pun = "buzzy",
                    owner = true,
                    keypair = keypair,
                    actorData = null
                )
            }
            .flatMap {
                ActorLoginDao.linkActorLogin( "furry", "buzzy", false )
            }
            .flatMapMaybe {
                ActorLoginDao.getLoginActorLink( "furry" )
            }
            .subscribe(
                { loginActorLink ->
                    testContext.verify {
                        assertThat( loginActorLink.id ).isEqualTo( "furry" )
                        assertThat( loginActorLink.actors ).isInstanceOf( JsonArray::class.java )
                        assertThat( loginActorLink.actors.size() ).isEqualTo( 2 )
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow( it )
                }
            )
    }

    @DisplayName( "Test get Login Actor Link Single Actor" )
    @Test
    fun testGetLoginActorLinkSingleActor( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool(vertx)
        val keypair = genRsa2048()
        ActorLoginDao.createActorLoginTx(
            loginId = "furry",
            password = "secret",
            loginData = null,
            pun = "fuzzy",
            owner = true,
            keypair = keypair,
            actorData = null
        )
            .flatMapMaybe {
                ActorLoginDao.getLoginActorLink( "furry" )
            }
            .subscribe(
                { loginActorLink ->
                    testContext.verify {
                        assertThat( loginActorLink.id ).isEqualTo( "furry" )
                        assertThat( loginActorLink.actors ).isInstanceOf( JsonArray::class.java )
                        assertThat( loginActorLink.actors.size() ).isEqualTo( 1 )
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow( it )
                }
            )
    }

}

