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

package com.swanpipe

import com.swanpipe.utils.Db
import com.swanpipe.utils.Db.table
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName("Test of A Veritcle Deployment")
@ExtendWith(VertxExtension::class)
object VertxTest {

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

    @DisplayName("Test simple verticle deployment")
    @Test
    fun testVerticle(vertx: Vertx, testContext: VertxTestContext) {

        vertx.deployVerticle(TestVerticle(),
            testContext.succeeding {
                testContext.completeNow()
            }
        )

    }
}

class TestVerticle : AbstractVerticle() {

    override fun start(startFuture: Future<Void>) {
        val sql = "select version, installed_on from ${table("flyway_schema_history")} order by version desc"
        InitPg.pool(vertx).query(sql) { ar ->
            if (ar.succeeded()) {
                assertThat(ar.result().size()).isGreaterThan(0)
                startFuture.complete()
            } else {
                assert(false)
                startFuture.fail(ar.cause())
            }
        }
    }

    override fun stop(stopFuture: Future<Void>) {
        stopFuture.complete()
    }
}
