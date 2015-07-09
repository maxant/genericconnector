--   Copyright 2015 Ant Kutschera
--
--   Licensed under the Apache License, Version 2.0 (the "License");
--   you may not use this file except in compliance with the License.
--   You may obtain a copy of the License at
--
--       http://www.apache.org/licenses/LICENSE-2.0
--
--   Unless required by applicable law or agreed to in writing, software
--   distributed under the License is distributed on an "AS IS" BASIS,
--   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--   See the License for the specific language governing permissions and
--   limitations under the License.

use temp;

drop table if exists address;
drop table if exists person;

create table person (
	id integer not null auto_increment,
    name varchar(30) not null,
    primary key (id)
) engine = innodb;

create table address (
	id integer not null auto_increment,
	person_fk integer not null,
    street varchar(30) not null,
    primary key (id),
    foreign key (person_fk) references person(id)
) engine = innodb;


