package org.apache.ldap.server.jndi.invocation;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.ldap.server.BackingStore;

public class Add extends Invocation {

    private final String userProvidedName;
    private final Name normalizedName;
    private final Attributes attributes;
    
    public Add( String userProvidedName, Name normalizedName, Attributes attributes )
    {
        if( userProvidedName == null )
        {
            throw new NullPointerException( "userProvidedName" );
        }
        
        if( normalizedName == null )
        {
            throw new NullPointerException( "normalizedName" );
        }
        
        if( attributes == null )
        {
            throw new NullPointerException( "attributes" );
        }
        
        this.userProvidedName = userProvidedName;
        this.normalizedName = normalizedName;
        this.attributes = attributes;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public Name getNormalizedName() {
        return normalizedName;
    }
    
    public String getUserProvidedName() {
        return userProvidedName;
    }

    protected Object doExecute( BackingStore store ) throws NamingException {
        store.add( userProvidedName, normalizedName, attributes );
        return null;
    }
}
