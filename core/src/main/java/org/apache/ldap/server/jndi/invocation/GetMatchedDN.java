package org.apache.ldap.server.jndi.invocation;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;
import org.apache.ldap.server.PartitionNexus;

public class GetMatchedDN extends Invocation {

    private final Name name;
    private final boolean normalized;
    
    public GetMatchedDN( Name name, boolean normalized )
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
        return ( ( PartitionNexus ) store ).getMatchedDn( name, normalized );
    }
}
