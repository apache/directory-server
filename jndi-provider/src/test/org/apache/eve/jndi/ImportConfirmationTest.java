/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.jndi;


import java.io.InputStream;

import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.ldap.common.ldif.LdifParser;
import org.apache.ldap.common.ldif.LdifParserImpl;
import org.apache.ldap.common.ldif.LdifIterator;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.common.exception.LdapConfigurationException;


/**
 * Tests to make sure the system.ldif entries were properly imported into the
 * system partition.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ImportConfirmationTest extends AbstractJndiTest
{
    /**
     * Tests to make sure we can authenticate after the database has already
     * been build as the admin user when simple authentication is in effect.
     *
     * @throws Exception if anything goes wrong
     */
    public void testConfirmImportedEntries() throws Exception
    {
        InputStream in = ( InputStream ) getClass().getResourceAsStream( "system.ldif" );
        LdifParser parser = new LdifParserImpl();

        try
        {
            LdifIterator iterator = new LdifIterator( in );
            while ( iterator.hasNext() )
            {
                Attributes ldifAttributes = new LockableAttributesImpl();
                String ldif = ( String ) iterator.next();
                parser.parse( ldifAttributes, ldif );
                Name dn = new LdapName( ( String ) ldifAttributes.remove( "dn" ).get() );

                dn.remove( 0 );
                Attributes entry = sysRoot.getAttributes( dn );

                assertNotNull( entry );
                NamingEnumeration ids = ldifAttributes.getIDs();
                while ( ids.hasMore() )
                {
                    String id = ( String ) ids.next();
                    Attribute entryAttribute = entry.get( id );
                    Attribute ldifAttribute = entry.get( id );
                    assertNotNull( ldifAttribute );
                    assertNotNull( entryAttribute );

                    for ( int ii = 0; ii < ldifAttribute.size(); ii++ )
                    {
                        assertTrue( entryAttribute.contains( ldifAttribute.get( ii ) ) );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            String msg = "failed while trying to parse system ldif file";
            NamingException ne = new LdapConfigurationException( msg );
            ne.setRootCause( e );
            throw ne;
        }
    }
}
