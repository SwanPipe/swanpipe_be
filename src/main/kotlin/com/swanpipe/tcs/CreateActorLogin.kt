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

import com.swanpipe.dao.ActorLoginDao
import io.vertx.core.Vertx
import io.vertx.core.cli.CLI
import io.vertx.ext.shell.command.Command
import io.vertx.ext.shell.command.CommandBuilder
import io.vertx.kotlin.core.cli.Option
import mu.KLogging

/**
 * A class for encampulating a cli command for creating an actor/login.
 */
class CreateActorLogin() {

    companion object : KLogging()

    val cli = CLI.create( "create-actor-login" )
            .setSummary( "Create an actor with a corresponding login" )
            .addOption( Option( argName="Login ID", shortName = "l", longName = "login-id", required = true ) )
            .addOption( Option( argName="Password", shortName = "p", longName = "password", required = true ) )
            .addOption( Option( argName="Preferred User Name", shortName = "u", longName = "pun", required = false ) )
            .addOption( Option( argName="Owner", shortName = "o", longName = "owner", required = false ) )
            .addOption( Option( argName = "help", shortName = "h", longName = "help", flag = true, help = true ) )

    fun command( vertx: Vertx ) : Command {
        return CommandBuilder.command( cli )
                .processHandler { process ->
                    val commandLine = process.commandLine()
                    val loginId = commandLine.getOptionValue<String>( "login-id" )
                    val password = commandLine.getOptionValue<String>( "password" )
                    val pun = commandLine.getOptionValue<String>( "pun" )?: loginId
                    val owner = commandLine.getOptionValue<Boolean>( "owner" )?: true
                    ActorLoginDao.createActorLogin( loginId = loginId, password = password, pun = pun, owner = owner )
                            .subscribe(
                                    { triple ->
                                        if( triple.third ) {
                                            process.write( "Actor of ${triple.second} with login-id ${triple.first} as owner\n")
                                        }
                                        else {
                                            process.write( "Actor of ${triple.second} with login-id ${triple.first}\n")
                                        }
                                        process.end()
                                    },
                                    {
                                        logger.error { it }
                                        process.write( "Error create actor with login: ${it.message}\n")
                                        process.end()
                                    }
                            )
                }
                .build(vertx)
    }
}