// Copyright (C) 2018 Andrew Newton
package com.swanpipe.verticles

import com.swanpipe.utils.Db
import com.swanpipe.utils.Db.table
import io.reactiverse.reactivex.pgclient.PgClient
import io.reactiverse.reactivex.pgclient.Tuple
import io.reactivex.Single
import io.vertx.core.AbstractVerticle
import io.vertx.ext.shell.ShellService
import io.vertx.ext.shell.ShellServiceOptionsConverter
import io.vertx.ext.shell.command.CommandBuilder
import io.vertx.ext.shell.command.CommandRegistry
import io.vertx.kotlin.core.cli.Argument
import io.vertx.kotlin.core.cli.Option
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.shell.ShellServiceOptions
import io.vertx.core.cli.CLI
import mu.KLogging

class Ssh : AbstractVerticle() {

    companion object : KLogging()

    override fun start() {

        var createFullAccountCli = CLI.create( "create-full-account" )
                .setSummary( "Create a full account including login and persona." )
                .addArgument( Argument( "persona-id" ))
                .addArgument( Argument( "display-name" ))
                .addArgument( Argument( "login-id" ))
                .addArgument( Argument( "password" ))
                .addOption( Option( argName = "help", shortName = "h", longName = "help", flag = true, help = true ))
        var createFullAccount = CommandBuilder.command( createFullAccountCli ).processHandler { process ->
            val commandLine = process.commandLine()
            val personaId = commandLine.getArgumentValue<String>( "persona-id" )
            val displayName = commandLine.getArgumentValue<String>( "display-name" )
            val loginId = commandLine.getArgumentValue<String>( "login-id" )
            val password = commandLine.getArgumentValue<String>( "password" )
            createFullAccount( loginId = loginId, personaId = personaId, displayName = displayName, password = password )
                .subscribe(
                    { id ->
                        process.write( "Account created with ID ${id}\n")
                        process.end()
                    },
                    {
                        logger.error { it }
                        process.write(( "Error creating account: ${it.message}\n"))
                        process.end( 1 )
                    }
                )
        }.build(vertx)

        var createAccount = CommandBuilder.command("create-account").processHandler { process ->
            createAccount().subscribe(
                    { id ->
                        process.write( "Account created with ID ${id}\n")
                        process.end()
                    },
                    {
                        logger.error { it }
                        process.write(( "Error creating account: ${it.message}\n"))
                        process.end( 1 )
                    }
            )
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
        CommandRegistry.getShared(vertx).registerCommand(createAccount)
        CommandRegistry.getShared(vertx).registerCommand(createFullAccount)
        service.start { ar ->
            if (!ar.succeeded()) {
                ar.cause().printStackTrace()
            }
        }
    }

    fun createAccount() : Single<Int> {
        return PgClient( Db.pgPool ).rxPreparedQuery( "insert into ${table("account")} ( id ) values (default) returning id")
                .map { pgRowSet ->
                    pgRowSet.iterator().next().getInteger( "id" )
                }
    }

    fun createFullAccount( loginId : String, password : String, personaId: String, displayName : String ) : Single<Int> {
        return PgClient( Db.pgPool ).rxPreparedQuery(
                """ WITH ins1 AS (
                       INSERT INTO ${table("account")} (id)
                       VALUES (default)
                       RETURNING id AS account_id
                    )
                    , ins2 AS (
                       INSERT INTO ${table("login")} (id, account_id,password)
                       SELECT $1, account_id, $2 FROM ins1
                       RETURNING account_id
                    )
                    INSERT INTO ${table("persona")} (id, account_id,display_name)
                    SELECT $3, account_id, $4 FROM ins2
                    RETURNING account_id
                """.trimIndent(), Tuple.of( loginId, password, personaId, displayName ) )
                .map { pgRowSet ->
                    pgRowSet.iterator().next().getInteger( "account_id" )
                }
    }
}