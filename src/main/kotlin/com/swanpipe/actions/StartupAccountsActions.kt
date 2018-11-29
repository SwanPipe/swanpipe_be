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

import com.swanpipe.actions.LoginActions.ID
import com.swanpipe.daos.LoginDao
import io.reactivex.Observable
import io.reactivex.Single
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

const val STARTUP_ACCOUNTS = "startupAccounts"
const val ACTOR_LOGINS = "actorLogins"

fun createStartupActorLogins( config: JsonObject) : Single<String> {
    return Single.create { emitter ->
        if( config.getJsonObject( STARTUP_ACCOUNTS ) != null &&
            config.getJsonObject(STARTUP_ACCOUNTS).getJsonArray(ACTOR_LOGINS) != null ) {
            val actorLogins = config.getJsonObject( STARTUP_ACCOUNTS ).getJsonArray( ACTOR_LOGINS )
            existingLogins( actorLogins )
                .subscribe(
                    { loginIds ->
                        Observable.fromIterable( actorLogins )
                            .filter { i ->
                                if( i is JsonObject ) {
                                    !loginIds.contains(i.getString( ID ) )
                                }
                                else {
                                    throw java.lang.RuntimeException( "it is not JSON object. is ${i::class.java}")
                                }
                            }
                            .flatMap { login ->
                                ActorLoginActions.createActorLogin( login as JsonObject).toObservable()
                            }
                            .collect( { ArrayList<Triple<String,String,Boolean>>() }, { a,i -> a.add( i )} )
                            .subscribe(
                                {
                                    emitter.onSuccess( "${it.size} actor logins created" )
                                },
                                {
                                    emitter.onError( it )
                                }
                            )
                    },
                    {
                        throw( it )
                    }
                )
        }
        else {
            emitter.onSuccess( "no actor logins configured" )
        }
    }
}

fun existingLogins( logins : JsonArray) : Single<ArrayList<String>> {
    return Observable.fromIterable( logins )
        .flatMap { i ->
            if( i is JsonObject ) {
                LoginDao.getLogin( i.getString( ID ) ).toObservable()
            }
            else {
                throw java.lang.RuntimeException( "it is not JSON object. is ${i::class.java}")
            }

        }
        .collect(
            {
                ArrayList<String>()
            },
            { list, login ->
                list.add( login.id )
            }
        )
}
