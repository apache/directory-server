package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class HasEntryRequest extends Request {

    private final Name name;
    
    public HasEntryRequest( Name name )
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
}
