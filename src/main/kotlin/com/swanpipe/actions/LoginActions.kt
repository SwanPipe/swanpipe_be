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
import com.swanpipe.daos.Login
import com.swanpipe.daos.LoginDao
import com.swanpipe.daos.LoginDao.getLogin
import com.swanpipe.daos.LoginDao.setLoginData
import com.swanpipe.utils.JsonValidator
import com.swanpipe.utils.ValidationException
import io.reactivex.Maybe
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import org.mindrot.jbcrypt.BCrypt
import java.time.OffsetDateTime

object LoginActions {

    val PASSWORD = "password"
    val ID = "id"
    val DATA = "data"
    val ROLES = "roles"
    val EMAIL = "email"

    enum class RolesEnum() {
        admin
    }

    fun prepareNewLogin(login: JsonObject): JsonObject {

        //bcrypt password
        val hashed = BCrypt.hashpw(login[PASSWORD], BCrypt.gensalt())
        login.put(PASSWORD, hashed)

        return login
    }


    fun validateNewLogin(login: JsonObject) {

        val validator = JsonValidator(login)

        validator.forProperty { it.getString(ID) } rules {
            length(min = 1, max = 100)
        } onError {
            errorMessage("Login ID must be between 1 and 100 characters")
        }

        validator.forProperty { it.getString(PASSWORD) } rules {
            length(min = 1, max = 100)
        } onError {
            errorMessage("password must be between 1 and 100 characters")
        }

        login.getJsonObject(DATA)?.let {

            validator.forProperty { it.getJsonObject(DATA)?.getString(EMAIL) } rules {
                email()
            } onError {
                errorMessage("email is not properly formatted")
            }

            validator.forProperty { it.getJsonObject(DATA)?.getJsonArray(ROLES) } rules {
                mustBe {
                    it?.let {
                        var found = false
                        for( r in it.list ) {
                            if( r is String && RolesEnum.values().map { it.name }.contains( r ) ) {
                                found = true
                                break
                            }
                        }
                        found
                    } ?: kotlin.run {
                        true
                    }
                }
            } onError {
                errorMessage("unknown role type in ${login.getJsonObject(DATA).getJsonArray(ROLES)}")
            }

        }

        ValidationException("new login validation").validate(validator)

    }

    fun createLogin(login: JsonObject): Maybe<Login> {
        return Maybe.just(login)
            .map {
                validateNewLogin(it)
                prepareNewLogin(it)
            }
            .flatMap {
                LoginDao.createLogin(it[ID], it[PASSWORD], it[DATA])
            }
    }

    /**
     * Gets a login, checks the password, and records the result.
     * The result will not have the lastSuccessfulLogin or lastFailedLogin in it.
     */
    fun checkLogin(id: String, password: String): Maybe<Login> {
        return getLogin(id)
            .flatMap { login ->
                val now = OffsetDateTime.now()
                if (!(BCrypt.checkpw(password, login.password) && login.enabled)) {
                    setLoginData(id, arrayOf("lastFailedLogin"), "$now")
                        .flatMapMaybe { Maybe.empty<Login>() }
                } else {
                    setLoginData(id, arrayOf("lastSuccessfulLogin"), "$now")
                        .flatMapMaybe { Maybe.just(login) }
                }
            }
    }
}
