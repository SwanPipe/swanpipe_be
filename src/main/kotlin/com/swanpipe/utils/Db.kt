//Copyright (C) 2018 Andrew Newton
package com.swanpipe.utils

import io.vertx.core.json.JsonObject

const val DB_CONFIG_NAME = "db"
const val SCHEMA_CONFIG_NAME = "schema"

object Db {

    var config: JsonObject? = null

    var dbConfig : JsonObject? = null

    fun schema() : String? {
        if( isConfigured() ) {
            return dbConfig!!.getString( SCHEMA_CONFIG_NAME )
        }
        return null
    }

    fun table( name : String ) : String? {
        if( isConfigured() ) {
            return "${schema()}.${name}"
        }
        return null
    }

    fun isConfigured() : Boolean {
        return config?.let { _ ->
            if( dbConfig == null ) {
                dbConfig = config!!.getJsonObject( DB_CONFIG_NAME )
            }
            dbConfig?.let { true } ?: false
        } ?: false
    }
}