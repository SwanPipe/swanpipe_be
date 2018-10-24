// Copyright (C) 2018 Andrew Newton
package com.swanpipe.verticles

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import mu.KLogging

class VersionVerticle : AbstractVerticle() {

    companion object : KLogging()

    override fun start( startFuture: Future<Void>) {
        vertx.fileSystem().readFile("version.json") { result ->
            if (result.succeeded()) {
                val json = result.result().toJsonObject()
                val version : String? = json.getString( "version" )
                val buildDate : String? = json.getString( "buildDate" )
                if( version == null || buildDate == null ) {
                    logger.error( "unable to get version information" )
                    startFuture.fail( result.cause() )
                }
                else {
                    logger.info( "Starting version=${version} buildDate=${buildDate}")
                    startFuture.complete()
                }
            } else {
                logger.error( "unable to start VersionVerticle" )
                startFuture.fail( result.cause() )
            }
        }
    }

    override fun stop( stopFuture: Future<Void> ) {
        logger.info { "version verticle stopping" }
        stopFuture.complete()
    }
}