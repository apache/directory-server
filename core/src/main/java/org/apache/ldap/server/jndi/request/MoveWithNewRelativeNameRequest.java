package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class MoveWithNewRelativeNameRequest extends Request {

    private final Name name;
    private final Name newParentName;
    private final String newRelativeName;
    private final boolean deleteOldName;
    
    public MoveWithNewRelativeNameRequest( Name name, Name newParentName, String newRelativeName,
                                           boolean deleteOldName )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        if( newParentName == null )
        {
            throw new NullPointerException( "newParentName" );
        }
        if( newRelativeName == null )
        {
            throw new NullPointerException( "newRelativeName" );
        }
        
        this.name = name;
        this.newParentName = newParentName;
        this.newRelativeName = newRelativeName;
        this.deleteOldName = deleteOldName;
    }
    
    public Name getName()
    {
        return name;
    }
    
    public Name getNewParentName()
    {
        return newParentName;
    }

    public String getNewRelativeName()
    {
        return newRelativeName;
    }
    
    public boolean isDeleteOldName()
    {
        return deleteOldName;
    }
}
