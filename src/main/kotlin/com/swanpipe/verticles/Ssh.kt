// Copyright (C) 2018 Andrew Newton
package com.swanpipe.verticles

import io.vertx.core.AbstractVerticle
import io.vertx.ext.shell.ShellService
import io.vertx.ext.shell.ShellServiceOptionsConverter
import io.vertx.ext.shell.command.CommandBuilder
import io.vertx.ext.shell.command.CommandRegistry
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.shell.ShellServiceOptions

class Ssh : AbstractVerticle() {
    override fun start() {

        var helloWorld = CommandBuilder.command("hello-world").processHandler { process ->
            process.write("hello world\n")
            process.end()
        }.build(vertx)

        val sshConfig = json {
            obj("sshOptions" to obj(
                    "host" to "localhost",
                    "port" to 5000,
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
        CommandRegistry.getShared(vertx).registerCommand(helloWorld)
        service.start { ar ->
            if (!ar.succeeded()) {
                ar.cause().printStackTrace()
            }
        }
    }
}