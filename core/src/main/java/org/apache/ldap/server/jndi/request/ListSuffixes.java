package org.apache.ldap.server.jndi.request;

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
