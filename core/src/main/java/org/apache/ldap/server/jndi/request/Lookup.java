package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class Lookup extends Call {

    private final Name name;
    
    public Lookup( Name name )
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
