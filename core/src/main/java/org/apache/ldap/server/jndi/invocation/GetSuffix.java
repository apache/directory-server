package org.apache.ldap.server.jndi.call;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;
import org.apache.ldap.server.ContextPartition;

public class GetSuffix extends Call {

    private final Name name;
    private final boolean normalized;
    
    public GetSuffix( Name name, boolean normalized )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        
        this.name = name;
        this.normalized = normalized;
    }

    public boolean isNormalized() {
        return normalized;
    }

    public Name getName() {
        return name;
    }

    protected Object doExecute(BackingStore store) throws NamingException {
        return ( ( ContextPartition) store ).getSuffix( normalized );
    }
}
