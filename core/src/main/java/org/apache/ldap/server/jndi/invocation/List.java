package org.apache.ldap.server.jndi.call;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;

public class List extends Call {

    private final Name baseName;
    
    public List( Name baseName )
    {
        if( baseName == null )
        {
            throw new NullPointerException( "baseName" );
        }
        
        this.baseName = baseName;
    }

    public Name getBaseName() {
        return baseName;
    }

    protected Object doExecute(BackingStore store) throws NamingException {
        return store.list( baseName );
    }
}
