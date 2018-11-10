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

import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.handler.ResponseContentTypeHandler

fun activityPubRouter( vertx: Vertx) : Router {
    val router = Router.router( vertx )
    // TODO test have activity pub router use responsecontexthandler
    router.route().handler( ResponseContentTypeHandler.create() )
    router.mountSubRouter( "/ap", actorRouter( vertx ) )
    return router
}
