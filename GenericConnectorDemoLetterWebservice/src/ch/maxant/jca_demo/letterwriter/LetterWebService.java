package ch.maxant.jca_demo.letterwriter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(name="LetterWriter")
public class LetterWebService {
	
    private final Logger log = Logger.getLogger(this.getClass().getName());
    
    public String writeLetter(@WebParam(name="txid") String txid, @WebParam(name="referenceNumber") String referenceNumber) throws Exception {

        log.log(Level.INFO, "SEND LETTER: " + referenceNumber + " for TXID " + txid);
        if("FAILWSLetterWriter".equals(referenceNumber)){
            throw new Exception("failed for test purposes");
        }else{
            return "will write letter at close of business: " + referenceNumber;
        }
    }

    public void cancelLetter(@WebParam(name="txId") String txId) throws IOException{
    	
        log.log(Level.INFO, "ROLLBACK LETTER: " + txId);
    }

}
