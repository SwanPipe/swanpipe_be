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

import com.github.kittinunf.result.Result
import com.swanpipe.daos.ActorLogin
import com.swanpipe.daos.ActorLoginDao
import com.swanpipe.utils.DaoConflict
import io.reactivex.Single
import io.vertx.core.json.JsonObject

object ActorLoginActions {

    const val OWNER = "owner"
    const val LOGINDATA = "loginData"
    const val ACTORDATA = "actorData"

    fun prepareNewActorLogin(actorLogin: JsonObject): JsonObject {
        actorLogin.getBoolean(OWNER) ?: kotlin.run {
            actorLogin.put(OWNER, true)
        }
        actorLogin.getString(ActorActions.PUN) ?: run {
            actorLogin.put(ActorActions.PUN, actorLogin.getString(LoginActions.ID))
        }
        return actorLogin
    }

    fun createActorLogin(actorLogin: JsonObject): Single<Result<ActorLogin,DaoConflict>> {
        prepareNewActorLogin( actorLogin )
        ActorActions.validateNewActor( actorLogin )
        LoginActions.validateNewLogin( actorLogin )
        LoginActions.prepareNewLogin( actorLogin )
        val actorPrep = ActorActions.prepareNewActor( actorLogin )
        return ActorLoginDao.createActorLoginTx(
            actorLogin.getString(LoginActions.ID),
            actorLogin.getString(LoginActions.PASSWORD),
            actorLogin.getJsonObject( LOGINDATA ),
            actorLogin.getString(ActorActions.PUN),
            actorLogin.getBoolean(OWNER),
            actorPrep.second,
            actorLogin.getJsonObject(ACTORDATA)
        )
    }

}