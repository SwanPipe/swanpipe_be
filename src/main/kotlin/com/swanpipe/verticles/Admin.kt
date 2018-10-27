// Copyright (C) 2018 Andrew Newton
package com.swanpipe.verticles

import io.vertx.core.Future
import io.vertx.reactivex.core.AbstractVerticle
import mu.KLogging

class Admin : AbstractVerticle() {

    companion object : KLogging()

    override fun start(startFuture: Future<Void>) {
        logger.info { "Admin verticle started." }
        startFuture.complete()
    }
}