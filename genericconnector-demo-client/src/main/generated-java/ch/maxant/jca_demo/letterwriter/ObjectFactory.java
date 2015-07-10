
package ch.maxant.jca_demo.letterwriter;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the ch.maxant.jca_demo.letterwriter package. 
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

    private final static QName _CancelLetterResponse_QNAME = new QName("http://letterwriter.jca_demo.maxant.ch/", "cancelLetterResponse");
    private final static QName _WriteLetterResponse_QNAME = new QName("http://letterwriter.jca_demo.maxant.ch/", "writeLetterResponse");
    private final static QName _IOException_QNAME = new QName("http://letterwriter.jca_demo.maxant.ch/", "IOException");
    private final static QName _CancelLetter_QNAME = new QName("http://letterwriter.jca_demo.maxant.ch/", "cancelLetter");
    private final static QName _WriteLetter_QNAME = new QName("http://letterwriter.jca_demo.maxant.ch/", "writeLetter");
    private final static QName _Exception_QNAME = new QName("http://letterwriter.jca_demo.maxant.ch/", "Exception");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: ch.maxant.jca_demo.letterwriter
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CancelLetterResponse }
     * 
     */
    public CancelLetterResponse createCancelLetterResponse() {
        return new CancelLetterResponse();
    }

    /**
     * Create an instance of {@link WriteLetterResponse }
     * 
     */
    public WriteLetterResponse createWriteLetterResponse() {
        return new WriteLetterResponse();
    }

    /**
     * Create an instance of {@link IOException }
     * 
     */
    public IOException createIOException() {
        return new IOException();
    }

    /**
     * Create an instance of {@link CancelLetter }
     * 
     */
    public CancelLetter createCancelLetter() {
        return new CancelLetter();
    }

    /**
     * Create an instance of {@link WriteLetter }
     * 
     */
    public WriteLetter createWriteLetter() {
        return new WriteLetter();
    }

    /**
     * Create an instance of {@link Exception }
     * 
     */
    public Exception createException() {
        return new Exception();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CancelLetterResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://letterwriter.jca_demo.maxant.ch/", name = "cancelLetterResponse")
    public JAXBElement<CancelLetterResponse> createCancelLetterResponse(CancelLetterResponse value) {
        return new JAXBElement<CancelLetterResponse>(_CancelLetterResponse_QNAME, CancelLetterResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WriteLetterResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://letterwriter.jca_demo.maxant.ch/", name = "writeLetterResponse")
    public JAXBElement<WriteLetterResponse> createWriteLetterResponse(WriteLetterResponse value) {
        return new JAXBElement<WriteLetterResponse>(_WriteLetterResponse_QNAME, WriteLetterResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IOException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://letterwriter.jca_demo.maxant.ch/", name = "IOException")
    public JAXBElement<IOException> createIOException(IOException value) {
        return new JAXBElement<IOException>(_IOException_QNAME, IOException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CancelLetter }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://letterwriter.jca_demo.maxant.ch/", name = "cancelLetter")
    public JAXBElement<CancelLetter> createCancelLetter(CancelLetter value) {
        return new JAXBElement<CancelLetter>(_CancelLetter_QNAME, CancelLetter.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WriteLetter }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://letterwriter.jca_demo.maxant.ch/", name = "writeLetter")
    public JAXBElement<WriteLetter> createWriteLetter(WriteLetter value) {
        return new JAXBElement<WriteLetter>(_WriteLetter_QNAME, WriteLetter.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Exception }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://letterwriter.jca_demo.maxant.ch/", name = "Exception")
    public JAXBElement<Exception> createException(Exception value) {
        return new JAXBElement<Exception>(_Exception_QNAME, Exception.class, null, value);
    }

}
