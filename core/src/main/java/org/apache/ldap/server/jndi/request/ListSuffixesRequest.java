package org.apache.ldap.server.jndi.request;

public class ListSuffixesRequest extends Request {

    private final boolean normalized;
    
    public ListSuffixesRequest( boolean normalized )
    {
        this.normalized = normalized;
    }

    public boolean isNormalized() {
        return normalized;
    }
}
