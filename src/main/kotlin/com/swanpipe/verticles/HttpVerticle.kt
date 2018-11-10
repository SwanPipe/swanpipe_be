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
package com.swanpipe.verticles

import com.swanpipe.actions.PUN_CHARS
import com.swanpipe.routers.apiRouter
import com.swanpipe.routers.actorRouter
import com.swanpipe.utils.DEFAULT_HOST
import com.swanpipe.utils.DEFAULT_PORT
import com.swanpipe.utils.HTTP_CONFIG_NAME
import com.swanpipe.utils.HttpInfo
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.http.HttpServerOptions
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.web.Router
import mu.KLogging

class HttpVerticle : AbstractVerticle() {

    companion object : KLogging()

    override fun start(startFuture: Future<Void>) {

        val serverOptions = HttpServerOptions()
        serverOptions.logActivity = HttpInfo.logActivity
        serverOptions.port = HttpInfo.port
        serverOptions.host = HttpInfo.host
        val server = vertx.createHttpServer(serverOptions)
        val router = Router.router(vertx)

        router.mountSubRouter("/api", apiRouter(vertx))
        router.mountSubRouter("/users", actorRouter(vertx))

        router.routeWithRegex("/@(${PUN_CHARS})")
            .handler { rc ->
                val pun = rc.request().getParam("param0")
                rc.response()
                    .setStatusCode(303)
                    .putHeader("Location", "/users/${pun}")
                    .end()
            }

        server.requestHandler { router.accept(it) }
            .rxListen()
            .subscribe(
                {
                    logger.info {
                        "Listening on port ${serverOptions.port} and host ${serverOptions.host} with activity logging set to ${serverOptions.logActivity}"
                    }
                    startFuture.complete()
                },
                {
                    startFuture.fail(it)
                })
    }
}
