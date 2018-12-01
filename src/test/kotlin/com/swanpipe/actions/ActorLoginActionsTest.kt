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
    fun testCreateActor(vertx: Vertx, testContext: VertxTestContext) {
        InitPg.pool(vertx)
        val json = JsonObject()
            .put("id", "foo")
            .put("password", "secret")
            .put("pun", "bar")
        ActorLoginActions.createActorLogin(json)
            .subscribe(
                { dbResult ->
                    testContext.verify {
                        assertThat(dbResult.result!!.first.id).isEqualTo("foo")
                        assertThat(dbResult.result!!.second.pun).isEqualTo("bar")
                        assertThat(dbResult.result!!.third).isTrue()
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow(it)
                }
            )
    }

}

