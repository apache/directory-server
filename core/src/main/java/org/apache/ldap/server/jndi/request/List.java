package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class List extends Call {

    private final Name baseName;
    
    public List( Name baseName )
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
