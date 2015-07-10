
package ch.maxant.jca_demo.client.generated.bookingsystem;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the ch.maxant.jca_demo.bookingsystem package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ReserveTickets_QNAME = new QName("http://bookingsystem.jca_demo.maxant.ch/", "reserveTickets");
    private final static QName _IOException_QNAME = new QName("http://bookingsystem.jca_demo.maxant.ch/", "IOException");
    private final static QName _ReserveTicketsResponse_QNAME = new QName("http://bookingsystem.jca_demo.maxant.ch/", "reserveTicketsResponse");
    private final static QName _CancelTickets_QNAME = new QName("http://bookingsystem.jca_demo.maxant.ch/", "cancelTickets");
    private final static QName _Exception_QNAME = new QName("http://bookingsystem.jca_demo.maxant.ch/", "Exception");
    private final static QName _BookTicketsResponse_QNAME = new QName("http://bookingsystem.jca_demo.maxant.ch/", "bookTicketsResponse");
    private final static QName _BookTickets_QNAME = new QName("http://bookingsystem.jca_demo.maxant.ch/", "bookTickets");
    private final static QName _CancelTicketsResponse_QNAME = new QName("http://bookingsystem.jca_demo.maxant.ch/", "cancelTicketsResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: ch.maxant.jca_demo.bookingsystem
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link BookTicketsResponse }
     * 
     */
    public BookTicketsResponse createBookTicketsResponse() {
        return new BookTicketsResponse();
    }

    /**
     * Create an instance of {@link BookTickets }
     * 
     */
    public BookTickets createBookTickets() {
        return new BookTickets();
    }

    /**
     * Create an instance of {@link CancelTicketsResponse }
     * 
     */
    public CancelTicketsResponse createCancelTicketsResponse() {
        return new CancelTicketsResponse();
    }

    /**
     * Create an instance of {@link ReserveTicketsResponse }
     * 
     */
    public ReserveTicketsResponse createReserveTicketsResponse() {
        return new ReserveTicketsResponse();
    }

    /**
     * Create an instance of {@link ReserveTickets }
     * 
     */
    public ReserveTickets createReserveTickets() {
        return new ReserveTickets();
    }

    /**
     * Create an instance of {@link IOException }
     * 
     */
    public IOException createIOException() {
        return new IOException();
    }

    /**
     * Create an instance of {@link CancelTickets }
     * 
     */
    public CancelTickets createCancelTickets() {
        return new CancelTickets();
    }

    /**
     * Create an instance of {@link Exception }
     * 
     */
    public Exception createException() {
        return new Exception();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReserveTickets }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://bookingsystem.jca_demo.maxant.ch/", name = "reserveTickets")
    public JAXBElement<ReserveTickets> createReserveTickets(ReserveTickets value) {
        return new JAXBElement<ReserveTickets>(_ReserveTickets_QNAME, ReserveTickets.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IOException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://bookingsystem.jca_demo.maxant.ch/", name = "IOException")
    public JAXBElement<IOException> createIOException(IOException value) {
        return new JAXBElement<IOException>(_IOException_QNAME, IOException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReserveTicketsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://bookingsystem.jca_demo.maxant.ch/", name = "reserveTicketsResponse")
    public JAXBElement<ReserveTicketsResponse> createReserveTicketsResponse(ReserveTicketsResponse value) {
        return new JAXBElement<ReserveTicketsResponse>(_ReserveTicketsResponse_QNAME, ReserveTicketsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CancelTickets }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://bookingsystem.jca_demo.maxant.ch/", name = "cancelTickets")
    public JAXBElement<CancelTickets> createCancelTickets(CancelTickets value) {
        return new JAXBElement<CancelTickets>(_CancelTickets_QNAME, CancelTickets.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Exception }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://bookingsystem.jca_demo.maxant.ch/", name = "Exception")
    public JAXBElement<Exception> createException(Exception value) {
        return new JAXBElement<Exception>(_Exception_QNAME, Exception.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BookTicketsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://bookingsystem.jca_demo.maxant.ch/", name = "bookTicketsResponse")
    public JAXBElement<BookTicketsResponse> createBookTicketsResponse(BookTicketsResponse value) {
        return new JAXBElement<BookTicketsResponse>(_BookTicketsResponse_QNAME, BookTicketsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BookTickets }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://bookingsystem.jca_demo.maxant.ch/", name = "bookTickets")
    public JAXBElement<BookTickets> createBookTickets(BookTickets value) {
        return new JAXBElement<BookTickets>(_BookTickets_QNAME, BookTickets.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CancelTicketsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://bookingsystem.jca_demo.maxant.ch/", name = "cancelTicketsResponse")
    public JAXBElement<CancelTicketsResponse> createCancelTicketsResponse(CancelTicketsResponse value) {
        return new JAXBElement<CancelTicketsResponse>(_CancelTicketsResponse_QNAME, CancelTicketsResponse.class, null, value);
    }

}
