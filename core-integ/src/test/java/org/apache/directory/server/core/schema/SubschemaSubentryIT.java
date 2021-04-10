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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.naming.NamingException;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.LdapSyntax;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.ObjectClass;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.SyntaxChecker;
import org.apache.directory.api.ldap.model.schema.comparators.BooleanComparator;
import org.apache.directory.api.ldap.model.schema.normalizers.DeepTrimNormalizer;
import org.apache.directory.api.ldap.model.schema.parsers.AttributeTypeDescriptionSchemaParser;
import org.apache.directory.api.ldap.model.schema.parsers.LdapComparatorDescription;
import org.apache.directory.api.ldap.model.schema.parsers.LdapComparatorDescriptionSchemaParser;
import org.apache.directory.api.ldap.model.schema.parsers.LdapSyntaxDescriptionSchemaParser;
import org.apache.directory.api.ldap.model.schema.parsers.MatchingRuleDescriptionSchemaParser;
import org.apache.directory.api.ldap.model.schema.parsers.NormalizerDescription;
import org.apache.directory.api.ldap.model.schema.parsers.NormalizerDescriptionSchemaParser;
import org.apache.directory.api.ldap.model.schema.parsers.ObjectClassDescriptionSchemaParser;
import org.apache.directory.api.ldap.model.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.api.ldap.model.schema.parsers.SyntaxCheckerDescriptionSchemaParser;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.OctetStringSyntaxChecker;
import org.apache.directory.api.ldap.schema.loader.SchemaEntityFactory;
import org.apache.directory.api.util.Base64;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * An integration test class for performing various operations on the
 * subschemaSubentry as listed in the rootDSE.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "SubschemaSubentryIT-class")
public class SubschemaSubentryIT extends AbstractLdapTestUnit
{
    private static final String GLOBAL_SUBSCHEMA_DN = "cn=schema";
    private static final String SUBSCHEMA_SUBENTRY = "subschemaSubentry";

    private static final SyntaxCheckerDescriptionSchemaParser SYNTAX_CHECKER_DESCRIPTION_SCHEMA_PARSER = new SyntaxCheckerDescriptionSchemaParser();
    private static final AttributeTypeDescriptionSchemaParser ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER = new AttributeTypeDescriptionSchemaParser();
    private LdapComparatorDescriptionSchemaParser comparatorDescriptionSchemaParser = new LdapComparatorDescriptionSchemaParser();
    private NormalizerDescriptionSchemaParser normalizerDescriptionSchemaParser = new NormalizerDescriptionSchemaParser();
    private LdapSyntaxDescriptionSchemaParser ldapSyntaxDescriptionSchemaParser = new LdapSyntaxDescriptionSchemaParser();
    private MatchingRuleDescriptionSchemaParser matchingRuleDescriptionSchemaParser = new MatchingRuleDescriptionSchemaParser();
    private ObjectClassDescriptionSchemaParser objectClassDescriptionSchemaParser = new ObjectClassDescriptionSchemaParser();

    private static LdapConnection connection;
    private String subschemaSubentryDn;
    private Entry subschemaSubentry;


    @BeforeEach
    public void init() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( getService() );

        Entry rootDse = connection.lookup( "", SUBSCHEMA_SUBENTRY );
        Attribute subschemaSubentryAT = rootDse.get( SUBSCHEMA_SUBENTRY );

        subschemaSubentryDn = subschemaSubentryAT.getString();
        updateSSSE();
    }


    /**
     * Test for DIRSHARED-60.
     * It is allowed to add an attribute type description without any matching rule.
     * Adding it via ou=schema partition worked. Adding it via the subschema subentry failed.
     */
    @Test
    public void testAddAttributeTypeWithoutMatchingRule() throws Exception
    {
        ModifyRequest modRequest = new ModifyRequestImpl();
        modRequest.setName( new Dn( GLOBAL_SUBSCHEMA_DN ) );
        modRequest.add( "attributeTypes", "( 2.5.4.58 NAME 'attributeCertificateAttribute' "
            + " DESC 'attribute certificate use ;binary' SYNTAX 1.3.6.1.4.1.1466.115.121.1.8 )" );
        ModifyResponse response = connection.modify( modRequest );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
    }


    /**
     * Make sure the global subschemaSubentry is where it is expected to be.
     *
     * @throws NamingException on error
     */
    @Test
    public void testRootDseSubschemaSubentry() throws Exception
    {
        assertEquals( GLOBAL_SUBSCHEMA_DN, subschemaSubentryDn );
        assertNotNull( subschemaSubentry );
    }


    /**
     * Tests the rejection of a delete operation on the SubschemaSubentry (SSSE).
     *
     * @throws NamingException on error
     */
    @Test
    public void testSSSEDeleteRejection() throws Exception
    {
        Assertions.assertThrows( LdapException.class, () -> 
        {
            connection.delete( subschemaSubentryDn );
            fail( "You are not allowed to delete the global schema subentry" );
        } );
    }


    /**
     * Tests the rejection of an add operation for the SubschemaSubentry (SSSE).
     *
     * @throws NamingException on error
     */
    @Test
    public void testSSSEAddRejection() throws Exception
    {
        Assertions.assertThrows( LdapException.class, () -> 
        {
            connection.add( subschemaSubentry );
            fail( "You are not allowed to add the global schema subentry which exists by default" );
        } );
    }


    /**
     * Tests the rejection of rename (modifyDn) operation for the SubschemaSubentry (SSSE).
     *
     * @throws NamingException on error
     */
    @Test
    public void testSSSERenameRejection() throws Exception
    {
        Assertions.assertThrows( LdapException.class, () -> 
        {
            connection.rename( subschemaSubentryDn, "cn=schema,ou=system" );
            fail( "You are not allowed to rename the global schema subentry which is fixed" );
        } );
    }


    /**
     * Tests the rejection of move operation for the SubschemaSubentry (SSSE).
     *
     * @throws NamingException on error
     */
    @Test
    public void testSSSEMoveRejection() throws Exception
    {
        try
        {
            connection.rename( subschemaSubentryDn, "cn=blah,ou=schema" );
            fail( "You are not allowed to move the global schema subentry which is fixed" );
        }
        catch ( LdapException e )
        {
        }

        try
        {
            connection.rename( subschemaSubentryDn, "cn=schema,ou=schema" );
            fail( "You are not allowed to move the global schema subentry which is fixed" );
        }
        catch ( LdapException e )
        {
        }
    }


    // -----------------------------------------------------------------------
    // SyntaxChecker Tests
    // -----------------------------------------------------------------------

    private void checkSyntaxCheckerPresent( SchemaManager schemaManager, String oid, String schemaName,
        boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------
        // get the subschemaSubentry again
        updateSSSE();

        Attribute attrTypes = subschemaSubentry.get( "syntaxCheckers" );
        SyntaxCheckerDescription syntaxCheckerDescription = null;

        for ( Value value : attrTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( oid ) != -1 )
            {
                syntaxCheckerDescription = SYNTAX_CHECKER_DESCRIPTION_SCHEMA_PARSER.parse( desc );
                break;
            }
        }

        if ( isPresent )
        {
            assertNotNull( syntaxCheckerDescription );
            assertEquals( oid, syntaxCheckerDescription.getOid() );
        }
        else
        {
            assertNull( syntaxCheckerDescription );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------
        if ( isPresent )
        {
            Entry entry = connection.lookup( "m-oid=" + oid + ",ou=syntaxCheckers,cn=" + schemaName + ",ou=schema" );
            assertNotNull( entry );
            SchemaEntityFactory factory = new SchemaEntityFactory();

            SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, entry, getService()
                .getSchemaManager().getRegistries(), schemaName );
            assertEquals( oid, syntaxChecker.getOid() );
        }
        else
        {
            Entry entry = connection.lookup( "m-oid=" + oid + ",ou=syntaxCheckers,cn=" + schemaName + ",ou=schema" );

            assertNull( entry );
        }

        // -------------------------------------------------------------------
        // check to see if it is present in the syntaxCheckerRegistry
        // -------------------------------------------------------------------

        if ( isPresent )
        {
            assertTrue( getService().getSchemaManager().getSyntaxCheckerRegistry().contains( oid ) );
        }
        else
        {
            assertFalse( getService().getSchemaManager().getSyntaxCheckerRegistry().contains( oid ) );
        }
    }


    /**
     * Tests a number of modify add, remove and replace operation combinations for
     * a syntaxChecker on the schema subentry.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddRemoveReplaceSyntaxCheckers() throws Exception
    {
        // 1st change
        enableSchema( "nis" );
        List<String> descriptions = new ArrayList<String>();

        // ( 1.3.6.1.4.1.18060.0.4.0.2.10000 DESC 'bogus desc' FQCN org.foo.Bar BYTECODE 14561234 )
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN "
            + OctetStringSyntaxChecker.class.getName() + " X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10001 DESC 'bogus desc' FQCN "
            + OctetStringSyntaxChecker.class.getName() + " X-SCHEMA 'nis' )" );

        // -------------------------------------------------------------------
        // add and check
        // -------------------------------------------------------------------
        // 2nd change
        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "syntaxCheckers" );
        checkSyntaxCheckerPresent( getService().getSchemaManager(), "1.3.6.1.4.1.18060.0.4.1.0.10000", "nis", true );
        checkSyntaxCheckerPresent( getService().getSchemaManager(), "1.3.6.1.4.1.18060.0.4.1.0.10001", "nis", true );

        // -------------------------------------------------------------------
        // remove and check
        // -------------------------------------------------------------------

        // 3rd change
        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "syntaxCheckers" );
        checkSyntaxCheckerPresent( getService().getSchemaManager(), "1.3.6.1.4.1.18060.0.4.1.0.10000", "nis", false );
        checkSyntaxCheckerPresent( getService().getSchemaManager(), "1.3.6.1.4.1.18060.0.4.1.0.10001", "nis", false );

        // -------------------------------------------------------------------
        // test failure to replace
        // -------------------------------------------------------------------

        try
        {
            modify( ModificationOperation.REPLACE_ATTRIBUTE, descriptions, "syntaxCheckers" );
            fail( "modify REPLACE operations should not be allowed" );
        }
        catch ( LdapException e )
        {
        }

        // -------------------------------------------------------------------
        // check add with valid bytecode
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions
            .add( "( 1.3.6.1.4.1.18060.0.4.1.0.10002 DESC 'bogus desc' FQCN org.apache.directory.api.ldap.model.schema.syntaxCheckers.DummySyntaxChecker BYTECODE "
                + getByteCode( "DummySyntaxChecker.bytecode" ) + " X-SCHEMA 'nis' )" );

        // 4th change
        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "syntaxCheckers" );
        checkSyntaxCheckerPresent( getService().getSchemaManager(), "1.3.6.1.4.1.18060.0.4.1.0.10002", "nis", true );

        // -------------------------------------------------------------------
        // check remove
        // -------------------------------------------------------------------

        // 5th change
        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "syntaxCheckers" );
        checkSyntaxCheckerPresent( getService().getSchemaManager(), "1.3.6.1.4.1.18060.0.4.1.0.10002", "nis", false );

        // -------------------------------------------------------------------
        // check add no schema info
        // -------------------------------------------------------------------
        descriptions.clear();
        descriptions
            .add( "( 1.3.6.1.4.1.18060.0.4.1.0.10002 DESC 'bogus desc' FQCN org.apache.directory.api.ldap.model.schema.syntaxCheckers.DummySyntaxChecker BYTECODE "
                + getByteCode( "DummySyntaxChecker.bytecode" ) + " )" );

        // 6th change
        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "syntaxCheckers" );
        checkSyntaxCheckerPresent( getService().getSchemaManager(), "1.3.6.1.4.1.18060.0.4.1.0.10002", "other", true );

        // after a total of 6 changes
        if ( getService().getChangeLog().getLatest() != null )
        {
            assertEquals( getService().getChangeLog().getLatest().getRevision() + 6, getService().getChangeLog()
                .getCurrentRevision() );
        }
    }


    // -----------------------------------------------------------------------
    // Comparator Tests
    // -----------------------------------------------------------------------

    private void checkComparatorPresent( String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------
        // get the subschemaSubentry again
        updateSSSE();

        Attribute attrTypes = subschemaSubentry.get( "comparators" );
        LdapComparatorDescription comparatorDescription = null;

        for ( Value value : attrTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( oid ) != -1 )
            {
                comparatorDescription = comparatorDescriptionSchemaParser.parse( desc );
                break;
            }
        }

        if ( isPresent )
        {
            assertNotNull( comparatorDescription );
            assertEquals( oid, comparatorDescription.getOid() );
        }
        else
        {
            assertNull( comparatorDescription );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------
        Entry entry = null;

        if ( isPresent )
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=comparators,cn=" + schemaName + ",ou=schema" );
            assertNotNull( entry );
        }
        else
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=comparators,cn=" + schemaName + ",ou=schema" );

            assertNull( entry );
        }

        // -------------------------------------------------------------------
        // check to see if it is present in the comparatorRegistry
        // -------------------------------------------------------------------
        if ( isPresent )
        {
            assertTrue( getService().getSchemaManager().getComparatorRegistry().contains( oid ) );
        }
        else
        {
            assertFalse( getService().getSchemaManager().getComparatorRegistry().contains( oid ) );
        }
    }


    /**
     * Tests a number of modify add, remove and replace operation combinations for
     * comparators on the schema subentry.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddRemoveReplaceComparators() throws Exception
    {
        enableSchema( "nis" );
        List<String> descriptions = new ArrayList<String>();

        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10002 DESC 'bogus desc' FQCN "
            + BooleanComparator.class.getName() + " X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10001 DESC 'bogus desc' FQCN "
            + BooleanComparator.class.getName() + " X-SCHEMA 'nis' )" );

        // -------------------------------------------------------------------
        // add and check
        // -------------------------------------------------------------------

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "comparators" );
        checkComparatorPresent( "1.3.6.1.4.1.18060.0.4.1.0.10002", "nis", true );
        checkComparatorPresent( "1.3.6.1.4.1.18060.0.4.1.0.10001", "nis", true );

        // -------------------------------------------------------------------
        // remove and check
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "comparators" );
        checkComparatorPresent( "1.3.6.1.4.1.18060.0.4.1.0.10002", "nis", false );
        checkComparatorPresent( "1.3.6.1.4.1.18060.0.4.1.0.10001", "nis", false );

        // -------------------------------------------------------------------
        // test failure to replace
        // -------------------------------------------------------------------

        try
        {
            modify( ModificationOperation.REPLACE_ATTRIBUTE, descriptions, "comparators" );
            fail( "modify REPLACE operations should not be allowed" );
        }
        catch ( LdapException e )
        {
        }

        // -------------------------------------------------------------------
        // check add with valid bytecode
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions
            .add( "( 1.3.6.1.4.1.18060.0.4.0.1.100000 DESC 'bogus desc' FQCN org.apache.directory.api.ldap.model.schema.comparators.DummyComparator BYTECODE "
                + getByteCode( "DummyComparator.bytecode" ) + " X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "comparators" );
        checkComparatorPresent( "1.3.6.1.4.1.18060.0.4.0.1.100000", "nis", true );

        // -------------------------------------------------------------------
        // check remove with valid bytecode
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "comparators" );
        checkComparatorPresent( "1.3.6.1.4.1.18060.0.4.0.1.100000", "nis", false );

        // -------------------------------------------------------------------
        // check add no schema info
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions
            .add( "( 1.3.6.1.4.1.18060.0.4.0.1.100000 DESC 'bogus desc' FQCN org.apache.directory.api.ldap.model.schema.comparators.DummyComparator BYTECODE "
                + getByteCode( "DummyComparator.bytecode" ) + " )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "comparators" );
        checkComparatorPresent( "1.3.6.1.4.1.18060.0.4.0.1.100000", "other", true );
    }


    // -----------------------------------------------------------------------
    // Normalizer Tests
    // -----------------------------------------------------------------------

    private void checkNormalizerPresent( String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------
        // get the subschemaSubentry again
        updateSSSE();

        Attribute attrTypes = subschemaSubentry.get( "normalizers" );
        NormalizerDescription normalizerDescription = null;

        for ( Value value : attrTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( oid ) != -1 )
            {
                normalizerDescription = normalizerDescriptionSchemaParser.parse( desc );
                break;
            }
        }

        if ( isPresent )
        {
            assertNotNull( normalizerDescription );
            assertEquals( oid, normalizerDescription.getOid() );
        }
        else
        {
            assertNull( normalizerDescription );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------

        Entry entry = null;

        if ( isPresent )
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=normalizers,cn=" + schemaName + ",ou=schema" );
            assertNotNull( entry );
        }
        else
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=normalizers,cn=" + schemaName + ",ou=schema" );

            assertNull( entry );
        }

        // -------------------------------------------------------------------
        // check to see if it is present in the normalizerRegistry
        // -------------------------------------------------------------------

        if ( isPresent )
        {
            assertTrue( getService().getSchemaManager().getNormalizerRegistry().contains( oid ) );
        }
        else
        {
            assertFalse( getService().getSchemaManager().getNormalizerRegistry().contains( oid ) );
        }
    }


    /**
     * Tests a number of modify add, remove and replace operation combinations for
     * normalizers on the schema subentry.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddRemoveReplaceNormalizers() throws Exception
    {
        enableSchema( "nis" );
        List<String> descriptions = new ArrayList<String>();

        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10002 DESC 'bogus desc' FQCN "
            + DeepTrimNormalizer.class.getName() + " X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10001 DESC 'bogus desc' FQCN "
            + DeepTrimNormalizer.class.getName() + " X-SCHEMA 'nis' )" );

        // -------------------------------------------------------------------
        // add and check
        // -------------------------------------------------------------------

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "normalizers" );
        checkNormalizerPresent( "1.3.6.1.4.1.18060.0.4.1.0.10002", "nis", true );
        checkNormalizerPresent( "1.3.6.1.4.1.18060.0.4.1.0.10001", "nis", true );

        // -------------------------------------------------------------------
        // remove and check
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "normalizers" );
        checkNormalizerPresent( "1.3.6.1.4.1.18060.0.4.1.0.10002", "nis", false );
        checkNormalizerPresent( "1.3.6.1.4.1.18060.0.4.1.0.10001", "nis", false );

        // -------------------------------------------------------------------
        // test failure to replace
        // -------------------------------------------------------------------

        try
        {
            modify( ModificationOperation.REPLACE_ATTRIBUTE, descriptions, "normalizers" );
            fail( "modify REPLACE operations should not be allowed" );
        }
        catch ( LdapException e )
        {
        }

        // -------------------------------------------------------------------
        // check add with valid bytecode
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions
            .add( "( 1.3.6.1.4.1.18060.0.4.0.1.100000 DESC 'bogus desc' FQCN org.apache.directory.api.ldap.model.schema.normalizers.DummyNormalizer BYTECODE "
                + getByteCode( "DummyNormalizer.bytecode" ) + " X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "normalizers" );
        checkNormalizerPresent( "1.3.6.1.4.1.18060.0.4.0.1.100000", "nis", true );

        // -------------------------------------------------------------------
        // check remove with valid bytecode
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "normalizers" );
        checkNormalizerPresent( "1.3.6.1.4.1.18060.0.4.0.1.100000", "nis", false );

        // -------------------------------------------------------------------
        // check add no schema info
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions
            .add( "( 1.3.6.1.4.1.18060.0.4.0.1.100000 DESC 'bogus desc' FQCN org.apache.directory.api.ldap.model.schema.normalizers.DummyNormalizer BYTECODE "
                + getByteCode( "DummyNormalizer.bytecode" ) + " )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "normalizers" );
        checkNormalizerPresent( "1.3.6.1.4.1.18060.0.4.0.1.100000", "other", true );
    }


    // -----------------------------------------------------------------------
    // Syntax Tests
    // -----------------------------------------------------------------------

    private void checkSyntaxPresent( String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------
        // get the subschemaSubentry again
        updateSSSE();

        Attribute attrTypes = subschemaSubentry.get( "ldapSyntaxes" );
        LdapSyntax ldapSyntax = null;

        for ( Value value : attrTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( oid ) != -1 )
            {
                ldapSyntax = ldapSyntaxDescriptionSchemaParser.parse( desc );
                break;
            }
        }

        if ( isPresent )
        {
            assertNotNull( ldapSyntax );
            assertEquals( oid, ldapSyntax.getOid() );
        }
        else
        {
            assertNull( ldapSyntax );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------

        Entry entry = null;

        if ( isPresent )
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=syntaxes,cn=" + schemaName + ",ou=schema" );
            assertNotNull( entry );
        }
        else
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=syntaxes,cn=" + schemaName + ",ou=schema" );

            assertNull( entry );
        }

        // -------------------------------------------------------------------
        // check to see if it is present in the syntaxRegistry
        // -------------------------------------------------------------------

        if ( isPresent )
        {
            assertTrue( getService().getSchemaManager().getLdapSyntaxRegistry().contains( oid ) );
        }
        else
        {
            assertFalse( getService().getSchemaManager().getLdapSyntaxRegistry().contains( oid ) );
        }
    }


    /**
     * Tests a number of modify add, remove and replace operation combinations for
     * syntaxes on the schema subentry.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddRemoveReplaceSyntaxes() throws Exception
    {
        enableSchema( "nis" );
        List<String> descriptions = new ArrayList<String>();

        // -------------------------------------------------------------------
        // add of syntaxes without their syntax checkers should fail
        // -------------------------------------------------------------------

        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10001 DESC 'bogus desc' X-SCHEMA 'nis' )" );

        try
        {
            modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "ldapSyntaxes" );
            fail( "should not be able to add syntaxes without their syntaxCheckers" );
        }
        catch ( LdapException e )
        {
        }

        // none of the syntaxes should be present
        checkSyntaxPresent( "1.3.6.1.4.1.18060.0.4.1.0.10000", "nis", false );
        checkSyntaxPresent( "1.3.6.1.4.1.18060.0.4.1.0.10001", "nis", false );

        // -------------------------------------------------------------------
        // first in order to add syntaxes we need their syntax checkers
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN "
            + OctetStringSyntaxChecker.class.getName() + " X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10001 DESC 'bogus desc' FQCN "
            + OctetStringSyntaxChecker.class.getName() + " X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10002 DESC 'bogus desc' FQCN "
            + OctetStringSyntaxChecker.class.getName() + " X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "syntaxCheckers" );
        checkSyntaxCheckerPresent( getService().getSchemaManager(), "1.3.6.1.4.1.18060.0.4.1.0.10000", "nis", true );
        checkSyntaxCheckerPresent( getService().getSchemaManager(), "1.3.6.1.4.1.18060.0.4.1.0.10001", "nis", true );
        checkSyntaxCheckerPresent( getService().getSchemaManager(), "1.3.6.1.4.1.18060.0.4.1.0.10002", "nis", true );

        // -------------------------------------------------------------------
        // add and check
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10001 DESC 'bogus desc' X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "ldapSyntaxes" );
        checkSyntaxPresent( "1.3.6.1.4.1.18060.0.4.1.0.10000", "nis", true );
        checkSyntaxPresent( "1.3.6.1.4.1.18060.0.4.1.0.10001", "nis", true );

        // -------------------------------------------------------------------
        // remove and check
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "ldapSyntaxes" );
        checkSyntaxPresent( "1.3.6.1.4.1.18060.0.4.1.0.10000", "nis", false );
        checkSyntaxPresent( "1.3.6.1.4.1.18060.0.4.1.0.10001", "nis", false );

        // -------------------------------------------------------------------
        // test failure to replace
        // -------------------------------------------------------------------

        try
        {
            modify( ModificationOperation.REPLACE_ATTRIBUTE, descriptions, "ldapSyntaxes" );
            fail( "modify REPLACE operations should not be allowed" );
        }
        catch ( LdapException e )
        {
        }

        // -------------------------------------------------------------------
        // check add no schema info
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.0.10002 DESC 'bogus desc' )" );
        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "ldapSyntaxes" );
        checkSyntaxPresent( "1.3.6.1.4.1.18060.0.4.1.0.10002", "other", true );
    }


    // -----------------------------------------------------------------------
    // MatchingRule Tests
    // -----------------------------------------------------------------------

    private void checkMatchingRulePresent( String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------
        // get the subschemaSubentry again
        updateSSSE();

        Attribute attrTypes = subschemaSubentry.get( "matchingRules" );
        MatchingRule matchingRule = null;

        for ( Value value : attrTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( oid ) != -1 )
            {
                matchingRule = matchingRuleDescriptionSchemaParser.parse( desc );
                break;
            }
        }

        if ( isPresent )
        {
            assertNotNull( matchingRule );
            assertEquals( oid, matchingRule.getOid() );
        }
        else
        {
            assertNull( matchingRule );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------

        Entry entry = null;

        if ( isPresent )
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=matchingRules,cn=" + schemaName + ",ou=schema" );
            assertNotNull( entry );
        }
        else
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=matchingRules,cn=" + schemaName + ",ou=schema" );

            assertNull( entry );
        }

        // -------------------------------------------------------------------
        // check to see if it is present in the matchingRuleRegistry
        // -------------------------------------------------------------------

        if ( isPresent )
        {
            assertTrue( getService().getSchemaManager().getMatchingRuleRegistry().contains( oid ) );
        }
        else
        {
            assertFalse( getService().getSchemaManager().getMatchingRuleRegistry().contains( oid ) );
        }
    }


    /**
     * Tests a number of modify add, remove and replace operation combinations for
     * matchingRules on the schema subentry.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddRemoveReplaceMatchingRules() throws Exception
    {
        enableSchema( "nis" );
        List<String> descriptions = new ArrayList<String>();

        // -------------------------------------------------------------------
        // test rejection with non-existant syntax
        // -------------------------------------------------------------------

        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10000 DESC 'bogus desc' SYNTAX 1.2.3.4 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10001 DESC 'bogus desc' SYNTAX 1.2.3.4 X-SCHEMA 'nis' )" );

        try
        {
            modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "matchingRules" );
            fail( "Cannot add matchingRule with bogus non-existant syntax" );
        }
        catch ( LdapException e )
        {
        }

        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10000", "nis", false );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10001", "nis", false );

        // -------------------------------------------------------------------
        // test add with existant syntax but no name and no desc
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10000 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10001 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "matchingRules" );

        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10000", "nis", true );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10001", "nis", true );

        // -------------------------------------------------------------------
        // test add with existant syntax but no name
        // -------------------------------------------------------------------

        // clear the matchingRules out now
        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "matchingRules" );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10000", "nis", false );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10001", "nis", false );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10000 DESC 'bogus desc' "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10001 DESC 'bogus desc' "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "matchingRules" );

        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10000", "nis", true );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10001", "nis", true );

        // -------------------------------------------------------------------
        // test add success with name
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "matchingRules" );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10000", "nis", false );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10001", "nis", false );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10000 NAME 'blah0' DESC 'bogus desc' "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10001 NAME ( 'blah1' 'othername1' ) DESC 'bogus desc' "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "matchingRules" );

        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10000", "nis", true );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10001", "nis", true );

        // -------------------------------------------------------------------
        // test add success full (with obsolete)
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "matchingRules" );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10000", "nis", false );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10001", "nis", false );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10000 NAME 'blah0' DESC 'bogus desc' "
            + "OBSOLETE SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10001 NAME ( 'blah1' 'othername1' ) DESC 'bogus desc' "
            + "OBSOLETE SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "matchingRules" );

        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10000", "nis", true );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10001", "nis", true );

        // -------------------------------------------------------------------
        // test failure to replace
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "matchingRules" );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10000", "nis", false );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10001", "nis", false );

        try
        {
            modify( ModificationOperation.REPLACE_ATTRIBUTE, descriptions, "matchingRules" );
            fail( "modify REPLACE operations should not be allowed" );
        }
        catch ( LdapException e )
        {
        }

        // -------------------------------------------------------------------
        // check add no schema info
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.1.10002 DESC 'bogus desc' "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )" );
        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "matchingRules" );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.1.10002", "other", true );
    }


    // -----------------------------------------------------------------------
    // AttributeType Tests
    // -----------------------------------------------------------------------

    private void checkAttributeTypePresent( String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------
        // get the subschemaSubentry again
        updateSSSE();

        Attribute attrTypes = subschemaSubentry.get( "attributeTypes" );
        AttributeType attributeType = null;

        for ( Value value : attrTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( oid ) != -1 )
            {
                attributeType = ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER.parse( desc );
                break;
            }
        }

        if ( isPresent )
        {
            assertNotNull( attributeType );
            assertEquals( oid, attributeType.getOid() );
        }
        else
        {
            assertNull( attributeType );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------
        Entry entry = null;

        if ( isPresent )
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName + ",ou=schema" );
            assertNotNull( entry );
        }
        else
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName + ",ou=schema" );

            assertNull( entry );
        }

        // -------------------------------------------------------------------
        // check to see if it is present in the attributeTypeRegistry
        // -------------------------------------------------------------------

        if ( isPresent )
        {
            assertTrue( getService().getSchemaManager().getAttributeTypeRegistry().contains( oid ) );
        }
        else
        {
            assertFalse( getService().getSchemaManager().getAttributeTypeRegistry().contains( oid ) );
        }
    }


    /**
     * Tests a number of modify add, remove and replace operation combinations for
     * attributeTypes on the schema subentry.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddRemoveReplaceAttributeTypes() throws Exception
    {
        enableSchema( "nis" );
        List<String> descriptions = new ArrayList<String>();

        // -------------------------------------------------------------------
        // test rejection with non-existant syntax
        // -------------------------------------------------------------------

        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10000 DESC 'bogus desc' " + "SYNTAX 1.2.3.4 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10001 DESC 'bogus desc' " + "SYNTAX 1.2.3.4 X-SCHEMA 'nis' )" );

        try
        {
            modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "attributeTypes" );
            fail( "Cannot add attributeType with bogus non-existant syntax" );
        }
        catch ( LdapException e )
        {
        }

        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", false );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", false );

        // -------------------------------------------------------------------
        // test reject with non-existant super type
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10000 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP 1.2.3.4 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10001 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP 1.2.3.4 X-SCHEMA 'nis' )" );

        try
        {
            modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "attributeTypes" );
            fail( "Cannot add attributeType with bogus non-existant syntax" );
        }
        catch ( LdapException e )
        {
        }

        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", false );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", false );

        // -------------------------------------------------------------------
        // test reject with non-existant equality matchingRule
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10000 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 EQUALITY 1.2.3.4 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10001 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 EQUALITY 1.2.3.4 X-SCHEMA 'nis' )" );

        try
        {
            modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "attributeTypes" );
            fail( "Cannot add attributeType with bogus non-existant equality MatchingRule" );
        }
        catch ( LdapException e )
        {
        }

        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", false );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", false );

        // -------------------------------------------------------------------
        // test reject with non-existant ordering matchingRule
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10000 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 ORDERING 1.2.3.4 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10001 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 ORDERING 1.2.3.4 X-SCHEMA 'nis' )" );

        try
        {
            modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "attributeTypes" );
            fail( "Cannot add attributeType with bogus non-existant ordering MatchingRule" );
        }
        catch ( LdapException e )
        {
        }

        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", false );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", false );

        // -------------------------------------------------------------------
        // test reject with non-existant substring matchingRule
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10000 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUBSTR 1.2.3.4 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10001 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUBSTR 1.2.3.4 X-SCHEMA 'nis' )" );

        try
        {
            modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "attributeTypes" );
            fail( "Cannot add attributeType with bogus non-existant substrings MatchingRule" );
        }
        catch ( LdapException e )
        {
        }

        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", false );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", false );

        // -------------------------------------------------------------------
        // test success with valid superior, valid syntax but no name
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10000 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP 2.5.4.41 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10001 "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP 2.5.4.41 X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "attributeTypes" );

        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", true );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", true );

        // -------------------------------------------------------------------
        // test success with valid superior, valid syntax and names
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "attributeTypes" );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10000 NAME 'type0' "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP 2.5.4.41 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10001 NAME ( 'type1' 'altName' ) "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP 2.5.4.41 X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "attributeTypes" );

        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", true );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", true );

        // -------------------------------------------------------------------
        // test success with everything
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "attributeTypes" );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10000 NAME 'type0' " + "OBSOLETE SUP 2.5.4.41 "
            + "EQUALITY caseExactIA5Match " + "ORDERING octetStringOrderingMatch "
            + "SUBSTR caseExactIA5SubstringsMatch COLLECTIVE " + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 "
            + "USAGE userApplications X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10001 NAME ( 'type1' 'altName' ) "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP 2.5.4.41 " + "USAGE userApplications X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "attributeTypes" );

        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", true );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", true );

        // -------------------------------------------------------------------
        // test failure to replace
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "attributeTypes" );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", false );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", false );

        try
        {
            modify( ModificationOperation.REPLACE_ATTRIBUTE, descriptions, "attributeTypes" );
            fail( "modify REPLACE operations should not be allowed" );
        }
        catch ( LdapException e )
        {
        }

        // -------------------------------------------------------------------
        // check add no schema info
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10000 NAME 'type0' " + "OBSOLETE SUP 2.5.4.41 "
            + "EQUALITY caseExactIA5Match " + "ORDERING octetStringOrderingMatch "
            + "SUBSTR caseExactIA5SubstringsMatch COLLECTIVE " + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 "
            + "USAGE userApplications )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.2.10001 NAME ( 'type1' 'altName' ) "
            + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP 2.5.4.41 " + "USAGE userApplications )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "attributeTypes" );

        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10000", "other", true );
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.10001", "other", true );
    }


    /**
     * Tests the addition of a new attributeType via a modify ADD on the SSSE to disabled schema.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddAttributeTypeOnDisabledSchema() throws Exception
    {
        disableSchema( "nis" );
        Dn dn = new Dn( subschemaSubentryDn );
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogusName' ) "
            + "DESC 'bogus description' SUP name SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SINGLE-VALUE X-SCHEMA 'nis' )";
        Modification mod = new DefaultModification(
            ModificationOperation.ADD_ATTRIBUTE, new DefaultAttribute( "attributeTypes", substrate ) );

        connection.modify( dn, mod );

        Attribute attributeTypes = subschemaSubentry.get( "attributeTypes" );
        AttributeType attributeType = null;

        for ( Value value : attributeTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( "1.3.6.1.4.1.18060.0.4.0.2.10000" ) != -1 )
            {
                attributeType = ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER.parse( desc );
                break;
            }
        }

        assertNull( attributeType );

        Entry entry = connection.lookup( "m-oid=1.3.6.1.4.1.18060.0.4.0.2.10000,ou=attributeTypes,cn=nis,ou=schema" );
        assertNotNull( entry );
        SchemaEntityFactory factory = new SchemaEntityFactory();

        AttributeType at = factory.getAttributeType( getService().getSchemaManager(), entry, getService()
            .getSchemaManager().getRegistries(), "nis" );
        assertEquals( "1.3.6.1.4.1.18060.0.4.0.2.10000", at.getOid() );
        assertEquals( "name", at.getSuperiorOid() );
        assertEquals( "bogus description", at.getDescription() );
        assertEquals( "bogus", at.getName() );
        assertEquals( "bogusName", at.getNames().get( 1 ) );
        assertEquals( true, at.isUserModifiable() );
        assertEquals( false, at.isCollective() );
        assertEquals( false, at.isObsolete() );
        assertEquals( true, at.isSingleValued() );
    }


    /**
     * Tests the addition of a new attributeType via a modify ADD on the SSSE to enabled schema.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddAttributeTypeOnEnabledSchema() throws Exception
    {
        enableSchema( "nis" );
        Dn dn = new Dn( subschemaSubentryDn );
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogusName' ) "
            + "DESC 'bogus description' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP name SINGLE-VALUE X-SCHEMA 'nis' )";
        Modification mod = new DefaultModification(
            ModificationOperation.ADD_ATTRIBUTE, new DefaultAttribute( "attributeTypes", substrate ) );

        connection.modify( dn, mod );

        updateSSSE();
        Attribute attributeTypes = subschemaSubentry.get( "attributeTypes" );
        AttributeType attributeType = null;

        for ( Value value : attributeTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( "1.3.6.1.4.1.18060.0.4.0.2.10000" ) != -1 )
            {
                attributeType = ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER.parse( desc );
                break;
            }
        }

        assertNotNull( attributeType );
        assertEquals( true, attributeType.isSingleValued() );
        assertEquals( false, attributeType.isCollective() );
        assertEquals( false, attributeType.isObsolete() );
        assertEquals( true, attributeType.isUserModifiable() );
        assertEquals( "bogus description", attributeType.getDescription() );
        assertEquals( "bogus", attributeType.getNames().get( 0 ) );
        assertEquals( "bogusName", attributeType.getNames().get( 1 ) );
        assertEquals( "name", attributeType.getSuperiorOid() );

        Entry entry = connection.lookup(
            "m-oid=1.3.6.1.4.1.18060.0.4.0.2.10000,ou=attributeTypes,cn=nis,ou=schema" );
        assertNotNull( entry );
        SchemaEntityFactory factory = new SchemaEntityFactory();

        AttributeType at = factory.getAttributeType( getService().getSchemaManager(), entry, getService()
            .getSchemaManager().getRegistries(), "nis" );
        assertEquals( "1.3.6.1.4.1.18060.0.4.0.2.10000", at.getOid() );
        assertEquals( "name", at.getSuperiorOid() );
        assertEquals( "bogus description", at.getDescription() );
        assertEquals( "bogus", at.getNames().get( 0 ) );
        assertEquals( "bogusName", at.getNames().get( 1 ) );
        assertEquals( true, at.isUserModifiable() );
        assertEquals( false, at.isCollective() );
        assertEquals( false, at.isObsolete() );
        assertEquals( true, at.isSingleValued() );
    }


    /**
     * Tests the addition of a new attributeType with some
     * underscores via a modify ADD on the SSSE to enabled schema.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddAttributeTypeWithUnderscoresOnEnabledSchema() throws Exception
    {
        Assertions.assertThrows( LdapInvalidAttributeValueException.class, () -> 
        {
            enableSchema( "nis" );
            Dn dn = new Dn( subschemaSubentryDn );
            String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogus_microsoft_name' ) "
                + "DESC 'bogus description' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP name SINGLE-VALUE X-SCHEMA 'nis' )";
            Modification mod = new DefaultModification(
                ModificationOperation.ADD_ATTRIBUTE, new DefaultAttribute( "attributeTypes", substrate ) );
    
            connection.modify( dn, mod );
        } );
    }


    /**
     * Tests the addition of a new attributeType where the DESC contains only spaces
     */
    @Test
    public void testAddAttributeTypeWithSpaceDesc() throws Exception
    {
        enableSchema( "nis" );
        Dn dn = new Dn( subschemaSubentryDn );
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogusName' ) "
            + "DESC '  ' SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SUP name SINGLE-VALUE X-SCHEMA 'nis' )";
        Modification mod = new DefaultModification(
            ModificationOperation.ADD_ATTRIBUTE, new DefaultAttribute( "attributeTypes", substrate ) );

        // Apply the addition
        connection.modify( dn, mod );

        // Get back the list of attributes, and find the one we just added
        updateSSSE();
        Attribute attributeTypes = subschemaSubentry.get( "attributeTypes" );
        AttributeType attributeType = null;

        for ( Value value : attributeTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( "1.3.6.1.4.1.18060.0.4.0.2.10000" ) != -1 )
            {
                attributeType = ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER.parse( desc );
                break;
            }
        }

        assertNotNull( attributeType );
        assertEquals( true, attributeType.isSingleValued() );
        assertEquals( false, attributeType.isCollective() );
        assertEquals( false, attributeType.isObsolete() );
        assertEquals( true, attributeType.isUserModifiable() );
        assertEquals( "  ", attributeType.getDescription() );
        assertEquals( "bogus", attributeType.getNames().get( 0 ) );
        assertEquals( "bogusName", attributeType.getNames().get( 1 ) );
        assertEquals( "name", attributeType.getSuperiorOid() );

        // Now check that the entry has been added
        Entry entry = connection.lookup(
            "m-oid=1.3.6.1.4.1.18060.0.4.0.2.10000,ou=attributeTypes,cn=nis,ou=schema" );
        assertNotNull( entry );
        SchemaEntityFactory factory = new SchemaEntityFactory();

        AttributeType at = factory.getAttributeType( getService().getSchemaManager(), entry, getService()
            .getSchemaManager().getRegistries(), "nis" );
        assertEquals( "1.3.6.1.4.1.18060.0.4.0.2.10000", at.getOid() );
        assertEquals( "name", at.getSuperiorOid() );
        assertEquals( "  ", at.getDescription() );
        assertEquals( "bogus", at.getNames().get( 0 ) );
        assertEquals( "bogusName", at.getNames().get( 1 ) );
        assertEquals( true, at.isUserModifiable() );
        assertEquals( false, at.isCollective() );
        assertEquals( false, at.isObsolete() );
        assertEquals( true, at.isSingleValued() );
    }


    // -----------------------------------------------------------------------
    // ObjectClass Tests
    // -----------------------------------------------------------------------

    private void checkObjectClassPresent( String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------
        // get the subschemaSubentry again
        updateSSSE();

        Attribute attrTypes = subschemaSubentry.get( "objectClasses" );
        ObjectClass objectClass = null;

        for ( Value value : attrTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( oid ) != -1 )
            {
                objectClass = objectClassDescriptionSchemaParser.parse( desc );
                break;
            }
        }

        if ( isPresent )
        {
            assertNotNull( objectClass );
            assertEquals( oid, objectClass.getOid() );
        }
        else
        {
            assertNull( objectClass );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------

        Entry entry = null;

        if ( isPresent )
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=objectClasses,cn=" + schemaName + ",ou=schema" );
            assertNotNull( entry );
        }
        else
        {
            entry = connection.lookup( "m-oid=" + oid + ",ou=objectClasses,cn=" + schemaName + ",ou=schema" );

            assertNull( entry );
        }

        // -------------------------------------------------------------------
        // check to see if it is present in the matchingRuleRegistry
        // -------------------------------------------------------------------

        if ( isPresent )
        {
            assertTrue( getService().getSchemaManager().getObjectClassRegistry().contains( oid ) );
        }
        else
        {
            assertFalse( getService().getSchemaManager().getObjectClassRegistry().contains( oid ) );
        }
    }


    /**
     * Tests a number of modify add, remove and replace operation combinations for
     * objectClasses on the schema subentry.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddRemoveReplaceObjectClasses() throws Exception
    {
        enableSchema( "nis" );
        List<String> descriptions = new ArrayList<String>();

        // -------------------------------------------------------------------
        // test rejection with non-existant superclass
        // -------------------------------------------------------------------

        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 SUP 1.2.3 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 SUP ( 1.2.3 $ 4.5.6 ) X-SCHEMA 'nis' )" );

        try
        {
            modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );
            fail( "Cannot add objectClass with bogus non-existant super" );
        }
        catch ( LdapException e )
        {
        }

        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", false );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", false );

        // -------------------------------------------------------------------
        // test add with existant superiors but no name and no desc
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 " + "SUP 2.5.6.0 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 " + "SUP 2.5.6.0 X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );

        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", true );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", true );

        // -------------------------------------------------------------------
        // test add with existant superiors with names and no desc
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "objectClasses" );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", false );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", false );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 "
            + "NAME ( 'blah0' 'altname0' ) SUP 2.5.6.0 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 "
            + "NAME ( 'blah1' 'altname1' ) SUP 2.5.6.0 X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );

        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", true );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", true );

        // -------------------------------------------------------------------
        // test add with existant superiors with names and desc
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "objectClasses" );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", false );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", false );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 "
            + "NAME ( 'blah0' 'altname0' ) DESC 'bogus' SUP 2.5.6.0 X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 "
            + "NAME ( 'blah1' 'altname1' ) DESC 'bogus' SUP 2.5.6.0 X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );

        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", true );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", true );

        // -------------------------------------------------------------------
        // test add with many existant superiors with names and desc
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "objectClasses" );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", false );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", false );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 "
            + "NAME ( 'blah0' 'altname0' ) DESC 'bogus' SUP ( 2.5.6.0 $ dynamicObject ) AUXILIARY X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 "
            + "NAME ( 'blah1' 'altname1' ) DESC 'bogus' SUP ( 2.5.6.0 $ domain ) X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );

        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", true );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", true );

        // -------------------------------------------------------------------
        // test reject with non-existant attributeType in may list
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "objectClasses" );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", false );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", false );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 "
            + "NAME ( 'blah0' 'altname0' ) DESC 'bogus' SUP ( 2.5.6.0 $ dynamicObject ) "
            + "MAY ( blah0 $ cn ) X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 "
            + "NAME ( 'blah1' 'altname1' ) DESC 'bogus' SUP ( 2.5.6.0 $ domain ) "
            + "MAY ( sn $ blah1 ) X-SCHEMA 'nis' )" );

        try
        {
            modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );
            fail( "Cannot add objectClass with bogus non-existant attributeTypes" );
        }
        catch ( LdapException e )
        {
        }

        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", false );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", false );

        // -------------------------------------------------------------------
        // test reject with non-existant attributeType in must list
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 "
            + "NAME ( 'blah0' 'altname0' ) DESC 'bogus' SUP ( 2.5.6.0 $ dynamicObject ) "
            + "MUST ( blah0 $ cn ) X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 "
            + "NAME ( 'blah1' 'altname1' ) DESC 'bogus' SUP ( 2.5.6.0 $ domain ) "
            + "MUST ( sn $ blah1 ) X-SCHEMA 'nis' )" );

        try
        {
            modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );
            fail( "Cannot add objectClass with bogus non-existant attributeTypes" );
        }
        catch ( LdapException e )
        {
        }

        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", false );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", false );

        // -------------------------------------------------------------------
        // test add with valid attributeTypes in may list
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 "
            + "NAME ( 'blah0' 'altname0' ) DESC 'bogus' SUP ( 2.5.6.0 $ dynamicObject ) AUXILIARY "
            + "MAY ( sn $ cn ) X-SCHEMA 'nis' )" );
        descriptions
            .add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 "
                + "NAME ( 'blah1' 'altname1' ) DESC 'bogus' SUP ( 2.5.6.0 $ domain ) "
                + "MAY ( sn $ ou ) X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );

        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", true );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", true );

        // -------------------------------------------------------------------
        // test add with valid attributeTypes in must list
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "objectClasses" );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", false );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", false );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 "
            + "NAME ( 'blah0' 'altname0' ) DESC 'bogus' SUP ( 2.5.6.0 $ dynamicObject ) AUXILIARY "
            + "MUST ( sn $ cn ) X-SCHEMA 'nis' )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 "
            + "NAME ( 'blah1' 'altname1' ) DESC 'bogus' SUP ( 2.5.6.0 $ domain ) "
            + "MUST ( sn $ ou ) X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );

        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", true );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", true );

        // -------------------------------------------------------------------
        // test add success full (with obsolete)
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "objectClasses" );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", false );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", false );

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 "
            + "NAME ( 'blah0' 'altname0' ) DESC 'bogus' OBSOLETE SUP ( 2.5.6.0 $ dynamicObject ) AUXILIARY "
            + "MUST ( sn $ cn ) " + "MAY ( gn $ ou ) " + "X-SCHEMA 'nis' ) " );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 "
            + "NAME ( 'blah1' 'altname1' ) DESC 'bogus' OBSOLETE SUP ( 2.5.6.0 $ domain ) STRUCTURAL "
            + "MUST ( sn $ ou ) " + "MAY ( cn $ gn ) " + "X-SCHEMA 'nis' )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );

        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", true );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", true );

        // -------------------------------------------------------------------
        // test failure to replace
        // -------------------------------------------------------------------

        modify( ModificationOperation.REMOVE_ATTRIBUTE, descriptions, "objectClasses" );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "nis", false );
        checkMatchingRulePresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "nis", false );

        try
        {
            modify( ModificationOperation.REPLACE_ATTRIBUTE, descriptions, "objectClasses" );
            fail( "modify REPLACE operations should not be allowed" );
        }
        catch ( LdapException e )
        {
        }

        // -------------------------------------------------------------------
        // check add no schema info
        // -------------------------------------------------------------------

        descriptions.clear();
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10000 "
            + "NAME ( 'blah0' 'altname0' ) DESC 'bogus' OBSOLETE SUP ( 2.5.6.0 $ dynamicObject ) AUXILIARY "
            + "MUST ( sn $ cn ) " + "MAY ( gn $ ou ) )" );
        descriptions.add( "( 1.3.6.1.4.1.18060.0.4.1.3.10001 "
            + "NAME ( 'blah1' 'altname1' ) DESC 'bogus' OBSOLETE SUP ( 2.5.6.0 $ domain ) STRUCTURAL "
            + "MUST ( sn $ ou ) " + "MAY ( gn $ cn ) )" );

        modify( ModificationOperation.ADD_ATTRIBUTE, descriptions, "objectClasses" );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10000", "other", true );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.3.10001", "other", true );
    }


    // -----------------------------------------------------------------------
    // Test Modifier and Timestamp Updates
    // -----------------------------------------------------------------------

    /**
     * This method checks the modifiersName, and the modifyTimestamp on the schema
     * subentry then modifies a schema.  It then checks it again to make sure these
     * values have been updated properly to reflect the modification time and the
     * modifier.
     *
     * @throws InterruptedException on error
     * @throws NamingException on error
     */
    @Test
    @Disabled("The test is failing when run in conjonction with other tests")
    // @TODO as we can't modify a schema element, the end of this test has been commented
    public void testTimestampAndModifierUpdates() throws Exception, InterruptedException
    {
        TimeZone tz = TimeZone.getTimeZone( "GMT" );

        // check first that everything that is required is present

        Attribute creatorsNameAttr = subschemaSubentry.get( "creatorsName" );
        Attribute createTimestampAttr = subschemaSubentry.get( "createTimestamp" );
        assertNotNull( creatorsNameAttr );
        assertNotNull( createTimestampAttr );

        Attribute modifiersNameAttr = subschemaSubentry.get( "modifiersName" );
        Attribute modifyTimestampAttr = subschemaSubentry.get( "modifyTimestamp" );
        assertNotNull( modifiersNameAttr );
        Dn expectedDn = new Dn( getService().getSchemaManager(), "uid=admin,ou=system" );
        assertEquals( expectedDn.getName(), modifiersNameAttr.get() );
        assertNotNull( modifyTimestampAttr );

        Calendar cal = Calendar.getInstance( tz, Locale.ROOT );
        String modifyTimestampStr = modifyTimestampAttr.getString();
        Date modifyTimestamp = DateUtils.getDate( modifyTimestampStr );
        Date currentTimestamp = cal.getTime();

        assertFalse( modifyTimestamp.after( currentTimestamp ) );

        // now update the schema information: add a new attribute type

        enableSchema( "nis" );
        Dn dn = new Dn( subschemaSubentryDn );
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogusName' ) "
            + "DESC 'bogus description' SUP name SINGLE-VALUE X-SCHEMA 'nis' )";
        Modification mod = new DefaultModification(
            ModificationOperation.ADD_ATTRIBUTE, new DefaultAttribute( "attributeTypes", substrate ) );

        connection.modify( dn, mod );

        // now check the modification timestamp and the modifiers name
        // check first that everything that is required is present
        Attribute creatorsNameAttrAfter = subschemaSubentry.get( "creatorsName" );
        Attribute createTimestampAttrAfter = subschemaSubentry.get( "createTimestamp" );
        assertNotNull( creatorsNameAttrAfter );
        assertNotNull( createTimestampAttrAfter );

        Attribute modifiersNameAttrAfter = subschemaSubentry.get( "modifiersName" );
        Attribute modifiersTimestampAttrAfter = subschemaSubentry.get( "modifyTimestamp" );
        assertNotNull( modifiersNameAttrAfter );
        expectedDn = new Dn( getService().getSchemaManager(), "uid=admin,ou=system" );
        assertEquals( expectedDn.getName(), modifiersNameAttrAfter.get() );
        assertNotNull( modifiersTimestampAttrAfter );

        cal = Calendar.getInstance( tz, Locale.ROOT );
        Date modifyTimestampAfter = DateUtils.getDate( modifiersTimestampAttrAfter.getString() );
        assertTrue( modifyTimestampAfter.getTime() <= cal.getTime().getTime() );

        assertTrue( modifyTimestampAfter.getTime() >= modifyTimestamp.getTime() );

        // now let's test the modifiersName update with another user besides
        // the administrator - we'll create a dummy user for that ...

        Entry user = new DefaultEntry(
            "cn=bogus user,ou=system",
            "objectClass: person",
            "sn: bogus",
            "cn: bogus user",
            "userPassword: secret" );

        connection.add( user );

        // now let's get a context for this user

        // now let's add another attribute type definition to the schema but
        // with this newly created user and check that the modifiers name is his
        /*
        substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10001 NAME ( 'bogus2' 'bogusName2' ) " +
            "DESC 'bogus description' SUP name SINGLE-VALUE X-SCHEMA 'nis' )";
        mods[0] = new ModificationItem( ModificationOperation.ADD_ATTRIBUTE,
            new BasicAttribute( "attributeTypes", substrate ) );
        ctx.modifyAttributes( JndiUtils.toName( dn ), mods );

        // now let's verify the new values for the modification attributes

        subentry = this.getSubschemaSubentryAttributes();

        creatorsNameAttrAfter = subentry.get( "creatorsName" );
        createTimestampAttrAfter = subentry.get( "createTimestamp" );
        assertNotNull( creatorsNameAttrAfter );
        assertNotNull( createTimestampAttrAfter );

        modifiersNameAttrAfter = subentry.get( "modifiersName" );
        modifiersTimestampAttrAfter = subentry.get( "modifyTimestamp" );
        assertNotNull( modifiersNameAttrAfter );
        expectedDn = new Dn( "cn=bogus user,ou=system" );
        expectedDn.normalize( servigetService()chemaManager().getNormalizerMapping() );
        assertEquals( expectedDn.getNormName(), modifiersNameAttrAfter.get() );
        assertNotNull( modifiersTimestampAttrAfter );

        cal = Calendar.getInstance( tz );
        modifyTimestamp = DateUtils.getDate( ( String ) modifyTimestampAttr.get() );
        modifyTimestampAfter = DateUtils.getDate( ( String ) modifiersTimestampAttrAfter.get() );
        assertTrue( modifyTimestampAfter.getTime() <= cal.getTime().getTime() );
        assertTrue( modifyTimestampAfter.getTime() >= modifyTimestamp.getTime() );
        */
    }


    // -----------------------------------------------------------------------
    // Private Utility Methods
    // -----------------------------------------------------------------------

    private void modify( ModificationOperation op, List<String> descriptions, String opAttr ) throws Exception
    {
        Dn dn = new Dn( subschemaSubentryDn );

        // Uses ModificationItem to keep the modification ordering
        Modification[] modifications = new DefaultModification[descriptions.size()];
        int i = 0;

        for ( String description : descriptions )
        {
            modifications[i++] = new DefaultModification( op,
                new DefaultAttribute( opAttr, description ) );
        }

        connection.modify( dn, modifications );
    }


    private void enableSchema( String schemaName ) throws Exception
    {
        // now enable the test schema
        Modification mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            new DefaultAttribute( "m-disabled", "FALSE" ) );
        connection.modify( "cn=" + schemaName + ",ou=schema", mod );
    }


    private void disableSchema( String schemaName ) throws Exception
    {
        // now enable the test schema
        Modification mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            new DefaultAttribute( "m-disabled", "TRUE" ) );
        connection.modify( "cn=" + schemaName + ",ou=schema", mod );
    }


    private String getByteCode( String resource ) throws IOException
    {
        try ( InputStream in = getClass().getResourceAsStream( resource );
            ByteArrayOutputStream out = new ByteArrayOutputStream() )
        {

            while ( in.available() > 0 )
            {
                out.write( in.read() );
            }

            return new String( Base64.encode( out.toByteArray() ) );
        }
    }


    private void updateSSSE() throws Exception
    {
        subschemaSubentry = connection.lookup( subschemaSubentryDn, "*", "attributeTypes", "objectClasses",
            "ldapSyntaxes", "matchingRules",
            "syntaxCheckers", "normalizers", "comparators" );
    }
}
