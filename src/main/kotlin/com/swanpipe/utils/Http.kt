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
/**
 * 8080
 */
const val DEFAULT_PORT = 8080
/**
 * "localhost"
 */
const val DEFAULT_HOST = "localhost"
const val INSTANCES_CONFIG = "instances"
const val PORT_CONFIG = "port"
const val HOST_CONFIG = "host"
const val LOG_ACTIVITY_CONFIG = "logActivity"

// Content type stuffs
const val ACTIVITY_JSON_TYPE = "application/activity+json"
const val JSON_TYPE = "application/json"

// Header stuffs
const val CONTENT_TYPE_HEADER = "Content-Type"
const val ACCEPT_HEADER = "Accept"
const val AUTHORIZATION_HEADER = "Authorization"


object HttpInfo {

    /**
     * The host to listen on. Default is [DEFAULT_PORT].
     */
    var host : String = DEFAULT_HOST
    /**
     * The port to listen on. Default is [DEFAULT_PORT].
     */
    var port : Int = DEFAULT_PORT
    /**
     * The actual port being listend on. Differs from [port] when listening on an ephemeral port.
     */
    var actualPort : Int = port
    /**
     * Log web activity if true.
     */
    var logActivity = false
    /**
     * The number of HTTP verticle instances to deploy. Default is the number of processors available.
     */
    var instances = Runtime.getRuntime().availableProcessors()

    lateinit var config : JsonObject

    fun configure( config : JsonObject ) {
        val httpConfig: JsonObject? = config.getJsonObject(HTTP_CONFIG_NAME)
        httpConfig?.let {
            logActivity = httpConfig.getBoolean( LOG_ACTIVITY_CONFIG, false)
            port = httpConfig.getInteger(PORT_CONFIG, DEFAULT_PORT)
            host = httpConfig.getString(HOST_CONFIG, DEFAULT_HOST)
            instances = httpConfig.getInteger( INSTANCES_CONFIG, instances )
        }
    }

    fun here( rc : RoutingContext ) : String {
        val mountPoint : String? = rc.mountPoint()
        mountPoint?.let {
            return "${rc.request().scheme()}://${host}:${port}${mountPoint}"
        }
        return "${rc.request().scheme()}://${host}:${port}"
    }
}