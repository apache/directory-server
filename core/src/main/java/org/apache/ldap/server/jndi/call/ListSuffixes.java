package org.apache.ldap.server.jndi.call;

import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;
import org.apache.ldap.server.PartitionNexus;

public class ListSuffixes extends Call {

    private final boolean normalized;
    
    public ListSuffixes( boolean normalized )
    {
        this.normalized = normalized;
    }

    public boolean isNormalized() {
        return normalized;
    }

    protected Object doExecute(BackingStore store) throws NamingException {
        return ( ( PartitionNexus ) store ).listSuffixes( normalized );
    }
}
