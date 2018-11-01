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
 Accounts simply tie together logins and personas.
 Each account can have multiple logins (a team of people
 under one account) and each account can have multiple
 personas (multiple personalities).
 */
create table account (
  id serial not null primary key,               -- account id
  enabled boolean not null default true,        -- is the account enabled
  created timestamptz not null default now()    -- when was it created
);

/*
 Logins allow a team of people to use one account.
 Each login id must be unique system wide.
 */
create table login (
  id text not null primary key,                          -- otherwise known as a login name or user name
  account_id integer not null references account( id ),  -- under which acocunt
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
  account_id integer not null references account( id ), -- which account does it belong to
  display_name text,                                    -- the display name
  created timestamptz not null default now()            -- when the persona was created
);
