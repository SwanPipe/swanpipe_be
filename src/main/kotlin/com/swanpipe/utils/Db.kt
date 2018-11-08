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
package com.swanpipe.utils

import io.reactiverse.pgclient.PgPool
import io.vertx.core.json.JsonObject
import java.time.LocalDateTime

const val DB_CONFIG_NAME = "db"
const val SCHEMA_CONFIG_NAME = "schema"

object Db {

    var config: JsonObject? = null

    lateinit var dbConfig: JsonObject

    lateinit var pgPool: PgPool

    lateinit var flywayVersion: String

    lateinit var installedOn: LocalDateTime

    lateinit var configuredFlywayVerstion: String

    fun schema(): String? {
        if (isConfigured()) {
            return dbConfig.getString(SCHEMA_CONFIG_NAME)
        }
        return null
    }

    fun table(name: String): String? {
        if (isConfigured()) {
            return "${schema()}.${name}"
        }
        return null
    }

    fun isConfigured(): Boolean {
        return config?.let { _ ->
            if (config!!.getJsonObject(DB_CONFIG_NAME) != null) {
                dbConfig = config!!.getJsonObject(DB_CONFIG_NAME)
                true
            } else {
                false
            }
        } ?: false
    }
}
