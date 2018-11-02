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
import io.reactiverse.reactivex.pgclient.Row
import io.reactiverse.reactivex.pgclient.Tuple
import io.reactivex.Maybe
import io.reactivex.Single

fun createActor( name : String, displayName : String ) : Single<String> {
    val keypair = genRsa2048()
    return PgClient( Db.pgPool )
            .rxPreparedQuery(
                    """insert into ${table("actor")}
                        | ( name, display_name, public_key_pem, private_key )
                        | values ($1,$2,$3,$4) returning name""".trimMargin(),
                    Tuple.of( name, displayName, keypair.first, keypair.second ))
            .map { pgRowSet ->
                pgRowSet.iterator().next().getString( "name" )
            }
}

// TODO change this to a Pair, with A being JSON and B the private key byte array
fun getActor( name: String ) : Maybe<Row> {
    return PgClient( Db.pgPool )
            .rxPreparedQuery(
                    "select * from ${table("actor")} where name = $1",
                    Tuple.of( name ))
            .flatMapMaybe<Row> { pgRowSet ->
                if( pgRowSet.size() != 0 ) {
                    Maybe.just( pgRowSet.iterator().next() )
                }
                else {
                    Maybe.empty()
                }
            }
}
