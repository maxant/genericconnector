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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import javax.transaction.xa.Xid;

/** An implementation of Xid transaction IDs which uses base 64 to 
   make the IDs HTTP friendly - i.e. they can be transported across
   HTTP connections. */
public class XidImpl implements Xid {
    
    private byte[] gtid;
    private int fid;
    private byte[] bq;

    public XidImpl(byte[] gtid, int fid, byte[] bq) {
        this.gtid = gtid;
        this.fid = fid;
        this.bq = bq;
    }

    @Override
    public byte[] getBranchQualifier() {
        return bq;
    }
    
    @Override
    public int getFormatId() {
        return fid;
    }
    
    @Override
    public byte[] getGlobalTransactionId() {
        return gtid;
    }

    /** gets a log friendly / web service friendly version of the global transaction ID */
    @Override
    public String toString(){
        return asString(this);
    }
    
    /** gets a log friendly / web service friendly version (base 64) of the global transaction ID 
     * @param xidImpl */
    public static String asString(Xid xid){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(new Object[]{xid.getBranchQualifier(), xid.getGlobalTransactionId(), xid.getFormatId()});
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to marshal xid " + xid, e);
        }
    }
    
    /** @return Convert a (base 64 encoded) string into an Xid. */
    public static Xid getXid(String base64) {
        try{
            byte[] decoded = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object[] o = (Object[]) ois.readObject();
            byte[] branchQualifier = (byte[]) o[0];
            byte[] gtid = (byte[]) o[1];
            int formatId = (Integer) o[2];
            
            return new XidImpl(gtid, formatId, branchQualifier);
        } catch (ClassNotFoundException e){
            //should never ever happen
            throw new RuntimeException("Failed to unmarshal xid " + base64, e);
        } catch (IOException e) {
            //should never ever happen
            throw new RuntimeException("Failed to unmarshal xid " + base64, e);
        }
    }
}
