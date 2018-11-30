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
import com.swanpipe.daos.ConfigDao
import com.swanpipe.daos.LoginDao
import com.swanpipe.utils.appLogger
import io.reactivex.Observable
import io.reactivex.Single
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

const val STARTUP_ACCOUNTS = "startupAccounts"
const val ACTOR_LOGINS = "actorLogins"

fun doStartupAccounts( config: JsonObject ) : Single<Boolean> {
    val saConfig = config.getJsonObject( STARTUP_ACCOUNTS )
    if( saConfig != null ) {
        return Single.create{ emitter ->
            ConfigDao.getConfig( STARTUP_ACCOUNTS )
                .subscribe(
                    { config ->
                        if( config.data.getBoolean("create" )) {
                            createStartupActorLogins( saConfig )
                                .flatMap {
                                    ConfigDao.setConfig( STARTUP_ACCOUNTS, JsonObject().put( "create", false ) )
                                }
                                .subscribe(
                                    {
                                        emitter.onSuccess( true )
                                    },
                                    {
                                        emitter.onError( it )
                                    }
                                )
                        }
                        else {
                            appLogger.info { "Startup accounts create configuration set to false" }
                            emitter.onSuccess(false )
                        }
                    },
                    {
                        emitter.onError( it )
                    },
                    {
                        appLogger.info { "No startup accounts configuration." }
                        emitter.onSuccess(false )
                    }
                )
        }
    }
    else {
        return Single.just( false )
    }
}

fun createStartupActorLogins( saConfig: JsonObject) : Single<Boolean> {
    return Single.create { emitter ->
        if( saConfig.getJsonArray(ACTOR_LOGINS) != null ) {
            val actorLogins = saConfig.getJsonArray( ACTOR_LOGINS )
            Observable.fromIterable( actorLogins )
                .subscribe(
                    { actorLogin ->
                        ActorLoginActions.createActorLogin( actorLogin as JsonObject )
                            .subscribe { result : Triple<String,String,Boolean> ->
                                appLogger.info { "Login ${result.first} with actor ${result.second} created" }
                            }
                    },
                    {
                        emitter.onError( it )
                    },
                    {
                        emitter.onSuccess( true )
                    }
                )
        }
        else {
            emitter.onSuccess( false )
        }
    }
}

