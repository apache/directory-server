package org.apache.ldap.server.jndi.call;

import javax.naming.Name;

public class ModifyRN extends Call {

    private final Name name;
    private final String newRelativeName;
    private final boolean deleteOldName;
    
    public ModifyRN( Name name, String newRelativeName,
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
