// Copyright (C) 2018 Andrew Newton
package com.swanpipe.verticles

import com.swanpipe.utils.Db
import com.swanpipe.utils.Db.dbConfig
import com.swanpipe.utils.Db.table
import com.swanpipe.utils.Version
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.DeploymentOptions
import mu.KLogging
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException


/**
 * This is a "Main" verticle. It is used to deploy all other verticles.
 */
class Main : AbstractVerticle() {

    companion object : KLogging()

    override fun start(startFuture: Future<Void>) {

        logger.info( "configuration: ${config()}")

        Completable.complete().concatWith {
            logVersion( it, vertx )
        }.concatWith {
            dbInit( it, vertx, config() )
        }.concatWith {
            deployVerticle( it, Admin() )
        }.concatWith {
            deployVerticle( it, Http() )
        }.subscribe(
                {
                    logger.info( "SwanPipe startup sequence complete" )
                    startFuture.complete()
                },
                {
                    startFuture.fail( it )
                }
        )
    }

    override fun stop(stopFuture: Future<Void>?) {
        logger.info( "Stopping ${this.javaClass.name}")
        Db.pgPool.close()
        super.stop(stopFuture)
    }

    /**
     * This creates a future and deploys the verticle, using [handleVerticleDeployment] as the handler.
     *
     * @return the future
     */
    fun deployVerticle( observer: CompletableObserver, verticle: Verticle ) {
        val future = Future.future<String>()
        val options = DeploymentOptions().setConfig( config() )
        future.setHandler { handleVerticleDeployment( observer, it ) }
        vertx.deployVerticle( verticle, options, future.completer() )
    }

    /**
     * Does something with the deployment of the verticle.
     */
    fun handleVerticleDeployment( observer: CompletableObserver, result: AsyncResult<String>) {
        if( result.succeeded() ) {
            logger.trace{ "Deployment of ${result.result()} succeeded" }
            observer.onComplete()
        }
        else {
            logger.error( "Deployment of ${result.result()} failed", result.cause() )
            observer.onError( result.cause() )
        }
    }

    fun logVersion(observer: CompletableObserver, vertx: Vertx) {
        vertx.fileSystem().readFile("version.json") { result ->
            if (result.succeeded()) {
                val json = result.result().toJsonObject()
                val version: String? = json.getString("version")
                val buildDate: String? = json.getString("buildDate")
                if (version == null || buildDate == null) {
                    logger.error("unable to get version information")
                    observer.onError(RuntimeException("unable to get version information"))
                } else {
                    Version.version = version
                    try {
                        Version.buildDate = OffsetDateTime.parse(buildDate)
                        logger.info("Starting version=${Version.version} buildDate=${Version.buildDate}")
                        observer.onComplete()
                    } catch (e: DateTimeParseException) {
                        logger.error(e.message)
                        observer.onError(e)
                    }
                }
            } else {
                logger.error("unable to read version file")
                observer.onError(RuntimeException("unable to read version file"))
            }
        }
    }

    fun dbInit(observer: CompletableObserver, vertx: Vertx, config: JsonObject) {
        Db.config = config
        if (Db.isConfigured()) {

            val options = PgPoolOptions(dbConfig)

            // Create the client pool
            val client = PgClient.pool(vertx, options)

            // A simple query
            Db.configuredFlywayVerstion = dbConfig.getString("flywayVersion")
            val sql = "select version, installed_on from ${table("flyway_schema_history")} order by version desc"
            client.query(sql) { ar ->
                if (ar.succeeded()) {
                    val result = ar.result()
                    logger.trace("Got ${result.size()} rows ")
                    var versionMatch = false
                    result.forEachIndexed { index, row ->
                        if (index == 0) {
                            Db.flywayVersion = row.getString(0)
                            Db.installedOn = row.getLocalDateTime(1)
                        }
                        if (row.getString(0).equals(Db.configuredFlywayVerstion)) {
                            versionMatch = true
                        }
                    }
                    if (!versionMatch) {
                        logger.error("flyway version does not match")
                        observer.onError(RuntimeException("flyway version does not match"))
                    } else {
                        logger.info("Database flyway version ${Db.configuredFlywayVerstion} confirmed.")
                        logger.info("Database is at version ${Db.flywayVersion} install on ${Db.installedOn}")
                        Db.pgPool = client
                        observer.onComplete()
                    }
                } else {
                    logger.error("Flyway inspection failure: sql=${sql} message=${ar.cause().message}")
                    observer.onError(ar.cause())
                }
            }
        } else {
            logger.error { "database not configured: dbConfig = ${dbConfig}" }
            observer.onError(RuntimeException("database does not appear to be configured"))
        }
    }
}

