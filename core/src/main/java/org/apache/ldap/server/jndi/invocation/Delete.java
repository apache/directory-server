package org.apache.ldap.server.jndi.call;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;

public class Delete extends Call {

    private final Name name;
    
    public Delete( Name name )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        
        this.name = name;
    }

    public Name getName() {
        return name;
    }

    protected Object doExecute(BackingStore store) throws NamingException {
        store.delete( name );
        return null;
    }
}
