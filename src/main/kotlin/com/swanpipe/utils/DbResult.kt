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

package com.swanpipe.utils

/**
 * Wraps results from the database so the true runtime exceptions can be distinguished from
 * database constraint violations using a [Single] instead of a [Maybe].
 */
class DbResult<T> {

    var result : T? = null
    var conflict : String? = null

    constructor( c : String ) {
        conflict = c
    }

    constructor( r : T? ) {
        result = r
    }
}
