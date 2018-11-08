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

package com.swanpipe.tcs

import com.swanpipe.actions.ActorActions
import com.swanpipe.actions.ActorLoginActions
import com.swanpipe.actions.LoginActions
import com.swanpipe.utils.ValidationException
import io.vertx.core.Vertx
import io.vertx.core.cli.CLI
import io.vertx.core.json.JsonObject
import io.vertx.ext.shell.command.Command
import io.vertx.ext.shell.command.CommandBuilder
import io.vertx.kotlin.core.cli.Option
import mu.KLogging

/**
 * A class for encampulating a cli command for creating an actor/login.
 */
class CreateActorLogin {

    companion object : KLogging()

    val cli = CLI.create("create-actor-login")
        .setSummary("Create an actor with a corresponding login")
        .addOption(Option(argName = "Login ID", shortName = "l", longName = "login-id", required = true))
        .addOption(Option(argName = "Password", shortName = "p", longName = "password", required = true))
        .addOption(Option(argName = "Preferred User Name", shortName = "u", longName = "pun", required = false))
        .addOption(Option(argName = "Owner", shortName = "o", longName = "owner", required = false))
        .addOption(Option(argName = "help", shortName = "h", longName = "help", flag = true, help = true))

    fun command(vertx: Vertx): Command {
        return CommandBuilder.command(cli)
            .processHandler { process ->
                val commandLine = process.commandLine()
                val loginId = commandLine.getOptionValue<String>("login-id")
                val password = commandLine.getOptionValue<String>("password")
                val pun = commandLine.getOptionValue<String>("pun") ?: loginId
                val owner = commandLine.getOptionValue<Boolean>("owner") ?: true
                val json = JsonObject()
                    .put(LoginActions.ID, loginId)
                    .put(LoginActions.PASSWORD, password)
                    .put(ActorActions.PUN, pun)
                    .put(ActorLoginActions.OWNER, owner)
                ActorLoginActions.createActorLogin(json)
                    .subscribe(
                        { triple ->
                            if (triple.third) {
                                process.write("Created actor of '${triple.second}' with login-id ${triple.first} as owner\n")
                            } else {
                                process.write("Created actor of '${triple.second}' with login-id ${triple.first}\n")
                            }
                            process.end()
                        },
                        {
                            logger.error { it }
                            process.write("Error create actor with login: ${it.message}\n")
                            if (it is ValidationException) {
                                for (m in it.issues) {
                                    process.write("$m\n")
                                }
                            }
                            process.end()
                        }
                    )
            }
            .build(vertx)
    }
}