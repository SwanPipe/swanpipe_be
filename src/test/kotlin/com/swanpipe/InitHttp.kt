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

package com.swanpipe

import com.swanpipe.utils.*
import com.swanpipe.verticles.HttpVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

object InitHttp {

    fun deployHttp( vertx: Vertx, testContext: VertxTestContext ) {
        val config = json { obj (
            HTTP_CONFIG_NAME to obj (
                PORT_CONFIG to 0,
                HOST_CONFIG to "127.0.0.1",
                LOG_ACTIVITY_CONFIG to true,
                INSTANCES_CONFIG to 1
            )
        ) }
        HttpInfo.configure( config )
        vertx.deployVerticle(
            HttpVerticle::class.java.name,
            DeploymentOptions().setConfig( config )
        ) {
            if( it.succeeded() ) {
                testContext.completeNow()
            }
            else {
                testContext.failNow( it.cause() )
            }
        }
    }

}