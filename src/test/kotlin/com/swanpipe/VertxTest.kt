package com.swanpipe

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName( "Test of A Veritcle Deployment" )
@ExtendWith( VertxExtension::class )
object TestMain {

    @DisplayName( "Test simple verticle deployment" )
    @Test
    fun testMainVerticle(vertx : Vertx, testContext: VertxTestContext) {

        vertx.deployVerticle( TestVerticle(),
                testContext.succeeding{
                    testContext.completeNow()
                }
        )

    }
}

class TestVerticle : AbstractVerticle() {

    override fun start(startFuture: Future<Void>) {
        startFuture.complete()
    }

    override fun stop(stopFuture: Future<Void>) {
        stopFuture.complete()
    }
}
