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


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapNameAlreadyBoundException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.syntax.AttributeTypeDescription;
import org.apache.directory.shared.ldap.schema.syntax.parser.AttributeTypeDescriptionSchemaParser;


/**
 * An integration test class for performing various operations on the 
 * subschemaSubentry as listed in the rootDSE.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubschemaSubentryITest extends AbstractAdminTestCase
{
    private static final String GLOBAL_SUBSCHEMA_DN = "cn=schema";
    private static final String SUBSCHEMA_SUBENTRY = "subschemaSubentry";
    private static final AttributeTypeDescriptionSchemaParser attributeTypeDescriptionSchemaParser = 
        new AttributeTypeDescriptionSchemaParser();

    
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
        
        NamingEnumeration<SearchResult> results = rootDSE.search( "", "(objectClass=*)", controls );
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
        
        NamingEnumeration<SearchResult> results = rootDSE.search( getSubschemaSubentryDN(), 
            "(objectClass=*)", controls );
        SearchResult result = results.next();
        results.close();
        return result.getAttributes();
    }
    
    
    /**
     * Make sure the global subschemaSubentry is where it is expected to be. 
     */
    public void testRootDSEsSubschemaSubentry() throws NamingException
    {
        assertEquals( GLOBAL_SUBSCHEMA_DN, getSubschemaSubentryDN() );
        Attributes subschemaSubentryAttrs = getSubschemaSubentryAttributes();
        assertNotNull( subschemaSubentryAttrs );
    }
    
    
    /**
     * Tests the rejection of a delete operation on the SubschemaSubentry (SSSE).
     */
    public void testSSSEDeleteRejection() throws NamingException
    {
        try
        {
            rootDSE.destroySubcontext( getSubschemaSubentryDN() );
            fail( "You are not allowed to delete the global schema subentry" );
        }
        catch( LdapOperationNotSupportedException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }
    }


    /**
     * Tests the rejection of an add operation for the SubschemaSubentry (SSSE).
     */
    public void testSSSEAddRejection() throws NamingException
    {
        try
        {
            rootDSE.createSubcontext( getSubschemaSubentryDN(), getSubschemaSubentryAttributes() );
            fail( "You are not allowed to add the global schema subentry which exists by default" );
        }
        catch( LdapNameAlreadyBoundException e )
        {
            assertEquals( ResultCodeEnum.ENTRY_ALREADY_EXISTS, e.getResultCode() );
        }
    }


    /**
     * Tests the rejection of rename (modifyDn) operation for the SubschemaSubentry (SSSE).
     */
    public void testSSSERenameRejection() throws NamingException
    {
        try
        {
            rootDSE.rename( getSubschemaSubentryDN(), "cn=schema,ou=system" );
            fail( "You are not allowed to rename the global schema subentry which is fixed" );
        }
        catch( LdapOperationNotSupportedException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }
    }


    /**
     * Tests the rejection of move operation for the SubschemaSubentry (SSSE).
     */
    public void testSSSEMoveRejection() throws NamingException
    {
        try
        {
            rootDSE.rename( getSubschemaSubentryDN(), "cn=blah,ou=schema" );
            fail( "You are not allowed to move the global schema subentry which is fixed" );
        }
        catch( LdapOperationNotSupportedException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }

        try
        {
            rootDSE.rename( getSubschemaSubentryDN(), "cn=schema,ou=schema" );
            fail( "You are not allowed to move the global schema subentry which is fixed" );
        }
        catch( LdapOperationNotSupportedException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }
    }
    

    private void enableSchema( String schemaName ) throws NamingException
    {
        // now enable the test schema
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-disabled", "FALSE" );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        super.schemaRoot.modifyAttributes( "cn=" + schemaName, mods );
    }
    
    
    private void disableSchema( String schemaName ) throws NamingException
    {
        // now enable the test schema
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( "m-disabled", "TRUE" );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        super.schemaRoot.modifyAttributes( "cn=" + schemaName, mods );
    }
    
    
    /**
     * Tests the addition of a new attributeType via a modify ADD on the SSSE to disabled schema.
     */
    public void testAddAttributeTypeOnDisabledSchema() throws Exception
    {
        disableSchema( "nis" );
        LdapDN dn = new LdapDN( getSubschemaSubentryDN() );
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogusName' ) " +
            "DESC 'bogus description' SUP name SINGLE-VALUE X-SCHEMA 'nis' )";
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        mods[0] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, 
            new AttributeImpl( "attributeTypes", substrate ) );
        
        rootDSE.modifyAttributes( dn, mods );
        
        Attributes attrs = getSubschemaSubentryAttributes();
        Attribute attrTypes = attrs.get( "attributeTypes" );
        AttributeTypeDescription attributeTypeDescription = null; 
        for ( int ii = 0; ii < attrTypes.size(); ii++ )
        {
            String desc = ( String ) attrTypes.get( ii );
            if ( desc.indexOf( "1.3.6.1.4.1.18060.0.4.0.2.10000" ) != -1 )
            {
                attributeTypeDescription = attributeTypeDescriptionSchemaParser.parseAttributeTypeDescription( desc );
                break;
            }
        }
        
        assertNull( attributeTypeDescription );

        attrs = null;
        attrs = schemaRoot.getAttributes( "oid=1.3.6.1.4.1.18060.0.4.0.2.10000,ou=attributeTypes,cn=nis" );
        assertNotNull( attrs );
        SchemaEntityFactory factory = new SchemaEntityFactory( registries );
        AttributeType at = factory.getAttributeType( attrs, registries, "nis" );
        assertEquals( "1.3.6.1.4.1.18060.0.4.0.2.10000", at.getOid() );
        assertEquals( "name", at.getSuperior().getName() );
        assertEquals( "bogus description", at.getDescription() );
        assertEquals( "bogus", at.getNames()[0] );
        assertEquals( "bogusName", at.getNames()[1] );
        assertEquals( true, at.isCanUserModify() );
        assertEquals( false, at.isCollective() );
        assertEquals( false, at.isObsolete() );
        assertEquals( true, at.isSingleValue() );
    }

    
    /**
     * Tests the addition of a new attributeType via a modify ADD on the SSSE to enabled schema.
     */
    public void testAddAttributeTypeOnEnabledSchema() throws Exception
    {
        enableSchema( "nis" );
        LdapDN dn = new LdapDN( getSubschemaSubentryDN() );
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogusName' ) " +
            "DESC 'bogus description' SUP name SINGLE-VALUE X-SCHEMA 'nis' )";
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        mods[0] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, 
            new AttributeImpl( "attributeTypes", substrate ) );
        
        rootDSE.modifyAttributes( dn, mods );
        
        Attributes attrs = getSubschemaSubentryAttributes();
        Attribute attrTypes = attrs.get( "attributeTypes" );
        AttributeTypeDescription attributeTypeDescription = null; 
        for ( int ii = 0; ii < attrTypes.size(); ii++ )
        {
            String desc = ( String ) attrTypes.get( ii );
            if ( desc.indexOf( "1.3.6.1.4.1.18060.0.4.0.2.10000" ) != -1 )
            {
                attributeTypeDescription = attributeTypeDescriptionSchemaParser.parseAttributeTypeDescription( desc );
                break;
            }
        }
        
        assertNotNull( attributeTypeDescription );
        assertEquals( true, attributeTypeDescription.isSingleValued() );
        assertEquals( false, attributeTypeDescription.isCollective() );
        assertEquals( false, attributeTypeDescription.isObsolete() );
        assertEquals( true, attributeTypeDescription.isUserModifiable() );
        assertEquals( "bogus description", attributeTypeDescription.getDescription() );
        assertEquals( "bogus", attributeTypeDescription.getNames().get( 0 ) );
        assertEquals( "bogusName", attributeTypeDescription.getNames().get( 1 ) );
        assertEquals( "name", attributeTypeDescription.getSuperType() );
        
        attrs = null;
        attrs = schemaRoot.getAttributes( "oid=1.3.6.1.4.1.18060.0.4.0.2.10000,ou=attributeTypes,cn=nis" );
        assertNotNull( attrs );
        SchemaEntityFactory factory = new SchemaEntityFactory( registries );
        AttributeType at = factory.getAttributeType( attrs, registries, "nis" );
        assertEquals( "1.3.6.1.4.1.18060.0.4.0.2.10000", at.getOid() );
        assertEquals( "name", at.getSuperior().getName() );
        assertEquals( "bogus description", at.getDescription() );
        assertEquals( "bogus", at.getNames()[0] );
        assertEquals( "bogusName", at.getNames()[1] );
        assertEquals( true, at.isCanUserModify() );
        assertEquals( false, at.isCollective() );
        assertEquals( false, at.isObsolete() );
        assertEquals( true, at.isSingleValue() );
    }
}
