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
import io.reactiverse.reactivex.pgclient.PgClient
import io.reactiverse.reactivex.pgclient.Row
import io.reactiverse.reactivex.pgclient.Tuple
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import java.time.OffsetDateTime

data class Actor(
    val pun: String,
    val created: OffsetDateTime,
    val publicKeyPem: String,
    val privateKey: Buffer,
    val data: JsonObject
)

object ActorDao {

    fun mapRowToActor(row: Row): Actor {
        return Actor(
            pun = row.getString("pun"),
            created = row.delegate.getOffsetDateTime("created"),
            publicKeyPem = row.getString("public_key_pem"),
            privateKey = row.delegate.getBuffer("private_key"),
            data = row.getJson("data").value() as JsonObject
        )
    }

    fun createActor(pun: String, keypair: Pair<String, Buffer>, data: JsonObject?): Single<Actor> {
        val actorData = data?.let { data }?:run { JsonObject() }
        return PgClient(Db.pgPool)
            .rxPreparedQuery(
                """insert into ${table("actor")}
                        | ( pun, public_key_pem, private_key, data )
                        | values ($1,$2,$3,$4) returning
                        | pun, created, public_key_pem, private_key, data""".trimMargin(),
                Tuple.of(pun, keypair.first, keypair.second, Json.create( actorData ))
            )
            .map { pgRowSet ->
                mapRowToActor(pgRowSet.iterator().next())
            }
    }

    fun getActor(pun: String): Maybe<Actor> {
        return PgClient(Db.pgPool)
            .rxPreparedQuery(
                """select
                        | pun,
                        | created,
                        | public_key_pem,
                        | private_key,
                        | data
                        | from ${table("actor")}
                        |where pun = $1""".trimMargin(),
                Tuple.of(pun)
            )
            .flatMapMaybe<Actor> { pgRowSet ->
                if (pgRowSet.size() != 0) {
                    val row = pgRowSet.iterator().next()
                    Maybe.just(mapRowToActor(row))
                } else {
                    Maybe.empty()
                }
            }
    }

    fun setActorData(pun: String, path: Array<String>, data: Any): Single<JsonObject> {
        return PgClient(Db.pgPool)
            .rxPreparedQuery(
                """
                        update ${table("actor")}
                        set data = jsonb_set( data, $2, $3::jsonb )
                        where pun = $1
                        returning data
                    """.trimIndent(),
                Tuple.of(pun, path, data)
            )
            .flatMap { pgRowSet ->
                Single.just(pgRowSet.iterator().next().getJson("data").value() as JsonObject)
            }
    }

}

