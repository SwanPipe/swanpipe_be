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

import com.markodevcic.kvalidation.onError
import com.markodevcic.kvalidation.rules
import com.swanpipe.daos.Actor
import com.swanpipe.daos.ActorDao
import com.swanpipe.utils.JsonValidator
import com.swanpipe.utils.genRsa2048
import io.reactivex.Single
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import java.util.regex.Pattern


const val PUN_CHARS = "[^\\s:?#\\[\\]!$@&'()*+,;=/]*"


object ActorActions {

    val PUN = "pun"
    val DATA = "data"

    private val PUN_REGEX = Pattern.compile(PUN_CHARS)

    fun prepareNewActor(actor: JsonObject): Pair<JsonObject, Pair<String, Buffer>> {
        val keypair = genRsa2048()
        return Pair(actor, keypair)
    }

    fun validateNewActor(actor: JsonObject) {

        val validator = JsonValidator(actor)
        validator.forProperty { it.getString(PUN) } rules {
            length(min = 1, max = 15)
            pattern(PUN_REGEX)
        } onError {
            errorMessage("preferred user names (pun) cannot be more than 15 characters or contain spaces or special characters")
        }

    }

    fun createActor(actor: JsonObject): Single<Actor> {
        return Single.just(actor)
            .map {
                validateNewActor(actor)
                prepareNewActor(actor)
            }
            .flatMap {
                ActorDao.createActor(it.first.getString(PUN), it.second, it.first.getJsonObject(DATA))
            }
    }

}