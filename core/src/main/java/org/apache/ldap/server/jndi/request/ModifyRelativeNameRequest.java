package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class ModifyRelativeNameRequest extends Request {

    private final Name name;
    private final String newRelativeName;
    private final boolean deleteOldName;
    
    public ModifyRelativeNameRequest( Name name, String newRelativeName,
                                      boolean deleteOldName )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        
        if( newRelativeName == null )
        {
            throw new NullPointerException( "newRelativeName" );
        }
        
        this.name = name;
        this.newRelativeName = newRelativeName;
        this.deleteOldName = deleteOldName;
    }
    
    public Name getName()
    {
        return name;
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
