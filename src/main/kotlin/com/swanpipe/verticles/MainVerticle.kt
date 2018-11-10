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
package com.swanpipe.verticles

import com.swanpipe.utils.Db
import com.swanpipe.utils.Db.dbConfig
import com.swanpipe.utils.Db.table
import com.swanpipe.utils.Version
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPoolOptions
import io.reactivex.Single
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.RxHelper
import mu.KLogging
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException


/**
 * This is a "MainVerticle" verticle. It is used to deploy all other verticles.
 */
class MainVerticle : AbstractVerticle() {

    companion object : KLogging()

    override fun start(startFuture: Future<Void>) {

        logger.info("configuration: \n${config().encodePrettily()}")

        logVersion(vertx)
            .flatMap {
                dbInit(vertx, config())
            }
            .flatMap {
                config().getJsonObject(SSH_CONFIG_NAME)?.let {
                    val options = DeploymentOptions().setConfig(config())
                    RxHelper.deployVerticle(io.vertx.reactivex.core.Vertx(vertx), Ssh(), options)
                }
                    ?: Single.just("No ssh configuration found. SSH veritcle not deployed")
            }
            .flatMap {
                logger.info { "verticle deployment: ${it}" }
                val options = DeploymentOptions().setConfig(config())
                val httpConfig = config().getJsonObject(HTTP_CONFIG_NAME)
                if (httpConfig.getInteger(INSTANCES) != null) {
                    options.instances = httpConfig.getInteger(INSTANCES)
                } else {
                    options.instances = Runtime.getRuntime().availableProcessors()
                }
                deployVerticle(vertx, Http::class.java.name, options)
            }
            .subscribe(
                {
                    logger.info { "verticle deployment: ${it}" }
                    logger.info("SwanPipe startup sequence complete")
                    startFuture.complete()
                },
                {
                    startFuture.fail(it)
                }
            )

    }

    override fun stop(stopFuture: Future<Void>?) {
        logger.info("Stopping ${this.javaClass.name}")
        Db.pgPool.close()
        super.stop(stopFuture)
    }

    fun logVersion(vertx: Vertx): Single<Boolean> {
        return Single.create { emitter ->
            vertx.fileSystem().readFile("version.json") { result ->
                if (result.succeeded()) {
                    val json = result.result().toJsonObject()
                    val version: String? = json.getString("version")
                    val buildDate: String? = json.getString("buildDate")
                    if (version == null || buildDate == null) {
                        logger.error("unable to get version information")
                        emitter.onError(RuntimeException("unable to get version information"))
                    } else {
                        Version.version = version
                        try {
                            Version.buildDate = OffsetDateTime.parse(buildDate)
                            logger.info("Starting version=${Version.version} buildDate=${Version.buildDate}")
                            emitter.onSuccess(true)
                        } catch (e: DateTimeParseException) {
                            logger.error(e.message)
                            emitter.onError(e)
                        }
                    }
                } else {
                    logger.error("unable to read version file")
                    emitter.onError(RuntimeException("unable to read version file"))
                }
            }
        }
    }

    fun dbInit(vertx: Vertx, config: JsonObject): Single<Boolean> {
        return Single.create { emitter ->
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
                            emitter.onError(RuntimeException("flyway version does not match"))
                        } else {
                            logger.info("Database flyway version ${Db.configuredFlywayVerstion} confirmed.")
                            logger.info("Database is at version ${Db.flywayVersion} install on ${Db.installedOn}")
                            Db.pgPool = client
                            emitter.onSuccess(true)
                        }
                    } else {
                        logger.error("Flyway inspection failure: sql=${sql} message=${ar.cause().message}")
                        emitter.onError(ar.cause())
                    }
                }
            } else {
                logger.error { "database not configured: dbConfig = ${dbConfig}" }
                emitter.onError(RuntimeException("database does not appear to be configured"))
            }
        }
    }

    fun deployVerticle(vertx: Vertx, name: String, options: DeploymentOptions): Single<String> {
        return Single.create { emmitter ->
            vertx.deployVerticle(name, options) { ar ->
                if (ar.succeeded()) {
                    emmitter.onSuccess(ar.result())
                } else {
                    emmitter.onError(ar.cause())
                }
            }
        }
    }

}

