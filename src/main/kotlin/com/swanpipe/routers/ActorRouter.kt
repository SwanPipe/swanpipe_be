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

import com.swanpipe.daos.Actor
import com.swanpipe.daos.ActorDao
import com.swanpipe.utils.ACTIVITY_JSON_TYPE
import com.swanpipe.utils.HttpInfo.here
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext

fun actorRouter(vertx: Vertx): Router {
    val router = Router.router(vertx)

    router.get("/:pun")
        .produces(ACTIVITY_JSON_TYPE)
        .handler { rc ->

            val pun = rc.request().getParam("pun")
            ActorDao.getActor(pun)
                .subscribe(
                    { actor ->
                        rc.response()
                            .end(constructorActorAP(actor,rc).encodePrettily())
                    },
                    {
                        rc.response()
                            .setStatusCode(500)
                            .setStatusMessage("${it}")
                            .end()
                    },
                    {
                        rc.response()
                            .setStatusCode(404)
                            .setStatusMessage("actor ${pun} not found")
                            .end()
                    }
                )
        }

    // TODO we'll have to figure this out later.
    router.get("/:pun")
        .produces("text/html")
        .handler { rc ->

            val pun = rc.request().getParam("pun")
            ActorDao.getActor(pun)
                .subscribe(
                    { actor ->
                        rc.response()
                            .end("<html><body><h1>${actor.pun}</h1></body></html>")
                    },
                    {
                        rc.response()
                            .setStatusCode(500)
                            .setStatusMessage("${it}")
                            .end()
                    },
                    {
                        rc.response()
                            .setStatusCode(404)
                            .setStatusMessage("actor ${pun} not found")
                            .end()
                    }
                )
        }

    return router
}

fun constructorActorAP(actor: Actor, rc : RoutingContext ): JsonObject {
    return json {
        obj(
            "@context" to array(
                "https://www.w3.org/ns/activitystreams",
                "https://w3id.org/security/v1"
            ),
            "id" to "${here( rc )}/${actor.pun}",
            "type" to "Person",
            "preferredUserName" to actor.pun,
            "inbox" to "${here( rc )}/${actor.pun}/inbox",
            "publicKey" to obj(
                "id" to "${here( rc )}/${actor.pun}#main-key",
                "owner" to "${here( rc )}/${actor.pun}",
                "publicKeyPem" to actor.publicKeyPem
            )
        )
    }
}