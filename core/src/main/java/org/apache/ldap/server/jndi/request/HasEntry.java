package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class HasEntry extends Call {

    private final Name name;
    
    public HasEntry( Name name )
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
