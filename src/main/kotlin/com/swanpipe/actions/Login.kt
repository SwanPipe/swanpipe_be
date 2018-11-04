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
import io.reactiverse.reactivex.pgclient.PgClient
import io.reactiverse.reactivex.pgclient.Row
import io.reactiverse.reactivex.pgclient.Tuple
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.json.JsonObject
import org.mindrot.jbcrypt.BCrypt
import java.time.OffsetDateTime

data class Login(
        val id: String,
        val password: String,
        val enabled: Boolean,
        val created : OffsetDateTime,
        val data : JsonObject
        )

fun mapRowToLogin( row : Row ) : Login {
    return Login(
            id = row.getString( "id" ),
            password = row.getString( "password" ),
            enabled = row.getBoolean( "enabled" ),
            created = row.delegate.getOffsetDateTime( "created" ),
            data = row.getJson( "data" ).value() as JsonObject
    )
}

fun createLogin( id : String, password : String ) : Single<Login> {
    val hashed = BCrypt.hashpw( password, BCrypt.gensalt())
    return PgClient( Db.pgPool )
            .rxPreparedQuery(
                    """insert into ${table("login")}
                        | ( id, password )
                        | values ($1,$2) returning
                        | id, password, enabled, created, data""".trimMargin(),
                    Tuple.of( id, hashed ) )
            .map { pgRowSet ->
                mapRowToLogin( pgRowSet.iterator().next() )
            }
}

fun getLogin( id: String ) : Maybe<Login> {
    return PgClient( Db.pgPool )
            .rxPreparedQuery(
                    """select
                        | id,
                        | password,
                        | enabled,
                        | created,
                        | data
                        | from ${table("login")}
                        |where id = $1""".trimMargin(),
                    Tuple.of( id ))
            .flatMapMaybe<Login> { pgRowSet ->
                if( pgRowSet.size() != 0 ) {
                    val row = pgRowSet.iterator().next()
                    Maybe.just( mapRowToLogin( row ) )
                }
                else {
                    Maybe.empty()
                }
            }
}

// TODO create a doLogin function to check pasword and update last login times in database
