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

import com.swanpipe.actions.LoginActions
import io.vertx.core.Vertx
import io.vertx.core.cli.CLI
import io.vertx.ext.shell.command.Command
import io.vertx.ext.shell.command.CommandBuilder
import io.vertx.kotlin.core.cli.Option
import mu.KLogging

class CheckLogin {

    companion object : KLogging()

    val cli = CLI.create("check-login")
        .setSummary("Checks that a login is working")
        .addOption(Option(argName = "Login ID", shortName = "l", longName = "login-id", required = true))
        .addOption(Option(argName = "Password", shortName = "p", longName = "password", required = true))
        .addOption(Option(argName = "help", shortName = "h", longName = "help", flag = true, help = true))

    fun command(vertx: Vertx): Command {
        return CommandBuilder.command(cli)
            .processHandler { process ->
                val commandLine = process.commandLine()
                val loginId = commandLine.getOptionValue<String>("login-id")
                val password = commandLine.getOptionValue<String>("password")
                LoginActions.checkLogin(loginId, password)
                    .subscribe(
                        {
                            process.write("Password matches for '${loginId}'\n")
                            process.end()
                        },
                        {
                            logger.error { it }
                            process.write("Error checking login: ${it.message}\n")
                            process.end()
                        },
                        {
                            process.write("Password '${password}' does not match for '${loginId}' or login is disabled\n")
                            process.end()
                        }
                    )
            }
            .build(vertx)
    }
}