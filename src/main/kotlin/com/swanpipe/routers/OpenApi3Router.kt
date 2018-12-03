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

package com.swanpipe.routers

import com.swanpipe.routers.Spv1Handlers.accountInfoHandler
import com.swanpipe.routers.Spv1Handlers.loginAccountHandler
import com.swanpipe.routers.Spv1Handlers.loginHandler
import com.swanpipe.routers.Spv1Handlers.signupTokenHandler
import com.swanpipe.utils.AUTHORIZATION_HEADER
import com.swanpipe.utils.appLogger
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.web.api.contract.RouterFactoryOptions
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.auth.User
import io.vertx.reactivex.ext.auth.jwt.JWTAuth
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.api.contract.openapi3.OpenAPI3RouterFactory


fun openApi3Router(vertx: Vertx, parent: Router) {

    val jwtAuthConfig = json {
        obj(
            "keyStore" to
                    obj(
                        "type" to "jceks",
                        "path" to "jwt.jceks",
                        "password" to "secret"
                    )
        )
    }
    val jwt = JWTAuth.create(vertx, JWTAuthOptions(jwtAuthConfig))

    OpenAPI3RouterFactory.rxCreate(vertx, "spv1.yaml")
        .subscribe { rf ->

            rf.addHandlerByOperationId("login", loginHandler(jwt))
            rf.addHandlerByOperationId("loginAccount", loginAccountHandler())
            rf.addHandlerByOperationId("accountInfo", accountInfoHandler())
            rf.addHandlerByOperationId("signupToken", signupTokenHandler(jwt))

            val options = RouterFactoryOptions()
                .setMountResponseContentTypeHandler(true)
            val router = rf.setOptions(options).getRouter()

            /*
            This handler checks to see if there is a JWT in the authorization header and if so, attempts to
            decode it and place the user in the router context
             */
            router.route().order(0).handler { rc ->
                val ah = rc.request().getHeader(AUTHORIZATION_HEADER )
                ah?.let {
                    jwt.rxAuthenticate(json { obj("jwt" to ah.substring(7)) })
                        .subscribe(
                            { user ->
                                rc.setUser(user)
                                rc.next()
                            },
                            {
                                appLogger.info { "jwt authentication did not authenticate" }
                                rc.next()
                            }
                        )
                } ?: run {
                    rc.next()
                }
            }

            parent.mountSubRouter("/spv1", router)
        }

}


fun verifyNbfToken( jwt: JWTAuth, token: String  ) : Single<User> {
    return jwt.rxAuthenticate( json { obj( "jwt" to token ) } )
}

fun <T> handlerMaybe(
    actionName: String,
    action: (RoutingContext) -> Maybe<T>,
    responder: (RoutingContext, T) -> Unit,
    authenticate: Boolean
):
            (RoutingContext) -> Unit {
    return { rc ->
        if (authenticate && rc.user() == null) {
            rc.response().setStatusCode(401).end()
        } else {
            action(rc)
                .subscribe(
                    {
                        responder(rc, it)
                    },
                    { e ->
                        appLogger.error { "action=${actionName} loginId=${loginId(rc)} result=error : ${e}" }
                        rc.response().setStatusCode(500).end()
                    },
                    {
                        appLogger.info { "action=${actionName} loginId=${loginId(rc)} result=failed" }
                        rc.response().setStatusCode(404).end()
                    }
                )
        }
    }
}

fun <T> handlerSingle(
    actionName: String,
    action: (RoutingContext) -> Single<T>,
    responder: (RoutingContext, T) -> Unit,
    authenticate: Boolean
    ) :
        (RoutingContext) -> Unit {
    return { rc ->
        if( authenticate && rc.user() == null ) {
            rc.response().setStatusCode( 401 ).end()
        }
        else {
            action( rc )
                .subscribe(
                    {
                        responder( rc, it )
                    },
                    { e ->
                        appLogger.error { "action=${actionName} loginId=${loginId(rc)} result=error : ${e}" }
                        rc.response().setStatusCode(500).end()
                    }
                )
        }
    }

}

fun loginId( rc : RoutingContext ) : String {
    return rc.user().principal().getString( "sub" )
}

fun sourceIp( rc: RoutingContext ) : String {
    val xheader : String? = rc.request().getHeader( "X-Forwarded-For")
    xheader?.let {
        val hosts = it.split( ',' )
        if( !hosts.isEmpty() ) {
            if( !hosts[0].isBlank() ) {
                return hosts[0].trim()
            }
            else {
                return rc.request().remoteAddress().host()
            }
        }
        else {
            return rc.request().remoteAddress().host()
        }
    }
    ?: run {
        return rc.request().remoteAddress().host()
    }
}