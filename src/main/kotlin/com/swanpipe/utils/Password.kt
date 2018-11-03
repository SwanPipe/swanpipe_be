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

const val SCRYPT_COST = 16384             // N
const val SCRYPT_BLOCK_SIZE = 8           // r
const val SCRYPT_PARALLELIZATION = 1      // p

/*

 For now we don't need to provide any utility methods beyond what is provided in Scrypt itself.
 In the future, when support for argon2 or another scheme is added, methods will be added here
 to autodetect versions and do the appropriate thing.  But that is the future.

 From the lambdaworks docs:

  com.lambdaworks.crypto.SCryptUtil implements a modified version of MCF,
  the modular crypt format, similar to the one used for storage of Unix
  passwords in the MD5, SHA-256, and bcrypt formats.

    SCryptUtil.scrypt(passwd, N, r, p)
    SCryptUtil.check(passwd, hashed)

 */