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

fun createPersona( id : String, displayName : String ) : Single<String> {
    return PgClient( Db.pgPool )
            .rxPreparedQuery(
                    "insert into ${table("persona")} ( id, display_name ) values ($1,$2) returning id",
                    Tuple.of( id, displayName ))
            .map { pgRowSet ->
                pgRowSet.iterator().next().getString( "id" )
            }
}

// TODO change this to a Pair, with A being JSON and B the private key byte array
fun getPersona( id: String ) : Maybe<Row> {
    return PgClient( Db.pgPool )
            .rxPreparedQuery(
                    "select * from ${table("persona")} where id = $1",
                    Tuple.of( id ))
            .flatMapMaybe<Row> { pgRowSet ->
                if( pgRowSet.size() != 0 ) {
                    Maybe.just( pgRowSet.iterator().next() )
                }
                else {
                    Maybe.empty()
                }
            }
}
