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
