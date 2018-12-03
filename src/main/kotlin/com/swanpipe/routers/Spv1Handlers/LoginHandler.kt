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

package com.swanpipe.routers.Spv1Handlers

import com.swanpipe.actions.LoginActions
import com.swanpipe.utils.appLogger
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.ext.auth.jwt.JWTAuth
import io.vertx.reactivex.ext.web.RoutingContext
import java.time.OffsetDateTime

fun loginHandler(jwt: JWTAuth): (RoutingContext) -> Unit {
    return { rc ->
        val data = rc.bodyAsJson
        val loginId = data.getString("loginId")
        val password = data.getString("password")

        LoginActions.checkLogin(loginId, password)
            .subscribe(
                { _ ->
                    appLogger.info { "action=login loginId=${loginId} result=success" }
                    rc.response()
                        .end(
                            json {
                                obj(
                                    "token" to jwt.generateToken(
                                        JsonObject()
                                            .put("sub", loginId)
                                            .put("iss", "swanpipe")
                                            .put("exp", OffsetDateTime.now().plusMonths(1).toEpochSecond())
                                    )
                                )
                            }.encodePrettily()
                        )
                },
                { e ->
                    appLogger.error { "action=login loginId=${loginId} result=error : ${e}" }
                    e.printStackTrace()
                    rc.response().setStatusCode(500).end()
                },
                {
                    appLogger.info { "action=login loginId=${loginId} result=failed" }
                    rc.response().setStatusCode(401).end()
                }
            )
    }
}