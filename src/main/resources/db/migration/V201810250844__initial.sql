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
 A persona is the public name and address.
 Each persona must be unique system wide. Nearly everything
 else hangs off of a persona.
 */
create table persona (
  id text not null primary key,                         -- the fediverse handle, such as @foo
  display_name text,                                    -- the display name
  created timestamptz not null default now()            -- when the persona was created
);

/*
 This table ties logins and personas together.
 */
create table login_persona_link (
  login_id text not null references login( id ),
  persona_id text not null references persona( id ),
  owner boolean not null default true,               -- determines that the login id has persona ownership privledges
  primary key( login_id, persona_id )
);
