package org.apache.ldap.server.jndi.call;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;

public class LookupWithAttrIds extends Call {

    private final Name name;
    private final String[] attributeIds;
    
    public LookupWithAttrIds( Name name, String[] attributeIds )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
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

    protected Object doExecute(BackingStore store) throws NamingException {
        return store.lookup( name, attributeIds );
    }
}
