// Copyright (C) 2018 Andrew Newton
package com.swanpipe

import com.swanpipe.verticles.Main
import io.vertx.core.AsyncResult
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.io.File

fun main( args: Array<String> ) {

    println( "starting...." )
    val config = File( "src/main/resources/default-config.json").readText()
    val vertx = Vertx.vertx()
    val options = DeploymentOptions().setConfig( JsonObject( config ) )
    vertx.deployVerticle( Main::class.java.name, options ) { handleVerticleDeployment( it ) }
}

fun handleVerticleDeployment(result: AsyncResult<String>) {
    if( result.succeeded() ) {
        println( "Deployment of ${result.result()} succeeded")
    }
    else {
        println( "Deployment of ${result.result()} failed")
        Vertx.vertx().close()
    }
}