package org.apache.ldap.server.jndi.call;

import javax.naming.Name;
import javax.naming.directory.ModificationItem;

public class ModifyMany extends Call {

    private final Name name;
    private final ModificationItem[] modificationItems;
    
    public ModifyMany( Name name, ModificationItem[] modificationItems )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        if( modificationItems == null )
        {
            throw new NullPointerException( "modificationItems" );
        }
        
        this.name = name;
        this.modificationItems = modificationItems;
    }

    public Name getName()
    {
        return name;
    }
    
    public ModificationItem[] getModificationItems()
    {
        return modificationItems;
    }
}
