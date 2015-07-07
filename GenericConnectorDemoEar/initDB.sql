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


