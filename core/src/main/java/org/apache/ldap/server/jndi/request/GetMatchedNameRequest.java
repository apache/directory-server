package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class GetMatchedNameRequest extends Request {

    private final Name name;
    private final boolean normalized;
    
    public GetMatchedNameRequest( Name name, boolean normalized )
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
}
