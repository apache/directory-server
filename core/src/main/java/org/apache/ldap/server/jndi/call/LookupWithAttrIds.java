package org.apache.ldap.server.jndi.call;

import javax.naming.Name;

public class LookupWithAttrIds extends Call {

    private final Name name;
    private final String[] attributeIds;
    
    public LookupWithAttrIds( Name name, String[] attributeIds )
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
