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

/*
 Logins allow a team of people to act as one persona.
 Each login id must be unique system wide.
 */
create table login (
  id text not null primary key,                          -- otherwise known as a login name or user name
  password text not null,
  enabled boolean not null default true,
  created timestamptz not null default now(),
  last_successful_login timestamptz,
  last_failed_login timestamptz
);

/*
 An actor is the public name and address.
 Each actor must be unique system wide.
 */
create table actor (
  name text not null primary key,                       -- the fediverse handle, such as @foo
  display_name text,                                    -- the display name
  created timestamptz not null default now(),           -- when the persona was created
  public_key_pem text not null,                         -- actors public key in PEM format
  private_key bytea not null                            -- actors private key in binary
);

/*
 This table ties logins and actors together.
 */
create table login_actor_link (
  login_id text not null references login( id ),
  actor_name text not null references actor( name ),
  owner boolean not null default true,               -- determines that the login id has actor ownership privledges
  primary key( login_id, actor_name )
);