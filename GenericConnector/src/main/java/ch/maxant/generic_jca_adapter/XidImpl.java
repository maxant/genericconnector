package ch.maxant.generic_jca_adapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import javax.transaction.xa.Xid;

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
    		throw new RuntimeException("Failed to unmarshal xid " + base64, e);
    	} catch (IOException e) {
			throw new RuntimeException("Failed to unmarshal xid " + base64, e);
    	}
    }
}
