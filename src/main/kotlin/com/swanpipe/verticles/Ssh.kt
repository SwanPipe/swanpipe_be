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
package com.swanpipe.verticles

import com.swanpipe.tcs.CheckLogin
import com.swanpipe.tcs.CreateActorLogin
import io.vertx.core.AbstractVerticle
import io.vertx.ext.shell.ShellService
import io.vertx.ext.shell.ShellServiceOptionsConverter
import io.vertx.ext.shell.command.CommandRegistry
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.shell.ShellServiceOptions
import mu.KLogging

const val SSH_CONFIG_NAME = "ssh"

class Ssh : AbstractVerticle() {

    companion object : KLogging()

    override fun start() {

        val host = config().getJsonObject( SSH_CONFIG_NAME ).getString( "host", "localhost" )
        val port = config().getJsonObject( SSH_CONFIG_NAME ).getInteger( "port", 5000 )

        val sshConfig = json {
            obj("sshOptions" to obj(
                    "host" to host,
                    "port" to port,
                    "keyPairOptions" to obj(
                            "path" to "src/main/resources/ssh.jks",
                            "password" to "secret"
                    ),
                    "authOptions" to obj(
                            "provider" to "shiro",
                            "config" to obj("properties_path" to "file:src/main/resources/auth.properties")
                    )
            ))
        }

        val options = ShellServiceOptions()
        ShellServiceOptionsConverter.fromJson( sshConfig, options )
        val service = ShellService.create(vertx, options )
        CommandRegistry.getShared(vertx).registerCommand(CreateActorLogin().command(vertx))
        CommandRegistry.getShared(vertx).registerCommand(CheckLogin().command(vertx))
        service.start { ar ->
            if (!ar.succeeded()) {
                ar.cause().printStackTrace()
            }
            else {
                logger.info { "SSH admin service started on ${host}:${port}" }
            }
        }
    }

}