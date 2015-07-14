#genericconnector demo

This folder contains a demo application for trying out and testing the generic resource adapter.  

See genericconnector-demo-client/README.md for more details.

Build in Maven starting from genericconnector-demo-parent

##Database Setup
In order to run this demo, you need a database with the following DDL.

###MySQL

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
    
###JBoss XA Database configuration
For example:

        <subsystem xmlns="urn:jboss:domain:datasources:2.0">
            <datasources>
                <xa-datasource jndi-name="java:/jdbc/MyXaDS" pool-name="MyXaDSPool" enabled="true" use-ccm="false">
                    <xa-datasource-property name="URL">
                        jdbc:mysql://localhost:3306/temp?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=UTF-8
                    </xa-datasource-property>
                    <xa-datasource-class>com.mysql.jdbc.jdbc2.optional.MysqlXADataSource</xa-datasource-class>
                    <driver>mysql</driver>
                    <xa-pool>
                        <min-pool-size>10</min-pool-size>
                        <max-pool-size>20</max-pool-size>
                        <is-same-rm-override>false</is-same-rm-override>
                        <interleaving>false</interleaving>
                        <pad-xid>false</pad-xid>
                        <wrap-xa-resource>false</wrap-xa-resource>
                    </xa-pool>
                    <security>
                        <user-name>tom</user-name>
                        <password>j0nes</password>
                    </security>
                    <validation>
                        <validate-on-match>false</validate-on-match>
                        <background-validation>false</background-validation>
                        <background-validation-millis>1000</background-validation-millis>
                    </validation>
                    <statement>
                        <prepared-statement-cache-size>0</prepared-statement-cache-size>
                        <share-prepared-statements>false</share-prepared-statements>
                    </statement>
                </xa-datasource>
                <drivers>
                    <driver name="h2" module="com.h2database.h2">
                        <xa-datasource-class>org.h2.jdbcx.JdbcDataSource</xa-datasource-class>
                    </driver>
                </drivers>
            </datasources>
        </subsystem>
