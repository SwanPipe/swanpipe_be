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
import com.swanpipe.dao.Login
import com.swanpipe.dao.LoginDao
import com.swanpipe.utils.JsonValidator
import com.swanpipe.utils.ValidationException
import io.reactivex.Single
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import org.mindrot.jbcrypt.BCrypt

object LoginActions {

    private val PASSWORD = "password"
    private val ID = "id"

    fun prepareNewLogin(login : JsonObject) : JsonObject {
        val hashed = BCrypt.hashpw( login[PASSWORD], BCrypt.gensalt() )
        login.put(PASSWORD, hashed )
        return login
    }


    fun validateNewLogin(login: JsonObject ) {

        val validator = JsonValidator( login )
        validator.forProperty { it.getString( ID ) } rules {
            length( min = 1, max = 100 )
        } onError {
            errorMessage( "Login ID must be between 1 and 100 characters")
        }
        validator.forProperty { it.getString( PASSWORD ) } rules {
            length( min = 1, max = 100 )
        } onError {
            errorMessage( "password must be between 1 and 100 characters")
        }

        ValidationException( "new login validatio" ).validate( validator )

    }

    fun createLogin( login : JsonObject ) : Single<Login> {
        return Single.just( login )
                .map {
                    validateNewLogin( it )
                    prepareNewLogin( it )
                }
                .flatMap {
                    LoginDao.createLogin( it[ID], it[PASSWORD] )
                }
    }

}
