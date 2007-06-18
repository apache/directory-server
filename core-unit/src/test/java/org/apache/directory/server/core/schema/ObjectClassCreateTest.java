package org.apache.directory.server.core.schema;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.name.LdapDN;

public class ObjectClassCreateTest extends AbstractAdminTestCase
{
    private String testOID                               =
        "1.3.6.1.4.1.18060.0.4.0.3.1.555555.5555.5555555";
    
    /**
     * Gets relative DN to ou=schema.
     */
    private final LdapDN getObjectClassContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=objectClasses,cn=" + schemaName );
    }

    /*
     * Test that I can create an ObjectClass entry with an invalid
     */
    public void testCreateObjectClassWithInvalidNameAttribute() 
    throws NamingException
    {
        Attributes attributes = new BasicAttributes();
        Attribute  objectClassAttribute = new BasicAttribute( "objectClass" );
        
        objectClassAttribute.add( "top" );
        objectClassAttribute.add( "metaTop" );
        objectClassAttribute.add( "metaObjectClass" );
        
        attributes.put( objectClassAttribute );
        
        attributes.put( "m-oid", "testOID" );
        
        // This name is invalid
        attributes.put( "m-name", "http://example.com/users/accounts/L0" );
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + testOID );
        
        try
        {
            schemaRoot.createSubcontext( dn, attributes );
            fail(); // Should not reach this point
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }
}
