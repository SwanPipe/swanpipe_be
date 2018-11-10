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

package com.swanpipe.routers

import com.swanpipe.utils.Db
import com.swanpipe.utils.JSON_TYPE
import com.swanpipe.utils.Version
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.handler.ResponseContentTypeHandler


fun apiRouter(vertx: Vertx): Router {
    val router = Router.router(vertx)
    router.route().handler( ResponseContentTypeHandler.create() )
    router.get("/v1/instance")
            .produces(JSON_TYPE)
            .handler { rc ->
                rc.response()
                        //.putHeader(CONTENT_TYPE_HEADER, JSON_TYPE)
                        .end(
                                json {
                                    obj(
                                            "version" to Version.version,
                                            "buildDate" to Version.buildDate.toString(),
                                            "flywayVersion" to Db.flywayVersion,
                                            "configuredFlywayVersion" to Db.configuredFlywayVerstion,
                                            "installOn" to Db.installedOn.toString()
                                    )
                                }.encodePrettily()
                        )
            }
    return router
}