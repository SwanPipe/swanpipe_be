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

import com.swanpipe.utils.appLogger
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.web.api.contract.RouterFactoryOptions
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.auth.jwt.JWTAuth
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.api.contract.openapi3.OpenAPI3RouterFactory
import java.time.OffsetDateTime


fun openApi3Router( vertx: Vertx, parent: Router ) {

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
    val jwt = JWTAuth.create( vertx, JWTAuthOptions( jwtAuthConfig ) )

    OpenAPI3RouterFactory.rxCreate( vertx, "spv1.yaml" )
        .subscribe { rf ->
            rf.addHandlerByOperationId( "login" ) { rc ->
                val data = rc.bodyAsJson
                appLogger.info { "loginId is ${data.getString( "loginId" )} and password is ${data.getString( "password" )}" }
                rc.response()
                    .end(
                        json { obj(
                            "token" to jwt.generateToken( JsonObject()
                                .put( "sub", "bob" )
                                .put( "iss", "swanpipe")
                                .put( "exp", OffsetDateTime.now().plusMonths( 1 ).toEpochSecond() )
                            )
                        ) }.encodePrettily()
                    )
            }
            val options = RouterFactoryOptions()
                .setMountResponseContentTypeHandler( true )
            val router = rf.setOptions( options ).getRouter()
            parent.mountSubRouter( "/spv1", router )
        }

}