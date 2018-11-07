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

import com.swanpipe.dao.ActorDao
import io.vertx.core.Vertx
import io.vertx.core.cli.CLI
import io.vertx.ext.shell.command.Command
import io.vertx.ext.shell.command.CommandBuilder
import io.vertx.kotlin.core.cli.Option
import mu.KLogging

class GetActor {

    companion object : KLogging()

    val cli = CLI.create( "get-actor" )
            .setSummary( "Gets information on an actor" )
            .addOption( Option( argName="Preferred User Name", shortName = "p", longName = "pun", required = true ) )
            .addOption( Option( argName = "help", shortName = "h", longName = "help", flag = true, help = true ) )

    fun command( vertx: Vertx) : Command {
        return CommandBuilder.command( cli )
                .processHandler { process ->
                    val commandLine = process.commandLine()
                    val pun = commandLine.getOptionValue<String>( "pun" )
                    ActorDao.getActor( pun )
                            .subscribe(
                                    { actor ->
                                        process.write( "Preferred User Name (pun): '${actor.pun}'\n")
                                        process.write( "Actor created:             ${actor.created}\n")
                                        process.write( "Actor Public Key PEM\n${actor.publicKeyPem}\n")
                                        process.write( "Actor data\n'${actor.data.encodePrettily()}'\n")
                                        process.end()
                                    },
                                    {
                                        logger.error { it }
                                        process.write( "Error getting login: ${it.message}\n")
                                        process.end()
                                    },
                                    {
                                        process.write( "'${pun}' does not exist.\n")
                                        process.end()
                                    }
                            )
                }
                .build( vertx )
    }
}