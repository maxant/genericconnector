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
package ch.maxant.jca_demo.acquirer;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(name="Acquirer")
public class AcquirerWebService {

    private static FileFilter executeFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().toLowerCase().startsWith("exec.");
        }
    };
    
    private File f = new File("~/temp/xa-transactions-state-acquirer");
    
    public AcquirerWebService(){
        if(!f.exists()){
            f.mkdirs();
        }
    }
    
    private final Logger log = Logger.getLogger(this.getClass().getName());
    
    public String reserveMoney(@WebParam(name="txid") String txid, @WebParam(name="referenceNumber") String referenceNumber) throws Exception {

        log.log(Level.INFO, "EXECUTE: reserving money " + referenceNumber + " for TXID " + txid);
        if("FAILWSAcquirer".equals(referenceNumber)){
            throw new Exception("failed for test purposes");
        }else{
            File f2 = File.createTempFile("exec.", ".txt", f);
            try(FileOutputStream fos = new FileOutputStream(f2)){
                fos.write(txid.getBytes(UTF_8));
            }
            return "reserved money " + referenceNumber;
        }
    }

    public void bookReservation(@WebParam(name="txId") String txId) throws IOException{
        
        updateStatus(txId, "commit");
        
        log.log(Level.INFO, "COMMIT money: " + txId);
    }

    public void cancelReservation(@WebParam(name="txId") String txId) throws IOException {
        updateStatus(txId, "rollback");
        log.log(Level.INFO, "rollback money: " + txId);
    }
    
    public List<String> findUnfinishedTransactions() throws IOException{
        List<String> unfinishedTxs = new ArrayList<>();

        //TODO add another state? we dont want to recover ones which have only just started! maybe look at age of file?
        for(File f : f.listFiles(executeFilter)){
            try(FileInputStream fis = new FileInputStream(f)){
                byte[] data = new byte[(int)f.length()];
                fis.read(data);
                unfinishedTxs.add(new String(data, UTF_8));
                log.log(Level.INFO, "Unfinished transaction found by web service: " + f.getName());
            }
        }
        return unfinishedTxs;
    }

    private void updateStatus(final String txId, String status) throws IOException,
            FileNotFoundException {
        for(File f : f.listFiles(executeFilter)){
            try(FileInputStream fis = new FileInputStream(f)){
                byte[] data = new byte[(int)f.length()];
                fis.read(data);
                if(new String(data, UTF_8).equals(txId)){
                    f.renameTo(new File(f.getParentFile(), status + "." + f.getName().substring(4)));
                    break;
                }
            }catch(FileNotFoundException e){
                //this can happen if one call is recovering
                //and the other is cleaning up. just ignore it.
            }
        }
    }

}
