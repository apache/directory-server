package org.apache.ldap.server.jndi.request;

import javax.naming.Name;
import javax.naming.directory.Attributes;

public class AddRequest extends Request {

    private final String userProvidedName;
    private final Name normalizedName;
    private final Attributes attributes;
    
    public AddRequest( String userProvidedName, Name normalizedName,
                       Attributes attributes )
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
}
