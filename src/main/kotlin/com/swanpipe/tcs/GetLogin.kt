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

import com.swanpipe.daos.LoginDao
import io.vertx.core.Vertx
import io.vertx.core.cli.CLI
import io.vertx.ext.shell.command.Command
import io.vertx.ext.shell.command.CommandBuilder
import io.vertx.kotlin.core.cli.Option
import mu.KLogging

class GetLogin {

    companion object : KLogging()

    val cli = CLI.create( "get-login" )
            .setSummary( "Gets information on a login" )
            .addOption( Option( argName="Login ID", shortName = "l", longName = "login-id", required = true ) )
            .addOption( Option( argName = "help", shortName = "h", longName = "help", flag = true, help = true ) )

    fun command( vertx: Vertx) : Command {
        return CommandBuilder.command( cli )
                .processHandler { process ->
                    val commandLine = process.commandLine()
                    val loginId = commandLine.getOptionValue<String>( "login-id" )
                    LoginDao.getLogin( loginId )
                            .subscribe(
                                    { login ->
                                        process.write( "Login ID:      '${login.id}'\n")
                                        process.write( "Login enabled: ${login.enabled}\n")
                                        process.write( "Login created: ${login.created}\n")
                                        process.write( "Login data\n'${login.data.encodePrettily()}'\n")
                                        process.end()
                                    },
                                    {
                                        logger.error { it }
                                        process.write( "Error getting login: ${it.message}\n")
                                        process.end()
                                    },
                                    {
                                        process.write( "'${loginId}' does not exist.\n")
                                        process.end()
                                    }
                            )
                }
                .build( vertx )
    }
}