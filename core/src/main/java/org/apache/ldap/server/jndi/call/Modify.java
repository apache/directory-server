package org.apache.ldap.server.jndi.call;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.ldap.server.BackingStore;

public class Modify extends Call {

    private final Name name;
    private final int modOp;
    private final Attributes attributes;
    
    public Modify( Name name, int modOp, Attributes attributes )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        
        this.name = name;
        this.modOp = modOp;
        this.attributes = attributes;
    }

    public Name getName()
    {
        return name;
    }
    
    public int getModOp()
    {
        return modOp;
    }
    
    public Attributes getAttributes()
    {
        return attributes;
    }

    protected Object doExecute(BackingStore store) throws NamingException {
        store.modify( name, modOp, attributes );
        return null;
    }
}
