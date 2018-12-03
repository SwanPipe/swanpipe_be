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

import com.swanpipe.daos.LoginDao
import com.swanpipe.routers.handlerMaybe
import com.swanpipe.routers.loginId
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.ext.web.RoutingContext

fun loginAccountHandler(): (RoutingContext) -> Unit {
    return handlerMaybe(
        "loginAccount",
        { rc ->
            LoginDao.getLogin(loginId(rc))
        },
        { rc, login ->
            rc.response()
                .end(
                    json {
                        obj(
                            "loginId" to login.id,
                            "created" to login.created.toString()
                        )
                    }.encodePrettily()
                )
        },
        true
    )
}