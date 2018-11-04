/*
 * Copyright (c) 2018. Andrew Newton
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

package com.swanpipe.actions

import com.swanpipe.utils.Db
import com.swanpipe.utils.Db.table
import com.swanpipe.utils.genRsa2048
import io.reactiverse.pgclient.data.Json
import io.reactiverse.reactivex.pgclient.PgClient
import io.reactiverse.reactivex.pgclient.Row
import io.reactiverse.reactivex.pgclient.Tuple
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import java.time.OffsetDateTime

data class Actor(
        val json: JsonObject,
        val created : OffsetDateTime,
        val privateKey : Buffer
        //TODO see about a JSON database object for user data and move display name into it
        //TODO see about a JSON database object for system data
        )

fun mapRowToActor( row : Row ) : Actor {
    return Actor(
            json = json {
                obj(
                        "name" to row.getString("name"),
                        "publicKeyPem" to row.getString( "public_key_pem" )
                )
            },
            created = row.delegate.getOffsetDateTime( "created" ),
            privateKey = row.delegate.getBuffer( "private_key" )
    )
}

fun createActor( name : String ) : Single<Actor> {
    val keypair = genRsa2048()
    return PgClient( Db.pgPool )
            .rxPreparedQuery(
                    """insert into ${table("actor")}
                        | ( name, public_key_pem, private_key )
                        | values ($1,$2,$3) returning
                        | name, created, public_key_pem, private_key""".trimMargin(),
                    Tuple.of( name, keypair.first, keypair.second ))
            .map { pgRowSet ->
                mapRowToActor( pgRowSet.iterator().next() )
            }
}

fun getActor( name: String ) : Maybe<Actor> {
    return PgClient( Db.pgPool )
            .rxPreparedQuery(
                    """select
                        | name,
                        | created,
                        | public_key_pem,
                        | private_key from ${table("actor")}
                        |where name = $1""".trimMargin(),
                    Tuple.of( name ))
            .flatMapMaybe<Actor> { pgRowSet ->
                if( pgRowSet.size() != 0 ) {
                    val row = pgRowSet.iterator().next()
                    Maybe.just( mapRowToActor( row ) )
                }
                else {
                    Maybe.empty()
                }
            }
}
