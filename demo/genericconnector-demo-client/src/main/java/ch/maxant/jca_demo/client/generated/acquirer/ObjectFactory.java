
package ch.maxant.jca_demo.client.generated.acquirer;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the ch.maxant.jca_demo.acquirer package. 
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

    private final static QName _BookReservation_QNAME = new QName("http://acquirer.jca_demo.maxant.ch/", "bookReservation");
    private final static QName _CancelReservation_QNAME = new QName("http://acquirer.jca_demo.maxant.ch/", "cancelReservation");
    private final static QName _FindUnfinishedTransactionsResponse_QNAME = new QName("http://acquirer.jca_demo.maxant.ch/", "findUnfinishedTransactionsResponse");
    private final static QName _ReserveMoney_QNAME = new QName("http://acquirer.jca_demo.maxant.ch/", "reserveMoney");
    private final static QName _FindUnfinishedTransactions_QNAME = new QName("http://acquirer.jca_demo.maxant.ch/", "findUnfinishedTransactions");
    private final static QName _ReserveMoneyResponse_QNAME = new QName("http://acquirer.jca_demo.maxant.ch/", "reserveMoneyResponse");
    private final static QName _Exception_QNAME = new QName("http://acquirer.jca_demo.maxant.ch/", "Exception");
    private final static QName _CancelReservationResponse_QNAME = new QName("http://acquirer.jca_demo.maxant.ch/", "cancelReservationResponse");
    private final static QName _BookReservationResponse_QNAME = new QName("http://acquirer.jca_demo.maxant.ch/", "bookReservationResponse");
    private final static QName _IOException_QNAME = new QName("http://acquirer.jca_demo.maxant.ch/", "IOException");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: ch.maxant.jca_demo.acquirer
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link FindUnfinishedTransactions }
     * 
     */
    public FindUnfinishedTransactions createFindUnfinishedTransactions() {
        return new FindUnfinishedTransactions();
    }

    /**
     * Create an instance of {@link ReserveMoneyResponse }
     * 
     */
    public ReserveMoneyResponse createReserveMoneyResponse() {
        return new ReserveMoneyResponse();
    }

    /**
     * Create an instance of {@link BookReservation }
     * 
     */
    public BookReservation createBookReservation() {
        return new BookReservation();
    }

    /**
     * Create an instance of {@link CancelReservation }
     * 
     */
    public CancelReservation createCancelReservation() {
        return new CancelReservation();
    }

    /**
     * Create an instance of {@link FindUnfinishedTransactionsResponse }
     * 
     */
    public FindUnfinishedTransactionsResponse createFindUnfinishedTransactionsResponse() {
        return new FindUnfinishedTransactionsResponse();
    }

    /**
     * Create an instance of {@link ReserveMoney }
     * 
     */
    public ReserveMoney createReserveMoney() {
        return new ReserveMoney();
    }

    /**
     * Create an instance of {@link CancelReservationResponse }
     * 
     */
    public CancelReservationResponse createCancelReservationResponse() {
        return new CancelReservationResponse();
    }

    /**
     * Create an instance of {@link BookReservationResponse }
     * 
     */
    public BookReservationResponse createBookReservationResponse() {
        return new BookReservationResponse();
    }

    /**
     * Create an instance of {@link IOException }
     * 
     */
    public IOException createIOException() {
        return new IOException();
    }

    /**
     * Create an instance of {@link Exception }
     * 
     */
    public Exception createException() {
        return new Exception();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BookReservation }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://acquirer.jca_demo.maxant.ch/", name = "bookReservation")
    public JAXBElement<BookReservation> createBookReservation(BookReservation value) {
        return new JAXBElement<BookReservation>(_BookReservation_QNAME, BookReservation.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CancelReservation }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://acquirer.jca_demo.maxant.ch/", name = "cancelReservation")
    public JAXBElement<CancelReservation> createCancelReservation(CancelReservation value) {
        return new JAXBElement<CancelReservation>(_CancelReservation_QNAME, CancelReservation.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FindUnfinishedTransactionsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://acquirer.jca_demo.maxant.ch/", name = "findUnfinishedTransactionsResponse")
    public JAXBElement<FindUnfinishedTransactionsResponse> createFindUnfinishedTransactionsResponse(FindUnfinishedTransactionsResponse value) {
        return new JAXBElement<FindUnfinishedTransactionsResponse>(_FindUnfinishedTransactionsResponse_QNAME, FindUnfinishedTransactionsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReserveMoney }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://acquirer.jca_demo.maxant.ch/", name = "reserveMoney")
    public JAXBElement<ReserveMoney> createReserveMoney(ReserveMoney value) {
        return new JAXBElement<ReserveMoney>(_ReserveMoney_QNAME, ReserveMoney.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FindUnfinishedTransactions }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://acquirer.jca_demo.maxant.ch/", name = "findUnfinishedTransactions")
    public JAXBElement<FindUnfinishedTransactions> createFindUnfinishedTransactions(FindUnfinishedTransactions value) {
        return new JAXBElement<FindUnfinishedTransactions>(_FindUnfinishedTransactions_QNAME, FindUnfinishedTransactions.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReserveMoneyResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://acquirer.jca_demo.maxant.ch/", name = "reserveMoneyResponse")
    public JAXBElement<ReserveMoneyResponse> createReserveMoneyResponse(ReserveMoneyResponse value) {
        return new JAXBElement<ReserveMoneyResponse>(_ReserveMoneyResponse_QNAME, ReserveMoneyResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Exception }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://acquirer.jca_demo.maxant.ch/", name = "Exception")
    public JAXBElement<Exception> createException(Exception value) {
        return new JAXBElement<Exception>(_Exception_QNAME, Exception.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CancelReservationResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://acquirer.jca_demo.maxant.ch/", name = "cancelReservationResponse")
    public JAXBElement<CancelReservationResponse> createCancelReservationResponse(CancelReservationResponse value) {
        return new JAXBElement<CancelReservationResponse>(_CancelReservationResponse_QNAME, CancelReservationResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BookReservationResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://acquirer.jca_demo.maxant.ch/", name = "bookReservationResponse")
    public JAXBElement<BookReservationResponse> createBookReservationResponse(BookReservationResponse value) {
        return new JAXBElement<BookReservationResponse>(_BookReservationResponse_QNAME, BookReservationResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IOException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://acquirer.jca_demo.maxant.ch/", name = "IOException")
    public JAXBElement<IOException> createIOException(IOException value) {
        return new JAXBElement<IOException>(_IOException_QNAME, IOException.class, null, value);
    }

}
