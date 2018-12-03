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
import com.swanpipe.daos.ActorDao
import com.swanpipe.daos.ConfigDao
import com.swanpipe.daos.LoginDao
import com.swanpipe.utils.Db
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName("Test of login daos")
@ExtendWith(VertxExtension::class)
object StartupAccountsActionsTest {

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

    @DisplayName("Test create login action")
    @Test
    fun testCreateLogin(vertx: Vertx, testContext: VertxTestContext) {

        //by default, the startupAccounts is set to true in flyway

        // our test data
        val sa = JsonObject(
            """
{
  "startupAccounts" : {
    "actorLogins" : [
      {
        "id" : "admin",
        "password" : "secret",
        "pun" : "beautifulSwan",
        "loginData" : {
          "email" : "foo@example.com",
          "roles" : [ "admin" ]
        }
      },
      {
        "id" : "admin2",
        "password" : "anothersecret",
        "pun" : "thedude",
        "loginData" : {
          "email" : "bar@example.com"
        }
      }
    ],
    "actors" : [
      {
        "pun" : "ambassador"
      }
    ],
    "links" : [
      {
        "loginId" : "admin",
        "pun" : "ambassador",
        "owner" : true
      }
    ]
  }
}
            """.trimIndent()
        )

        InitPg.pool(vertx)
        doStartupAccounts( sa )
            .flatMapMaybe {
                testContext.verify {
                    assertThat( it ).isTrue()
                }
                LoginDao.getLogin( "admin" )
            }
            .flatMap {
                ActorDao.getActor( "beautifulSwan" )
            }
            .flatMap {
                ConfigDao.getConfig( STARTUP_ACCOUNTS )
            }
            .subscribe(
                { config ->
                    testContext.verify {
                        assertThat( config.data.getBoolean( "create" ) ).isFalse()
                    }
                    testContext.completeNow()
                },
                {
                    testContext.failNow( it )
                }
            )
    }


}

