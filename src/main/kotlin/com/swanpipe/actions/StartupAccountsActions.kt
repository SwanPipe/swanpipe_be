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

import com.swanpipe.daos.ActorLoginDao
import com.swanpipe.daos.ConfigDao
import com.swanpipe.utils.appLogger
import io.reactivex.Flowable
import io.reactivex.Single
import io.vertx.core.json.JsonObject

const val STARTUP_ACCOUNTS = "startupAccounts"
const val ACTOR_LOGINS = "actorLogins"
const val ACTORS = "actors"
const val LINKS = "links"

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
                                    createActors( saConfig )
                                }
                                .flatMap {
                                    createLinks( saConfig )
                                }
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
            Flowable.fromIterable( actorLogins )
                .flatMap { actorLogin ->
                    ActorLoginActions.createActorLogin( actorLogin as JsonObject ).toFlowable()
                }
                .map { result ->
                    result.fold(
                        {
                            val (login,actor) = it
                            appLogger.info { "Created login ${login.id} with actor ${actor.pun}" }
                        },
                        {
                            appLogger.error { "Conflict creating ${it.conflict}" }
                        }
                    )
                }
                .subscribe(
                    { },
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

fun createActors( saConfig: JsonObject ) : Single<Boolean> {
    return Single.create { emitter ->
        if( saConfig.getJsonArray( ACTORS ) != null ) {
            val actors = saConfig.getJsonArray( ACTORS )
            Flowable.fromIterable( actors )
                .flatMap { actor ->
                    ActorActions.createActor( actor as JsonObject ).toFlowable()
                }
                .map { result ->
                    appLogger.info { "created actor ${result.pun}" }
                }
                .subscribe(
                    { },
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

fun createLinks( saConfig: JsonObject ) : Single<Boolean> {
    return Single.create { emitter ->
        if( saConfig.getJsonArray( LINKS ) != null ) {
            val links = saConfig.getJsonArray( LINKS )
            Flowable.fromIterable( links )
                .flatMap { link ->
                    val loginId = (link as JsonObject).getString( "loginId" )
                    val pun = link.getString( "pun" )
                    val owner = link.getBoolean( "owner" )
                    ActorLoginDao.linkActorLogin( loginId, pun, owner ).toFlowable()
                }
                .map { result ->
                    appLogger.info { "Create link between login ${result.first} and actor ${result.second} as owner = ${result.third}" }
                }
                .subscribe(
                    { },
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

