/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.directory.shared.ldap.ldif;


import junit.framework.TestCase;

import org.apache.directory.shared.ldap.ldif.LdifParser;
import org.apache.directory.shared.ldap.ldif.LdifParserImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;

import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.NamingException;


/**
 * @author <a href="mailto:akarasulu@safehaus.org">Alex Karasulu</a>
 * @version $Rev$
 */
public class LdifParserImplTest extends TestCase
{
    public void testLdifParser() throws NamingException
    {
        String ldif = "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                "cn: app1\n" +
                "objectClass: top\n" +
                "objectClass: apApplication\n" +
                "displayName: app1\n" +
                "serviceType: http\n" +
                "dependencies:\n" +
                "httpHeaders:\n" +
                "startupOptions:\n" +
                "envVars:";
        LdifParser parser = new LdifParserImpl();
        Attributes attrs = new LockableAttributesImpl();
        parser.parse( attrs, ldif );

        assertNotNull( attrs );

        Attribute attr = attrs.get( "objectClass" );
        assertTrue( attr.contains( "apApplication" ) );
        assertTrue( attr.contains( "top" ) );
    }


    public void testLdifParserComments() throws NamingException
    {
        String ldif = "#comment\n" +
                "dn: cn=app1,ou=applications,ou=conf,dc=apache,dc=org\n" +
                "cn: app1#another comment\n" +
                "objectClass: top\n" +
                "objectClass: apApplication\n" +
                "displayName: app1\n" +
                "serviceType: http\n" +
                "dependencies:\n" +
                "httpHeaders:\n" +
                "startupOptions:\n" +
                "envVars:";
        LdifParser parser = new LdifParserImpl();
        Attributes attrs = new LockableAttributesImpl();
        parser.parse( attrs, ldif );

        assertNotNull( attrs );

        Attribute attr = attrs.get( "objectClass" );
        assertTrue( attr.contains( "apApplication" ) );
        assertTrue( attr.contains( "top" ) );

        assertEquals( "app1", attrs.get( "cn" ).get() );
    }
}
