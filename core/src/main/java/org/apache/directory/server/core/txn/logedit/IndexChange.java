
package org.apache.directory.server.core.txn.logedit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.Serializer;
import org.apache.directory.server.core.txn.TxnManagerFactory;

import org.apache.directory.shared.ldap.model.entry.Value;

public class IndexChange<ID> extends AbstractDataChange<ID>
{
    /** Index this change is done on */
    private transient Index<?, ?, ID> index;
    
    /** oid of the attribute the index is on */
    private String oid;
    
    /** key of the forward index */
    private Value<?> key;
    
    /** if for the index */
    private ID id;
    
    /** Change type */
    Type type;
    
    // For externalizable
    public IndexChange()
    {
        
    }
    
    public IndexChange( Index<?, ?, ID> index, String oid, Value<?> key, ID id, Type type )
    {
        this.index = index;
        this.oid = oid;
        this.key = key;
        this.id = id;
        this.type = type;
    }
    
    
    public String getOID()
    {
        return this.oid;
    }
    
    public Index<?, ?, ID> getIndex()
    {
        return index;
    }
    
    
    public Value<?> getKey()
    {
        return key;
    }
    
    public ID getID()
    {
        return id;
    }
    
    public Type getType()
    {
        return type;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        Serializer idSerializer = TxnManagerFactory.txnManagerInstance().getIDSerializer();
        
        oid = in.readUTF();
        key = (Value<?>) in.readObject();
        
        int len = in.readInt();
        byte[] buf = new byte[len];
        in.readFully( buf );
        id = (ID)idSerializer.deserialize( buf );
        
        type = Type.values()[in.readInt()];
    }


    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        Serializer idSerializer = TxnManagerFactory.txnManagerInstance().getIDSerializer();
        
        out.writeUTF( oid );
        out.writeObject( key );
        
        byte[] buf = idSerializer.serialize( id );
        out.writeInt( buf.length );
        out.write( buf );
        
        out.writeInt( type.ordinal() );
    }
    
    
    
    public enum Type
    {
        ADD,
        DELETE
    }
    
    
}
