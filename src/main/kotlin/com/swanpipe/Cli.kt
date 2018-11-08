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

import com.swanpipe.verticles.Main
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import mu.KLogging
import java.io.File

fun main(args: Array<String>) {

    println("starting....")
    val config = File("src/main/resources/default-config.json").readText()
    val vertx = Vertx.vertx()
    val options = DeploymentOptions().setConfig(JsonObject(config))
    vertx.deployVerticle(Main::class.java.name, options) { ar ->
        if (ar.succeeded()) {
            KLogging().logger.info("Deployment of ${ar.result()} succeeded")
        } else {
            KLogging().logger.error("Unable to deploy SwanPipe main verticle: ${ar.cause()}")
            vertx.close()
        }
    }
}
