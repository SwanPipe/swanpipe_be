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
import io.reactiverse.reactivex.pgclient.PgClient
import io.reactiverse.reactivex.pgclient.Tuple
import io.reactivex.Single
import org.mindrot.jbcrypt.BCrypt

fun linkActorLogin( loginId: String, actorName : String, owner: Boolean ) : Single<Triple<String, String, Boolean>> {
    return PgClient( Db.pgPool )
            .rxPreparedQuery(
                    """insert into ${table("login_actor_link")}
                        | ( login_id, actor_name, owner )
                        | values
                        | ( $1, $2, $3 )
                        | on conflict (login_id, actor_name)
                        | do update set owner = $3
                        | returning login_id, actor_name, owner
                    """.trimMargin(),
                    Tuple.of( loginId, actorName, owner )
            )
            .map { pgRowSet ->
                val row = pgRowSet.iterator().next()
                Triple( row.getString( "login_id"),
                        row.getString( "actor_name" ),
                        row.getBoolean( "owner" ) )
            }
}

fun createActorLogin(
        loginId: String,
        password : String,
        actorName: String,
        displayName : String,
        owner: Boolean
) : Single<Triple<String, String, Boolean>> {
    val keypair = genRsa2048()
    val hashed = BCrypt.hashpw( password, BCrypt.gensalt())
    return PgClient(Db.pgPool).rxPreparedQuery(
            """
                with login_insert as (
                insert into ${table("login")}
                        ( id, password )
                        values ($1,$2)
                ),
                actor_insert as (
                insert into ${table("actor")}
                        ( name, display_name, public_key_pem, private_key )
                        values ( $3, $4, $5, $6 ) returning
                        name, display_name, created, public_key_pem, private_key
                )
                insert into ${table("login_actor_link")}
                        ( login_id, actor_name, owner )
                        values
                        ( $1, $3, $7 )
                returning login_id, actor_name, owner
            """.trimIndent(),
                Tuple.of( loginId, hashed, actorName, displayName, keypair.first, keypair.second )
                        .addBoolean( owner )
            )
            .map { pgRowSet ->
                val row = pgRowSet.iterator().next()
                Triple(row.getString("login_id"),
                        row.getString("actor_name"),
                        row.getBoolean("owner"))
            }
}
