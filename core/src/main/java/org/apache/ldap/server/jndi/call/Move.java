package org.apache.ldap.server.jndi.call;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;

public class Move extends Call {

    private final Name name;
    private final Name newParentName;
    
    public Move( Name name, Name newParentName )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        
        if( newParentName == null )
        {
            throw new NullPointerException( "newParentName" );
        }
        
        this.name = name;
        this.newParentName = newParentName;
    }
    
    public Name getName()
    {
        return name;
    }

    public Name getNewParentName()
    {
        return newParentName;
    }

    protected Object doExecute(BackingStore store) throws NamingException {
        store.move( name, newParentName );
        return null;
    }
}
