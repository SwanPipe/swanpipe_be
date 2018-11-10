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

import io.vertx.core.json.JsonObject
import io.vertx.reactivex.ext.web.RoutingContext

// Configuration constants
const val HTTP_CONFIG_NAME = "http"
const val DEFAULT_PORT = 8080
const val DEFAULT_HOST = "localhost"
const val INSTANCES = "instances"
const val LOG_ACTIVITY = "logActivity"

// Content type stuffs
const val ACTIVITY_JSON_TYPE = "application/activity+json"
const val JSON_TYPE = "application/json"

// Header stuffs
const val CONTENT_TYPE_HEADER = "Content-Type"

object HttpInfo {

    var host : String = DEFAULT_HOST
    var port : Int = DEFAULT_PORT
    var logActivity = false
    var instances = Runtime.getRuntime().availableProcessors()

    lateinit var config : JsonObject

    fun configure( config : JsonObject ) {
        val httpConfig: JsonObject? = config.getJsonObject(HTTP_CONFIG_NAME)
        httpConfig?.let {
            logActivity = httpConfig.getBoolean("logActivity", false)
            port = httpConfig.getInteger("port", DEFAULT_PORT)
            host = httpConfig.getString("host", DEFAULT_HOST)
            instances = httpConfig.getInteger( INSTANCES, instances )
        }
    }

    fun here( rc : RoutingContext ) : String {
        return "${rc.request().scheme()}://${host}:${port}"
    }
}