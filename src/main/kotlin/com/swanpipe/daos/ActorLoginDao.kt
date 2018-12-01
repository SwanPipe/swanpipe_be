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

package com.swanpipe.daos

import com.swanpipe.utils.Db
import com.swanpipe.utils.Db.table
import io.reactiverse.pgclient.data.Json
import io.reactiverse.reactivex.pgclient.*
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.time.OffsetDateTime

data class ActorLink (
    val pun: String,
    val owner: Boolean
)

data class LoginActorLink(
    val id: String,
    val enabled: Boolean,
    val created: OffsetDateTime,
    val data : JsonObject,
    val actors: JsonArray
)

object ActorLoginDao {

    fun mapRowToLoginActorLink(row: Row): LoginActorLink {
        return LoginActorLink(
            id = row.getString("id"),
            enabled = row.getBoolean("enabled"),
            created = row.delegate.getOffsetDateTime("created"),
            data = row.getJson("data").value() as JsonObject,
            actors = row.getJson( "json_agg" ).value() as JsonArray
        )
    }

    fun linkActorLogin(loginId: String, pun: String, owner: Boolean): Single<Triple<String, String, Boolean>> {
        return linkActorLogin( loginId, pun, owner, PgClient( Db.pgPool ) )
    }

    fun linkActorLogin(loginId: String, pun: String, owner: Boolean, pg: PgClient): Single<Triple<String, String, Boolean>> {
        return pg
            .rxPreparedQuery(
                """insert into ${table("login_actor_link")}
                        | ( login_id, pun, owner )
                        | values
                        | ( $1, $2, $3 )
                        | on conflict (login_id, pun)
                        | do update set owner = $3
                        | returning login_id, pun, owner
                    """.trimMargin(),
                Tuple.of(loginId, pun, owner)
            )
            .map { pgRowSet ->
                val row = pgRowSet.iterator().next()
                Triple(
                    row.getString("login_id"),
                    row.getString("pun"),
                    row.getBoolean("owner")
                )
            }
    }

    fun createActorLogin(
        loginId: String,
        password: String,
        loginData: JsonObject?,
        pun: String,
        owner: Boolean,
        keypair: Pair<String, Buffer>,
        actorData: JsonObject?
    ): Single<Triple<String, String, Boolean>> {
        val ld = loginData?.let { loginData } ?:kotlin.run { JsonObject() }
        val ad = actorData?.let { actorData } ?:kotlin.run { JsonObject() }
        val tuple = Tuple.of(loginId, password, pun, keypair.first, keypair.second)
        tuple.addBoolean( owner )
        tuple.delegate.addJson( Json.create( ld ) )
        tuple.delegate.addJson( Json.create( ad ) )
        return PgClient(Db.pgPool).rxPreparedQuery(
            """
                with login_insert as (
                insert into ${table("login")}
                        ( id, password, data )
                        values ($1, $2, $7)
                ),
                actor_insert as (
                insert into ${table("actor")}
                        ( pun, public_key_pem, private_key, data )
                        values ( $3, $4, $5, $8 )
                )
                insert into ${table("login_actor_link")}
                        ( login_id, pun, owner )
                        values
                        ( $1, $3, $6 )
                returning login_id, pun, owner
            """.trimIndent(),
            tuple
        )
            .map { pgRowSet ->
                val row = pgRowSet.iterator().next()
                Triple(
                    row.getString("login_id"),
                    row.getString("pun"),
                    row.getBoolean("owner")
                )
            }
    }

/*    fun createActorLoginTx(
        loginId: String,
        password: String,
        loginData: JsonObject?,
        pun: String,
        owner: Boolean,
        keypair: Pair<String, Buffer>,
        actorData: JsonObject?
    ) : Single<Triple<String,String,Boolean>> {
        val pg = PgPool( Db.pgPool )
        return pg
            .rxBegin()
            .flatMapMaybe {
                LoginDao.createLogin( loginId, password, loginData, pg )
            }
            .flatMap {
                ActorDao.createActor( pun, keypair, actorData, pg )
            }
    }*/

    fun getLoginActorLink( id: String ) : Maybe<LoginActorLink> {
        return PgClient(Db.pgPool)
            .rxPreparedQuery(
                """
                    select
                        id,
                        enabled,
                        created,
                        data,
                        json_agg( sub )
                    from ${table("login")},
                        (
                            select
                                login_id as "loginId",
                                pun,
                                owner
                            from
                                ${table("login_actor_link")}
                            where
                                login_id = $1
                        ) sub
                    where
                        id=$1
                    group by login.id;
                """.trimIndent(),
                Tuple.of( id )
            ).flatMapMaybe<LoginActorLink> { pgRowSet ->
                if (pgRowSet.size() != 0) {
                    val row = pgRowSet.iterator().next()
                    Maybe.just(mapRowToLoginActorLink(row))
                } else {
                    Maybe.empty()
                }
            }
    }

}

