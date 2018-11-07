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

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.swanpipe.utils.DB_CONFIG_NAME
import com.swanpipe.utils.Db
import com.swanpipe.utils.SCHEMA_CONFIG_NAME
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.flywaydb.core.Flyway
import java.time.LocalDateTime

/**
 * Initializes Postgres for tests.
 */
object InitPg {

    var pgStarted = false
    var pg : EmbeddedPostgres? = null
    val dbConfig = JsonObject()
    var flyway : Flyway? = null

    fun startPg() : InitPg {
        if( !pgStarted ) {
            pg = EmbeddedPostgres
                    .builder()
                    // none of this seems to work for whatever reason
                    //.setServerConfig( "log-connections", "true" )
                    //.setServerConfig( "log-statement", "all" )
                    //.setServerConfig( "log_min_duration_statement", "0" )
                    //.setServerConfig( "log_destination", "stderr" )
                    //.setServerConfig( "logging_collector", "on" )
                    .start()
            println( "Embedded Postgres started on port ${pg!!.getPort()}" )
            pgStarted = true
        }
        pg?.let{
            val config = JsonObject()
            dbConfig.put( SCHEMA_CONFIG_NAME, "public" )
            dbConfig.put( "port", pg!!.port )
            dbConfig.put( "host", "localhost" )
            dbConfig.put( "database", "postgres" )
            dbConfig.put( "user", "postgres" )
            dbConfig.put( "password", "secret" )
            dbConfig.put( "maxSize", "5" )
            config.put( DB_CONFIG_NAME, dbConfig )
            Db.config = config
            Db.installedOn = LocalDateTime.now()
            Db.flywayVersion = "0"
            Db.configuredFlywayVerstion = "0"
            if( !Db.isConfigured() ) {
                throw RuntimeException( "unable to initialize database for testing" )
            }
            flyway = Flyway
                    .configure()
                    .dataSource("jdbc:postgresql://localhost:${pg!!.port}/postgres", "postgres", "secret")
                    .load()
        }
        return this
    }

    fun clean() : InitPg {
        flyway!!.clean()
        return this
    }

    fun migrate() : InitPg {
        flyway!!.migrate()
        return this
    }

    fun pool( vertx: Vertx ) : PgClient {
        val options = PgPoolOptions(dbConfig)
        Db.pgPool = PgClient.pool(vertx, options)
        return Db.pgPool
    }
}
