package org.apache.ldap.server.jndi.call;

import javax.naming.Name;

public class IsSuffix extends Call {

    private final Name name;
    
    public IsSuffix( Name name )
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
