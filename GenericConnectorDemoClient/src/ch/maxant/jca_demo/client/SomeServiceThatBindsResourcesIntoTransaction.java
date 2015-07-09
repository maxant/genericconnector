/*
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

 */
package ch.maxant.jca_demo.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;

import ch.maxant.generic_jca_adapter.TransactionAssistanceFactory;
import ch.maxant.generic_jca_adapter.TransactionAssistant;
import ch.maxant.jca_demo.acquirer.Acquirer;
import ch.maxant.jca_demo.acquirer.AcquirerWebServiceService;
import ch.maxant.jca_demo.bookingsystem.BookingSystem;
import ch.maxant.jca_demo.bookingsystem.BookingSystemWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWriter;

/*
add to JBoss/standalone/configuration/server.xml:

        <subsystem xmlns="urn:jboss:domain:resource-adapters:2.0">
            <resource-adapters>
                <resource-adapter id="GenericConnector.rar">
                    <archive>
                        GenericConnectorDemoEar.ear#GenericConnector.rar
                    </archive>
                    <transaction-support>XATransaction</transaction-support>
                    <connection-definitions>
                        <connection-definition class-name="ch.maxant.generic_jca_adapter.ManagedTransactionAssistanceFactory" jndi-name="java:/maxant/Acquirer" pool-name="AcquirerPool">
                            <config-property name="id">
                                Acquirer
                            </config-property>
                            <xa-pool>
                                <min-pool-size>1</min-pool-size>
                                <max-pool-size>5</max-pool-size>
                            </xa-pool>
                            <recovery no-recovery="false">
                                <recover-credential>
                                    <user-name>asdf</user-name>
                                    <password>fdsa</password>
                                </recover-credential>
                            </recovery>
                        </connection-definition>
                        <connection-definition class-name="ch.maxant.generic_jca_adapter.ManagedTransactionAssistanceFactory" jndi-name="java:/maxant/BookingSystem" pool-name="BookingSystemPool">
                            <config-property name="recoveryStatePersistenceDirectory">
                                ../standalone/data/bookingsystem-tx-object-store
                            </config-property>
                            <config-property name="id">
                                BookingSystem
                            </config-property>
                            <config-property name="handleRecoveryInternally">
                                true
                            </config-property>
                            <xa-pool>
                                <min-pool-size>1</min-pool-size>
                                <max-pool-size>5</max-pool-size>
                            </xa-pool>
                            <recovery no-recovery="false">
                                <recover-credential>
                                    <user-name>asdf</user-name>
                                    <password>fdsa</password>
                                </recover-credential>
                            </recovery>
                        </connection-definition>
                        <connection-definition class-name="ch.maxant.generic_jca_adapter.ManagedTransactionAssistanceFactory" jndi-name="java:/maxant/LetterWriter" pool-name="LetterWriterPool">
                            <config-property name="recoveryStatePersistenceDirectory">
                                ../standalone/data/letterwriter-tx-object-store
                            </config-property>
                            <config-property name="id">
                                LetterWriter
                            </config-property>
                            <config-property name="handleRecoveryInternally">
                                true
                            </config-property>
                            <xa-pool>
                                <min-pool-size>1</min-pool-size>
                                <max-pool-size>5</max-pool-size>
                            </xa-pool>
                            <recovery no-recovery="false">
                                <recover-credential>
                                    <user-name>asdf</user-name>
                                    <password>fdsa</password>
                                </recover-credential>
                            </recovery>
                        </connection-definition>
                    </connection-definitions>
                </resource-adapter>
            </resource-adapters>
        </subsystem>



if necessary, here is an H2 XA config, which uses the same driver as the normal example H2 connection:

                <xa-datasource jndi-name="java:jboss/datasources/ExampleXADS" pool-name="ExampleXADS">
                   <driver>h2</driver>
                   <xa-datasource-property name="URL">jdbc:h2:tcp://localhost/~/test</xa-datasource-property>
                   <xa-pool>
                        <min-pool-size>10</min-pool-size>
                        <max-pool-size>20</max-pool-size>
                        <prefill>true</prefill>
                   </xa-pool>
                   <security>
                        <user-name>sa</user-name>
                        <password></password>
                   </security>
                </xa-datasource>


 */
@Stateless
@LocalBean
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SomeServiceThatBindsResourcesIntoTransaction {

    private final Logger log = Logger.getLogger(this.getClass().getName());

    @Resource(lookup = "java:/maxant/Acquirer")
    private TransactionAssistanceFactory acquirerFactory;

    @Resource(lookup = "java:/maxant/BookingSystem")
    private TransactionAssistanceFactory bookingFactory;

    @Resource(lookup = "java:/maxant/LetterWriter")
    private TransactionAssistanceFactory letterWriterFactory;
    
    @Resource(lookup = "java:/jdbc/MyXaDS")
//    @Resource(lookup = "java:jboss/datasources/ExampleXADS")
    private DataSource ds;

    @Resource
    private SessionContext ctx;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String doSomethingInvolvingSeveralResources(String refNumber)
            throws Exception {

        Acquirer acquirer = new AcquirerWebServiceService().getAcquirerPort();
        BookingSystem bookingSystem = new BookingSystemWebServiceService().getBookingSystemPort();
        LetterWriter letterWriter = new LetterWebServiceService().getLetterWriterPort();

        try (TransactionAssistant acquirerTransactionAssistant = acquirerFactory.getTransactionAssistant();
             TransactionAssistant bookingTransactionAssistant = bookingFactory.getTransactionAssistant();
             TransactionAssistant letterWriterTransactionAssistant = letterWriterFactory.getTransactionAssistant() 
        ) {
            int id = insertFirstRecordIntoDatabase();

            String acquirerResponse = acquirerTransactionAssistant
                    .executeInTransaction(txid -> {
                        return acquirer.reserveMoney(txid, refNumber);
                    });
            log.log(Level.INFO, "reserved money...");

            String bookingResponse = bookingTransactionAssistant.executeInTransaction(txid -> {
                return bookingSystem.reserveTickets(txid, refNumber);
            });
            
            String letterResponse = letterWriterTransactionAssistant.executeInTransaction(txid -> {
                return letterWriter.writeLetter(txid, refNumber);
            });
            
            insertAnotherRecordIntoDatabase(refNumber, id);

            String response = acquirerResponse + "/" + bookingResponse + "/" + letterResponse;
            log.log(Level.INFO, "returning " + response);
            return response;
        } catch (SQLException e) {
            // have to catch SQLException explicitly since
            // its considered to be an application exception
            // since it inherits from Exception and not
            // RuntimeException - kinda sucks really.
            ctx.setRollbackOnly();
            throw e;
        }
    }

    private void insertAnotherRecordIntoDatabase(String refNum, int id) throws SQLException {
        try (Connection database = ds.getConnection();
            PreparedStatement statement = database.prepareStatement("insert into temp.address values (null,?,?)")) {
            statement.setString(2, "THIRD_" + new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss.SSS").format(new Date()));
            if ("FAILDB".equals(refNum)) {
                statement.setInt(1, 0); // fails with FK Constraint exception
            } else {
                statement.setInt(1, id);
            }
            int cnt = statement.executeUpdate();
            log.log(Level.INFO, "wrote to db... " + cnt);
        }
    }

    // TESTS:
    // 1) no problem:
    // http://localhost:8080/GenericConnectorDemo/ResourceServlet
    // 2) fail db:
    // http://localhost:8080/GenericConnectorDemo/ResourceServlet?refNum=FAILDB
    // => check logs and db that no insert or sap update occurred
    // 3) fail ws:
    // http://localhost:8080/GenericConnectorDemo/ResourceServlet?refNum=FAILWSAcquirer
    // http://localhost:8080/GenericConnectorDemo/ResourceServlet?refNum=FAILWSBookingSystem
    // http://localhost:8080/GenericConnectorDemo/ResourceServlet?refNum=FAILWSLetterWriter
    // => check logs and db that no insert or sap update occurred
    // 4) disaster recovery: force NPE while at a breakpoint during
    // commit in web service. see if recovery occurs, to cleanup SAP =>
    // yes, a rollback occurs, since no resource has committed yet
    // 5) at same breakpoint, shut down DB. then continue with no error
    // => expect that db will be commit later. do we get a heuristic
    // exception?? yes: javax.transaction.HeuristicMixedException
    
    //mysql> delete from address;
    //mysql> delete from person;
    //mysql> select * from person p inner join address a on a.person_FK = p.id;

    // TODO redo tests where we kill something
    // TODO redo tests using request params
    // TODO redo tests documented above
    // TODO test killing server, so that recovery runs
    // TODO update jboss forum
    // TODO mavenise all and publish impl JAR to maven? => then you just need to package it to make it work
    // TODO ???
    
    private int insertFirstRecordIntoDatabase() throws SQLException {
        int id;
        try (Connection database = ds.getConnection();
            PreparedStatement statement = database.prepareStatement(
                "insert into temp.person values (null,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "FIRST_" + new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss.SSS").format(new Date()));
            int cnt = statement.executeUpdate();
            ResultSet generatedKeys = statement.getGeneratedKeys();
            generatedKeys.next();
            id = generatedKeys.getInt(1);
            log.log(Level.INFO, "wrote to db... " + cnt + " with ID " + id);
        }
        return id;
    }

}
