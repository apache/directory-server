package org.apache.ldap.server.jndi.request;

import javax.naming.Name;
import javax.naming.directory.ModificationItem;

public class ModifyManyRequest extends Request {

    private final Name name;
    private final ModificationItem[] modificationItems;
    
    public ModifyManyRequest( Name name, ModificationItem[] modificationItems )
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
