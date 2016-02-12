package ch.maxant.jca_demo.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import ch.maxant.jca_demo.acquirer.Acquirer;
import ch.maxant.jca_demo.acquirer.AcquirerWebServiceService;
import ch.maxant.jca_demo.bookingsystem.BookingSystem;
import ch.maxant.jca_demo.bookingsystem.BookingSystemWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWebServiceService;
import ch.maxant.jca_demo.letterwriter.LetterWriter;

public final class IntegrationLayer {
	
	private IntegrationLayer() {}
	
    public static Acquirer getAcquirer() {
		//String baseUrl = "http://localhost:9081/genericconnector-demo-webservice-acquirer-2.1.1-SNAPSHOT/AcquirerWebService";
		String baseUrl = "http://a.maxant.ch/AcquirerWebService";
		try{
			URL url = new URL(baseUrl + "?wsdl");
			Acquirer svc = new AcquirerWebServiceService(url).getAcquirerPort();
			Map<String, Object> ctx = ((BindingProvider)svc).getRequestContext();
			ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, baseUrl);
			//ctx.put(BindingProvider.USERNAME_PROPERTY, "someUsername");
			//ctx.put(BindingProvider.PASSWORD_PROPERTY, "somePassword");
			return svc;
		}catch(MalformedURLException e){
			throw new RuntimeException("cant instantiate webservice client", e);
		}
	}

	public static BookingSystem getBookingsystem() {
		//String baseUrl = "http://localhost:9082/genericconnector-demo-webservice-bookingsystem-2.1.1-SNAPSHOT/BookingSystemWebService";
		String baseUrl = "http://b.maxant.ch/BookingSystemWebService";
		try{
			URL url = new URL(baseUrl + "?wsdl");
			BookingSystem svc = new BookingSystemWebServiceService(url).getBookingSystemPort();
			Map<String, Object> ctx = ((BindingProvider)svc).getRequestContext();
			ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, baseUrl);
			//ctx.put(BindingProvider.USERNAME_PROPERTY, "someUsername");
			//ctx.put(BindingProvider.PASSWORD_PROPERTY, "somePassword");
			return svc;
		}catch(MalformedURLException e){
			throw new RuntimeException("cant instantiate webservice client", e);
		}
	}

	public static LetterWriter getLetterwriter() {
		//String baseUrl = "http://localhost:9083/genericconnector-demo-webservice-letter-2.1.1-SNAPSHOT/LetterWebService";
		String baseUrl = "http://l.maxant.ch/LetterWebService";
		try{
			URL url = new URL(baseUrl + "?wsdl");
			LetterWriter svc = new LetterWebServiceService(url).getLetterWriterPort();
			Map<String, Object> ctx = ((BindingProvider)svc).getRequestContext();
			ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, baseUrl);
			//ctx.put(BindingProvider.USERNAME_PROPERTY, "someUsername");
			//ctx.put(BindingProvider.PASSWORD_PROPERTY, "somePassword");
			return svc;
		}catch(MalformedURLException e){
			throw new RuntimeException("cant instantiate webservice client", e);
		}
	}


}
