package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class ListRequest extends Request {

    private final Name baseName;
    
    public ListRequest( Name baseName )
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
}
