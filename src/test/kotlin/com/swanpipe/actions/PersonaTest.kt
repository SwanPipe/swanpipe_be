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
import java.lang.RuntimeException

@DisplayName( "Test of persona actions" )
@ExtendWith( VertxExtension::class )
object PersonaTest {

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

    @DisplayName( "Test create persona" )
    @Test
    fun testCreatePersona(vertx : Vertx, testContext: VertxTestContext) {

        InitPg.pool( vertx )
        createPersona( "fugly", "the fugly monster" )
                .flatMap {
                    assertThat( it ).isEqualTo( "fugly" )
                    getPersona( it ).toSingle()
                }
                .subscribe(
                        { row ->
                            testContext.verify {
                                assertThat(row?.getString("id")).isEqualTo("fugly")
                                assertThat(row?.getString("display_name")).isEqualTo("the fugly monster")
                                assertThat(row?.getValue("created")).isNotNull()
                            }
                            testContext.completeNow()
                        },
                        {
                            testContext.failNow(it)
                        }
                )
    }

    @DisplayName( "Test non existent persona" )
    @Test
    fun testNonExistentPersona( vertx: Vertx, testContext: VertxTestContext ) {
        InitPg.pool( vertx )
        getPersona( "nobody" )
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

}

