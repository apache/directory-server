package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class DeleteRequest extends Request {

    private final Name name;
    
    public DeleteRequest( Name name )
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
