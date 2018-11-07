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

package com.swanpipe.dao

import com.swanpipe.utils.Db
import com.swanpipe.utils.Db.table
import io.reactiverse.reactivex.pgclient.PgClient
import io.reactiverse.reactivex.pgclient.Tuple
import io.reactivex.Single
import io.vertx.core.buffer.Buffer

object ActorLoginDao {

    fun linkActorLogin( loginId: String, pun : String, owner: Boolean ) : Single<Triple<String, String, Boolean>> {
        return PgClient( Db.pgPool )
                .rxPreparedQuery(
                        """insert into ${table("login_actor_link")}
                        | ( login_id, pun, owner )
                        | values
                        | ( $1, $2, $3 )
                        | on conflict (login_id, pun)
                        | do update set owner = $3
                        | returning login_id, pun, owner
                    """.trimMargin(),
                        Tuple.of( loginId, pun, owner )
                )
                .map { pgRowSet ->
                    val row = pgRowSet.iterator().next()
                    Triple( row.getString( "login_id"),
                            row.getString( "pun" ),
                            row.getBoolean( "owner" ) )
                }
    }

    fun createActorLogin(
            loginId: String,
            password : String,
            pun: String,
            owner: Boolean,
            keypair: Pair<String, Buffer>
    ) : Single<Triple<String, String, Boolean>> {
        return PgClient(Db.pgPool).rxPreparedQuery(
                """
                with login_insert as (
                insert into ${table("login")}
                        ( id, password )
                        values ($1,$2)
                ),
                actor_insert as (
                insert into ${table("actor")}
                        ( pun, public_key_pem, private_key )
                        values ( $3, $4, $5 )
                )
                insert into ${table("login_actor_link")}
                        ( login_id, pun, owner )
                        values
                        ( $1, $3, $6 )
                returning login_id, pun, owner
            """.trimIndent(),
                Tuple.of( loginId, password, pun, keypair.first, keypair.second )
                        .addBoolean( owner )
        )
                .map { pgRowSet ->
                    val row = pgRowSet.iterator().next()
                    Triple(row.getString("login_id"),
                            row.getString("pun"),
                            row.getBoolean("owner"))
                }
    }

}

