package org.apache.ldap.server.jndi.request;

import javax.naming.Name;

public class LookUpWithAttributeIdsRequest extends Request {

    private final Name name;
    private final String[] attributeIds;
    
    public LookUpWithAttributeIdsRequest( Name name, String[] attributeIds )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        if( attributeIds == null )
        {
            throw new NullPointerException( "attributeIds" );
        }
        
        this.name = name;
        this.attributeIds = attributeIds;
    }

    public Name getName() {
        return name;
    }
    
    public String[] getAttributeIds() {
        return attributeIds;
    }
}
