/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.schema;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.syntax.AttributeTypeDescription;
import org.apache.directory.shared.ldap.schema.syntax.parser.AttributeTypeDescriptionSchemaParser;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.ArrayList;
import java.util.List;


/**
 * An integration test class for testing persistence for various operations 
 * on the subschemaSubentry with server restarts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class SchemaPersistenceIT
{
    private static final String SUBSCHEMA_SUBENTRY = "subschemaSubentry";
    private static final AttributeTypeDescriptionSchemaParser ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER =
        new AttributeTypeDescriptionSchemaParser();


    public static DirectoryService service;


    /**
     * Tests to see if an attributeType is persisted when added, then server 
     * is shutdown, then restarted again.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddAttributeTypePersistence() throws Exception
    {
        enableSchema( "nis" );
        List<String> descriptions = new ArrayList<String>();

        // -------------------------------------------------------------------
        // test successful add with everything
        // -------------------------------------------------------------------

        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10000 NAME 'type0' " +
                "OBSOLETE SUP 2.5.4.41 " +
                "EQUALITY caseExactIA5Match " +
                "ORDERING octetStringOrderingMatch " +
                "SUBSTR caseExactIA5SubstringsMatch COLLECTIVE " +
                "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 " +
                "SINGLE-VALUE USAGE userApplications X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10001 NAME ( 'type1' 'altName' ) " +
                "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP 2.5.4.41 " +
                "NO-USER-MODIFICATION USAGE directoryOperation X-SCHEMA 'nis' )" );
        
        modify( DirContext.ADD_ATTRIBUTE, descriptions, "attributeTypes" );
        
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", true );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", true );

        // sync operation happens anyway on shutdowns but just to make sure we can do it again
        service.sync();
        
        service.shutdown();
        service.startup();
        
        AttributesImpl attrs = new AttributesImpl( "objectClass", "metaSchema" );
        attrs.put( "cn", "blah" );
        getSchemaContext( service ).createSubcontext( "cn=blah", attrs );
        
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", true );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", true );
    }
    

    // -----------------------------------------------------------------------
    // Private Utility Methods 
    // -----------------------------------------------------------------------
    

    private void modify( int op, List<String> descriptions, String opAttr ) throws Exception
    {
        LdapDN dn = new LdapDN( getSubschemaSubentryDN() );
        Attribute attr = new AttributeImpl( opAttr );
        for ( String description : descriptions )
        {
            attr.add( description );
        }
        
        Attributes mods = new AttributesImpl();
        mods.put( attr );
        
        getRootContext( service ).modifyAttributes( dn, op, mods );
    }
    
    
    private void enableSchema( String schemaName ) throws NamingException
    {
        // now enable the test schema
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-disabled", "FALSE" );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( "cn=" + schemaName, mods );
    }
    
    
    /**
     * Get's the subschemaSubentry attribute value from the rootDSE.
     * 
     * @return the subschemaSubentry distinguished name
     * @throws NamingException if there are problems accessing the RootDSE
     */
    private String getSubschemaSubentryDN() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ SUBSCHEMA_SUBENTRY } );
        
        NamingEnumeration<SearchResult> results = getRootContext( service ).search( "", "(objectClass=*)", controls );
        SearchResult result = results.next();
        results.close();
        Attribute subschemaSubentry = result.getAttributes().get( SUBSCHEMA_SUBENTRY );
        return ( String ) subschemaSubentry.get();
    }

    
    /**
     * Gets the subschemaSubentry attributes for the global schema.
     * 
     * @return all operational attributes of the subschemaSubentry 
     * @throws NamingException if there are problems accessing this entry
     */
    private Attributes getSubschemaSubentryAttributes() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ "+", "*" } );
        
        NamingEnumeration<SearchResult> results = getRootContext( service ).search( getSubschemaSubentryDN(),
            "(objectClass=*)", controls );
        SearchResult result = results.next();
        results.close();
        return result.getAttributes();
    }


    private void checkAttributeTypePresent( String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------
        
        Attributes attrs = getSubschemaSubentryAttributes();
        Attribute attrTypes = attrs.get( "attributeTypes" );
        AttributeTypeDescription attributeTypeDescription = null; 
        for ( int ii = 0; ii < attrTypes.size(); ii++ )
        {
            String desc = ( String ) attrTypes.get( ii );
            if ( desc.indexOf( oid ) != -1 )
            {
                attributeTypeDescription = ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER.parseAttributeTypeDescription( desc );
                break;
            }
        }
     
        if ( isPresent )
        {
            assertNotNull( attributeTypeDescription );
            assertEquals( oid, attributeTypeDescription.getNumericOid() );
        }
        else
        {
            assertNull( attributeTypeDescription );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------
        
        attrs = null;
        
        if ( isPresent )
        {
            attrs = getSchemaContext( service ).getAttributes( "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName );
            assertNotNull( attrs );
        }
        else
        {
            //noinspection EmptyCatchBlock
            try
            {
                attrs = getSchemaContext( service ).getAttributes( "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName );
                fail( "should never get here" );
            }
            catch( NamingException e )
            {
            }
            assertNull( attrs );
        }
        
        // -------------------------------------------------------------------
        // check to see if it is present in the attributeTypeRegistry
        // -------------------------------------------------------------------
        
        if ( isPresent ) 
        { 
            assertTrue( service.getRegistries().getAttributeTypeRegistry().hasAttributeType( oid ) );
        }
        else
        {
            assertFalse( service.getRegistries().getAttributeTypeRegistry().hasAttributeType( oid ) );
        }
    }
}
