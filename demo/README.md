#genericconnector Demos

This folder contains demo applications for trying out and testing the generic connector.  

- `genericconnector-demo-common` - contains generated code for calling back end services
- `genericconnector-demo-javaee-client` - a mini Java EE application which calls three web services (see below) and writes to a local database. It demonstrates how to integrate the generic adapter in a full on Java EE environment.
- `genericconnector-demo-javaee-ear` - The assembly for the Java EE application above. Puts the resource archive (RAR) and the client web application above into an EAR which you can deploy to your application server. Please ensure the app server is correctly configured - see below for the database and see ../connector/README.md for the server JCA adapter configuration.
- `genericconnector-demo-parent` - A Maven project for building all demo applications in one step.
- `genericconnector-demo-springboot-atomikos` - A Spring Boot demo application showing how to integrate the generic connector when you use the Atomikos transaction manager.
- `genericconnector-demo-springboot-bitronix` - A Spring Boot demo application showing how to integrate the generic connector when you use the Bitronix transaction manager. 
- `genericconnector-demo-springboot-common` -  Code common to both the Atomikos and Bitronix based Spring Boot demo applications above.
- `genericconnector-demo-standalone-atomikos` - A simple standalone Java application which integrates the generic connector using the Atomikos transaction manager. This is here to show you how it all works, but it is not recommended you write applications like this - use Java EE, Spring or Spring Boot instead.
- `genericconnector-demo-standalone-bitronix` - A simple standalone Java application which integrates the generic connector using the Bitronix transaction manager. This is here to show you how it all works, but it is not recommended you write applications like this - use Java EE, Spring or Spring Boot instead.
- `genericconnector-demo-tomcat-atomikos` - A simple Tomcat application which integrates the generic connector using the Atomikos transaction manager. So that this application can be deployed without additional Tomcat configuration, it contains "illegal" code which is annotated with comments in the code. Basically it configures the transaction manager once per application rather than once per server.
- `genericconnector-demo-tomcat-bitronix` - A simple Tomcat application which integrates the generic connector using the Bitronix transaction manager. So that this application can be deployed without additional Tomcat configuration, it contains "illegal" code which is annotated with comments in the code. Basically it configures the transaction manager once per application rather than once per server.

Additionally it contains some applications which must be deployed to a server so that these demo applications work:

- `genericconnector-demo-webservice-acquirer` - A demo web service which must be deployed to `http://localhost:8080/genericconnector-demo-webservice-acquirer/AcquirerWebService` in order for the demos to work
- `genericconnector-demo-webservice-bookingsystem` - A demo web service which must be deployed to `http://localhost:8080/genericconnector-demo-webservice-bookingsystem/BookingSystemWebService` in order for the demos to work
- `genericconnector-demo-webservice-letter` - A demo web service which must be deployed to `http://localhost:8080/genericconnector-demo-webservice-letter/LetterWebService` in order for the demos to work

See genericconnector-demo-javaee-client/README.md for more details.

Build in Maven starting from genericconnector-demo-parent

##Server Configuration
See ../connector/README.md for details.

##Database Setup
In order to run these demos, you need a database with the following DDL, located at localhost in a schema/user called "temp". The configurations depend on mysql with user/password root/password. Of course you can change this for any database with an XA compliant JDBC driver.

###MySQL 5.x

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
                        <user-name>root</user-name>
                        <password>password</password>
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
                    <driver name="mysql" module="com.mysql">
                        <xa-datasource-class>com.mysql.jdbc.jdbc2.optional.MysqlXADataSource</xa-datasource-class>
                    </driver>
                </drivers>
            </datasources>
        </subsystem>

##License

 Copyright 2015 Ant Kutschera

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
