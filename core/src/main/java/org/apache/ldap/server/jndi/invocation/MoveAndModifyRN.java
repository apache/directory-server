package org.apache.ldap.server.jndi.invocation;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;

public class MoveAndModifyRN extends Invocation {

    private final Name name;
    private final Name newParentName;
    private final String newRelativeName;
    private final boolean deleteOldName;
    
    public MoveAndModifyRN( Name name, Name newParentName, String newRelativeName,
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

    protected Object doExecute(BackingStore store) throws NamingException {
        store.move( name, newParentName, newRelativeName, deleteOldName );
        return null;
    }
}
