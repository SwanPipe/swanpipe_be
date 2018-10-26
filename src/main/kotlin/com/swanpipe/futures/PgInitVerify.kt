// Copyright (c) 2018 Andrew Newton
package com.swanpipe.futures

import com.swanpipe.utils.Db
import com.swanpipe.utils.Db.dbConfig
import com.swanpipe.utils.Db.table
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import mu.KLogging

class PgInitVerify {

    companion object : KLogging()

    fun execute( vertx: Vertx, config: JsonObject ) : Future<Void> {

        return Future.future<Void> { future ->

            Db.config = config
            if( Db.isConfigured() ) {

                val options = PgPoolOptions( dbConfig )

                // Create the client pool
                val client = PgClient.pool(vertx, options)

                // A simple query
                Db.configuredFlywayVerstion = dbConfig.getString( "flywayVersion" )
                val sql = "select version, installed_on from ${table("flyway_schema_history")} order by version desc"
                client.query( sql ) { ar ->
                    if (ar.succeeded()) {
                        val result = ar.result()
                        logger.trace("Got ${result.size()} rows ")
                        var versionMatch = false
                        result.forEachIndexed { index, row ->
                            if( index == 0 ) {
                                Db.flywayVersion = row.getString( 0 )
                                Db.installedOn = row.getLocalDateTime( 1 )
                            }
                            if( row.getString(0).equals( Db.configuredFlywayVerstion ) ) {
                                versionMatch = true
                            }
                        }
                        if( !versionMatch ) {
                            logger.error( "flyway version does not match")
                            future.fail( "flyway version does not match" )
                        }
                        else {
                            logger.info( "Database flyway version ${Db.configuredFlywayVerstion} confirmed.")
                            logger.info( "Database is at version ${Db.flywayVersion} install on ${Db.installedOn}")
                            future.complete()
                        }
                    } else {
                        logger.error("Flyway inspection failure: sql=${sql} message=${ar.cause().message}")
                        future.fail( ar.cause() )
                    }

                    Db.pgPool = client
                }

            }
            else {
                logger.error { "database not configured: dbConfig = ${dbConfig}" }
                future.fail( "database does not appear to be configured" )
            }

        }



    }


}