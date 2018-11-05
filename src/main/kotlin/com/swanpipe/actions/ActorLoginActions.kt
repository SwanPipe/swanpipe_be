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

package com.swanpipe.actions

import com.swanpipe.dao.ActorLoginDao
import io.reactivex.Single
import io.vertx.core.json.JsonObject

object ActorLoginActions {

    val OWNER = "owner"

    fun createActorLogin(actorLogin: JsonObject ) : Single<Triple<String,String,Boolean>> {
        return Single.just( actorLogin )
                .map {
                    ActorActions.validateNewActor( actorLogin )
                    LoginActions.validateNewLogin( actorLogin )
                    ActorActions.prepareNewActor( actorLogin )
                    LoginActions.prepareNewLogin( actorLogin )
                }
                .flatMap {
                    val owner = it.getBoolean( OWNER )?: true
                    ActorLoginDao.createActorLogin(
                            it.getString( LoginActions.ID ),
                            it.getString( LoginActions.PASSWORD ),
                            it.getString( ActorActions.PUN ),
                            owner )
                }
    }

}