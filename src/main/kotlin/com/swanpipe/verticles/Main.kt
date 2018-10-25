// Copyright (C) 2018 Andrew Newton
package com.swanpipe.verticles

import io.vertx.core.*
import io.vertx.kotlin.core.DeploymentOptions
import mu.KLogging



/**
 * This is a "Main" verticle. It is used to deploy all other verticles.
 */
class Main : AbstractVerticle() {

    companion object : KLogging()

    override fun start(startFuture: Future<Void>) {

        logger.info( "configuration: ${config()}")

        /**
         * Stage1 would be things that initialize resources, or perhaps verify resources are available
         * for a fail-fast start
         */
        val stage1 = listOf( Version().logVersion( vertx ) )

        /**
         * Stage2 are the verticles that are business logice etc...
         */
        val stage2 = emptyList<Verticle>()

        /**
         * Stage3 would bring up the endpoints and things that start "listening" for requests.
         */
        val stage3 = emptyList<Verticle>()

        CompositeFuture.all(
                stage1
        ).compose { _ ->
            CompositeFuture.all(
                    stage2.map{ deployVerticle( it ) }
            )
        }.compose { _ ->
            CompositeFuture.all(
                    stage3.map{ deployVerticle( it ) }
            )
        }.setHandler{ ar ->
            if( ar.succeeded() ) {
                logger.info { "Startup Sequence Complete" }
                startFuture.complete()
            }
            else {
                startFuture.fail( ar.cause() )
            }
        }

    }

    /**
     * This creates a future and deploys the verticle, using [handleVerticleDeployment] as the handler.
     *
     * @return the future
     */
    fun deployVerticle( verticle: Verticle ) : Future<String> {
        val future = Future.future<String>()
        val options = DeploymentOptions().setConfig( config() )
        future.setHandler { handleVerticleDeployment( it ) }
        vertx.deployVerticle( verticle, options, future.completer() )
        return future
    }

    /**
     * Does something with the deployment of the verticle.
     */
    fun handleVerticleDeployment(result: AsyncResult<String>) {
        if( result.succeeded() ) {
            logger.debug{ "Deployment of ${result.result()} succeeded" }
        }
        else {
            logger.error( "Deployment of ${result.result()} failed", result.cause() )
        }
    }
}

