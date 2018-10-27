// Copyright (C) 2018 Andrew Newton
package com.swanpipe.verticles

import com.swanpipe.utils.Db
import com.swanpipe.utils.Version
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.web.Router
import mu.KLogging

const val HTTP_CONFIG_NAME = "http"
const val DEFAULT_PORT = 8080
const val DEFAULT_HOST = "localhost"

class Http : AbstractVerticle() {

    companion object : KLogging()

    override fun start(startFuture: Future<Void>) {

        val server = vertx.createHttpServer()
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

        router.routeWithRegex( "/@[^/]*" )
                .handler { rc ->
                    rc.response().putHeader( "Content-Type", "text/plain" )
                            .end( "Hello World" )
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

        val httpConfig : JsonObject? = config().getJsonObject( HTTP_CONFIG_NAME )
        var port = DEFAULT_PORT
        var host = DEFAULT_HOST
        httpConfig?.let {
            port = httpConfig.getInteger( "port" ) ?: DEFAULT_PORT
            host = httpConfig.getString( "host" ) ?: DEFAULT_HOST
        }

        server.requestHandler { router.accept(it) }
                .rxListen( port, host )
                .subscribe(
                        {
                            logger.info { "Listening on port ${port} and host ${host}" }
                            startFuture.complete()
                        },
                        {
                            startFuture.fail(it)
                        })
    }
}
