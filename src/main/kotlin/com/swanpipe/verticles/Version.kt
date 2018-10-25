// Copyright (C) 2018 Andrew Newton
package com.swanpipe.verticles

import io.vertx.core.Future
import io.vertx.core.Vertx
import mu.KLogging

class Version {

    companion object : KLogging()

    fun logVersion( vertx: Vertx ) : Future<Void> {
        return Future.future<Void> { future ->
            vertx.fileSystem().readFile("version.json") { result ->
                if (result.succeeded()) {
                    val json = result.result().toJsonObject()
                    val version : String? = json.getString( "version" )
                    val buildDate : String? = json.getString( "buildDate" )
                    if( version == null || buildDate == null ) {
                        logger.error( "unable to get version information" )
                        future.fail( result.cause() )
                    }
                    else {
                        logger.info( "Starting version=${version} buildDate=${buildDate}")
                        future.complete()
                    }
                } else {
                    logger.error( "unable to start VersionVerticle" )
                    future.fail( result.cause() )
                }
            }
        }
    }

}

