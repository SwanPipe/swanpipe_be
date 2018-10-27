// Copyright (C) 2018 Andrew Newton
package com.swanpipe.verticles

import com.swanpipe.utils.Db
import com.swanpipe.utils.Version
import io.vertx.core.Future
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.web.Router
import mu.KLogging

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

        server.requestHandler { router.accept(it) }
                .rxListen(8080)
                .subscribe(
                        {
                            logger.info { "Listening on port 8080" }
                            startFuture.complete()
                        },
                        {
                            startFuture.fail(it)
                        })
    }
}
