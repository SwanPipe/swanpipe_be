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

package com.swanpipe.daos

import com.swanpipe.utils.Db
import com.swanpipe.utils.Db.table
import io.reactiverse.pgclient.data.Json
import io.reactiverse.reactivex.pgclient.PgClient
import io.reactiverse.reactivex.pgclient.Row
import io.reactiverse.reactivex.pgclient.Tuple
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.json.JsonObject

data class Config(
    val id: String,
    val data: JsonObject
)

object ConfigDao {

    fun mapRowToConfig( row: Row) : Config {
        return Config(
            id = row.getString( "id" ),
            data = row.getJson( "data" ).value() as JsonObject
        )
    }

    fun getConfig( id: String ) : Maybe<Config> {
        return PgClient( Db.pgPool )
            .rxPreparedQuery(
                """
                    select id, data from ${table("config")} where id = $1
                """.trimIndent(),
                Tuple.of( id )
            )
            .flatMapMaybe<Config> { pgRowSet ->
                if( pgRowSet.size() != 0 ) {
                    val row = pgRowSet.iterator().next()
                    Maybe.just( mapRowToConfig( row ) )
                }
                else {
                    Maybe.empty()
                }
            }
    }

    fun setConfig( id: String, data: JsonObject, loginId: String, role: String ) : Single<Config> {
        return PgClient( Db.pgPool )
            .rxPreparedQuery(
                """
                    insert into ${table("config")}
                        ( id, data )
                    select
                        $1, $2::jsonb
                    where
                        ( select data#>'{roles}' ? $4 from ${table("login")} where ${table("login")}.id = $3 )
                    on conflict( id )
                    do update set data = $2::jsonb
                    returning id, data
                """.trimIndent(),
                Tuple.of( id, Json.create( data ), loginId, role )
            )
            .map { pgRowSet ->
                mapRowToConfig( pgRowSet.iterator().next() )
            }
    }

}