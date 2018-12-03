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

package com.swanpipe.routers.Spv1Handlers

import com.swanpipe.actions.ActorLoginActions.LOGINDATA
import com.swanpipe.actions.ActorLoginActions.TOKEN
import com.swanpipe.actions.ActorLoginActions.createActorLoginWithToken
import com.swanpipe.actions.LoginActions.EMAIL
import com.swanpipe.daos.ConfigDao
import com.swanpipe.daos.SIGNUP_CONFIG_ID
import com.swanpipe.routers.verifyNbfToken
import com.swanpipe.utils.appLogger
import io.reactivex.Single
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.ext.auth.jwt.JWTAuth
import io.vertx.reactivex.ext.web.RoutingContext

const val CONFIRMATION_REQUIRED = "confirmationRequired"
const val ALLOW_OPEN_REGISTRATION = "allowOpenRegistration"

fun signupHandler(jwt: JWTAuth) : (RoutingContext) -> Unit {
    return { rc ->
        ConfigDao.getConfig(SIGNUP_CONFIG_ID)
            .subscribe(
                { config ->
                    val openRegistration = config.data.getBoolean( ALLOW_OPEN_REGISTRATION )
                    val json = rc.bodyAsJson
                    if( openRegistration ) {
                        isSignupTokenValid( json, jwt )
                            .subscribe { b ->
                                if( b ) {
                                    val email = json.getString( EMAIL )
                                    val loginData = JsonObject().put( EMAIL, email )
                                    json.put( LOGINDATA, loginData )
                                    createActorLoginWithToken( json )
                                        .subscribe(
                                            { result ->
                                                result.fold(
                                                    { _ ->
                                                        val response = json { obj(
                                                            CONFIRMATION_REQUIRED to config.data.getBoolean( CONFIRMATION_REQUIRED ),
                                                            EMAIL to email
                                                        )}
                                                        //TODO actually send some email too
                                                        rc.response().setStatusCode(201).end( response.encodePrettily() )
                                                    },
                                                    { conflict ->
                                                        val response = json { obj(
                                                            "conflict" to conflict.conflict
                                                        ) }
                                                        rc.response().setStatusCode(409).end( response.encodePrettily() )
                                                    }
                                                )
                                            },
                                            { e ->
                                                appLogger.error { "action=signup result=error : ${e}" }
                                                e.printStackTrace()
                                                rc.response().setStatusCode(500).end()
                                            }
                                        )
                                }
                                else {
                                    appLogger.info { "action=signup result=failed : invalid token" }
                                    rc.response().setStatusCode(401).end()
                                }
                            }
                    }
                    else {
                        appLogger.info { "action=signup result=failed : open registration is closed" }
                        rc.response().setStatusCode(401).end()
                    }
                },
                { e->
                    appLogger.error { "action=signup result=error : ${e}" }
                    e.printStackTrace()
                    rc.response().setStatusCode(500).end()
                },
                {
                    appLogger.error { "action=signup result=failed no signup configuration" }
                    rc.response().setStatusCode(500).end()
                }
            )
    }
}

fun isSignupTokenValid( json: JsonObject, jwt: JWTAuth ) : Single<Boolean> {
    val token: String? = json.getString( TOKEN )
    token?.let {
        return verifyNbfToken( jwt, token )
            .flatMap {
                Single.just( true )
            }
    }
    ?: run {
        return Single.just( false )
    }
}