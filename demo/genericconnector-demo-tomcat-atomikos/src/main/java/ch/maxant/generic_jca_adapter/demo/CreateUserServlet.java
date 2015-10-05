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
package ch.maxant.generic_jca_adapter.demo;

import java.io.File;
import java.io.IOException;

import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.atomikos.icatch.jta.UserTransactionManager;

import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactory;
import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactoryImpl;
import ch.maxant.generic_jca_adapter.CommitRollbackCallback;
import ch.maxant.generic_jca_adapter.MicroserviceXAResource;
import ch.maxant.generic_jca_adapter.TransactionAssistant;
import ch.maxant.generic_jca_adapter.TransactionConfigurator;
import ch.maxant.jca_demo.bookingsystem.BookingSystem;
import ch.maxant.jca_demo.bookingsystem.BookingSystemWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWriter;

/** call like this: http://localhost:8081/genericconnector-demo-tomcat-atomikos/sale?account=john */
@WebServlet("/sale")
@WebListener
public class CreateUserServlet extends HttpServlet implements ServletContextListener {
	
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		UserTransaction tx = null;
		try {
			final String account = request.getParameter("account");
			
			tx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
			tx.begin();
			
			String msResponse = null;
			{//inside transaction:
				
				//CALL BOOKING SERVICE
				BasicTransactionAssistanceFactory bookingSystemFactory = new BasicTransactionAssistanceFactoryImpl("xa/bookingSystem");
				try(TransactionAssistant transactionAssistant = bookingSystemFactory.getTransactionAssistant()){
					msResponse = transactionAssistant.executeInActiveTransaction(txid->{
						final BookingSystem service = new BookingSystemWebServiceService().getBookingSystemPort();
						return service.reserveTickets(txid, account);
					});
				}

				//SEND LETTER
				BasicTransactionAssistanceFactory letterWriterFactory = new BasicTransactionAssistanceFactoryImpl("xa/letterWriter");
				try(TransactionAssistant transactionAssistant = letterWriterFactory.getTransactionAssistant()){
					msResponse += "/" + transactionAssistant.executeInActiveTransaction(txid->{
						final LetterWriter service = new LetterWebServiceService().getLetterWriterPort();
						return service.writeLetter(txid, account);
					});
				}
			}

			if(account.equals("john")){
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

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		//dont really do this here - do it using a server lifecycle listener - im just doing it here to make this app portable. 
		//see http://www.atomikos.com/Documentation/Tomcat7Integration35
		{
			//example of configuring the transaction assistance component:
			MicroserviceXAResource.configure(30000L, new File("."));
			
			//initialise TM
			UserTransactionManager utm = new UserTransactionManager();
			try {
				utm.init();
			} catch (SystemException e) {
				throw new RuntimeException(e);
			}
		} //end "dont do here"
		
		//once per microservice that you want to use - do this when app starts, so that recovery can function immediately
		{
			CommitRollbackCallback commitRollbackCallback = new CommitRollbackCallback() {
				private static final long serialVersionUID = 1L;
				@Override
				public void rollback(String txid) throws Exception {
					//compensate by cancelling the letter
					new LetterWebServiceService().getLetterWriterPort().cancelLetter(txid);
				}
				@Override
				public void commit(String txid) throws Exception {
					//nothing to do, this service autocommits.
				}
			};
			TransactionConfigurator.setup("xa/letterWriter", commitRollbackCallback);

		}
		{
			CommitRollbackCallback commitRollbackCallback = new CommitRollbackCallback() {
				private static final long serialVersionUID = 1L;
				@Override
				public void rollback(String txid) throws Exception {
					getService().cancelTickets(txid);
				}
				@Override
				public void commit(String txid) throws Exception {
					getService().bookTickets(txid);
				}
				private BookingSystem getService() {
					return new BookingSystemWebServiceService().getBookingSystemPort();
				}
			};
			TransactionConfigurator.setup("xa/bookingSystem", commitRollbackCallback);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		TransactionConfigurator.unregisterMicroserviceResourceFactory("xa/bookingSystem");
		TransactionConfigurator.unregisterMicroserviceResourceFactory("xa/letterWriter");

		//dont do this here - see note in #contextInitialized
		new UserTransactionManager().close();
	}
}
