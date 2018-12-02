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
import io.reactiverse.reactivex.pgclient.PgClient
import io.reactiverse.reactivex.pgclient.PgRowSet
import io.reactiverse.reactivex.pgclient.Tuple
import io.reactivex.Maybe
import io.reactivex.Single

object LoginTokenDao {

    fun createLoginToken( loginId: String, token: String ) : Maybe<String> {
        return createLoginToken( loginId, token, PgClient( Db.pgPool ) )
            .flatMapMaybe<String> { pgRowSet ->
                if( pgRowSet.size() != 0 ) {
                    Maybe.just( pgRowSet.iterator().next().getString( "login_id" ) )
                }
                else {
                    Maybe.empty()
                }
            }
    }

    fun createLoginToken( loginId: String, token: String, pg : PgClient ) : Single<PgRowSet> {
        return pg
            .rxPreparedQuery(
                """
                    insert into ${table("login_token")}
                    (login_id, token )
                    values
                    ( $1, $2 )
                    on conflict do nothing
                    returning
                    login_id
                """.trimIndent(),
                Tuple.of( loginId, token )
            )
    }

}