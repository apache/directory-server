package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class Move extends Call {

    private final Name name;
    private final Name newParentName;
    
    public Move( Name name, Name newParentName )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        
        if( newParentName == null )
        {
            throw new NullPointerException( "newParentName" );
        }
        
        this.name = name;
        this.newParentName = newParentName;
    }
    
    public Name getName()
    {
        return name;
    }

    public Name getNewParentName()
    {
        return newParentName;
    }
}
