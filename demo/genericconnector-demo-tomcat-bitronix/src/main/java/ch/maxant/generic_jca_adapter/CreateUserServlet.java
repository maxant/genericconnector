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
package ch.maxant.generic_jca_adapter;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;
import bitronix.tm.resource.ResourceLoader;
import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWriter;

/** call like this: http://localhost:8081/genericconnector-demo-tomcat-bitronix/createUser?username=john */
@WebServlet("/createUser")
@WebListener
public class CreateUserServlet extends HttpServlet implements ServletContextListener {
	
	private static final long serialVersionUID = 1L;
	private ResourceLoader rl;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		UserTransaction tx = null;
		try {
			final String username = request.getParameter("username");
			
			tx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
			tx.begin();
			
			String msResponse = null;
			{//inside transaction:
				Context ctx = new BitronixContext();
				BasicTransactionAssistanceFactory microserviceFactory = (BasicTransactionAssistanceFactory) ctx.lookup("xa/ms1");
				try(TransactionAssistant transactionAssistant = microserviceFactory.getTransactionAssistant()){
					msResponse = transactionAssistant.executeInActiveTransaction(txid->{
						final LetterWriter service = new LetterWebServiceService().getLetterWriterPort();
						return service.writeLetter(txid, username);
					});
				}
				
				runSql(username);
			}

			if(username.equals("john")){
				throw new RuntimeException("simulated error");
			}
			tx.commit();

			response.getWriter().write(msResponse);
			response.setContentType("text/plain");
		} catch (Exception e) {
			if(tx != null){
				try {
					tx.rollback();
				} catch (IllegalStateException | SecurityException | SystemException e1) {
					e1.printStackTrace();
				}
			}
			throw new ServletException(e);
		}

    }

    private static void runSql(String username) throws SQLException, NamingException {
		Context ctx = new BitronixContext();
		DataSource ds = (DataSource) ctx.lookup("jdbc/mysql1");
		try(Connection conn = ds.getConnection()){
			try(PreparedStatement stmt = conn.prepareStatement("insert into person(id, name) select max(id)+1, ? from person")){
				stmt.setString(1, username);
				stmt.executeUpdate();
			}
		}
    }

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		//dont really do this here - do it using a server lifecycle listener - im just doing it here to make this app portable. 
		//see serverl.xml =>   <Listener className="bitronix.tm.integration.tomcat55.BTMLifecycleListener"></Listener>
		TransactionManagerServices.getTransactionManager();

//TODO lookup resources using bitronix factory in normal initialContext, rather than in BitronixContext?
		
		//once per microservice that you want to use - do this when app starts, so that recovery can function immediately
		{
			final LetterWriter service = new LetterWebServiceService().getLetterWriterPort(); //take from pool if you want
			CommitRollbackHandler commitRollbackCallback = new CommitRollbackHandler() {
				@Override
				public void rollback(String txid) throws Exception {
					//compensate by cancelling the letter
					service.cancelLetter(txid);
				}
				@Override
				public void commit(String txid) throws Exception {
					//nothing to do, this service autocommits.
				}
			};
			BitronixTransactionConfigurator.setup("xa/ms1", commitRollbackCallback, 30000L, new File("."));
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		BitronixTransactionConfigurator.unregisterMicroserviceResourceFactory("xa/ms1");

		//dont do this here - see note in #contextInitialized
        TransactionManagerServices.getTransactionManager().shutdown();
	}
}
