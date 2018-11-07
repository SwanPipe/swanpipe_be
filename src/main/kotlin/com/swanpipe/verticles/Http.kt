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
import com.swanpipe.utils.Db
import com.swanpipe.utils.Version
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.http.HttpServerOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.web.Router
import mu.KLogging

const val HTTP_CONFIG_NAME = "http"
const val DEFAULT_PORT = 8080
const val DEFAULT_HOST = "localhost"
const val INSTANCES = "instances"

class Http : AbstractVerticle() {

    companion object : KLogging()

    override fun start(startFuture: Future<Void>) {

        val serverOptions = HttpServerOptions()
        val httpConfig : JsonObject? = config().getJsonObject( HTTP_CONFIG_NAME )
        httpConfig?.let{
            serverOptions.logActivity = httpConfig.getBoolean( "logActivity", false )
            serverOptions.port = httpConfig.getInteger( "port", DEFAULT_PORT )
            serverOptions.host = httpConfig.getString( "host", DEFAULT_HOST )
        }
        val server = vertx.createHttpServer( serverOptions )
        val router = Router.router( vertx )

        router.get("/api/v1/instance")
                .handler { rc ->
                    rc.response().putHeader("Content-Type", "application/json")
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

        router.routeWithRegex( "/@(${PUN_CHARS})" )
                .handler { rc ->
                    val pun = rc.request().getParam( "param0" )
                    rc.response()
                            .setStatusCode( 303 )
                            .putHeader( "Location", "http://localhost/users/${pun}" )
                            .end()
                }

        router.routeWithRegex( "/@.*/" )
                .handler { rc ->
                    rc.response().putHeader( "Content-Type", "text/plain" )
                            .end( "Hello World 2" )
                }

        router.routeWithRegex( "/@.*/follows" )
                .handler { rc ->
                    rc.response().putHeader( "Content-Type", "text/plain" )
                            .end( "Follows" )
                }

        server.requestHandler { router.accept(it) }
                .rxListen()
                .subscribe(
                        {
                            logger.info {
                                "Listening on port ${serverOptions.port} and host ${serverOptions.host} with activity logging set to ${serverOptions.logActivity}" }
                            startFuture.complete()
                        },
                        {
                            startFuture.fail(it)
                        })
    }
}
