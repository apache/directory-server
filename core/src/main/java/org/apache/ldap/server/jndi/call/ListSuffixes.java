package org.apache.ldap.server.jndi.call;

public class ListSuffixes extends Call {

    private final boolean normalized;
    
    public ListSuffixes( boolean normalized )
    {
        this.normalized = normalized;
    }

    public boolean isNormalized() {
        return normalized;
    }
}
