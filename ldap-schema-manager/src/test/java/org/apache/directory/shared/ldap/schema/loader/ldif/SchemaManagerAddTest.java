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
package org.apache.directory.shared.ldap.schema.loader.ldif;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.NoSuchAttributeException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.schema.comparators.BooleanComparator;
import org.apache.directory.shared.ldap.schema.comparators.ComparableComparator;
import org.apache.directory.shared.ldap.schema.comparators.CsnComparator;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.OctetStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.RegexSyntaxChecker;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * A test class for SchemaManager, testig the addition of a SchemaObject.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaManagerAddTest
{
    // A directory in which the ldif files will be stored
    private static String workingDirectory;

    // The schema repository
    private static File schemaRepository;


    @BeforeClass
    public static void setup() throws Exception
    {
        workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SchemaManagerAddTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        schemaRepository = new File( workingDirectory, "schema" );

        // Cleanup the target directory
        FileUtils.deleteDirectory( schemaRepository );

        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();
    }


    @AfterClass
    public static void cleanup() throws IOException
    {
        // Cleanup the target directory
        FileUtils.deleteDirectory( schemaRepository );
    }


    private SchemaManager loadSystem() throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        String schemaName = "system";

        schemaManager.loadWithDeps( schemaName );

        return schemaManager;
    }


    /**
     * Check if an AT is present in the AT registry
     */
    private boolean isATPresent( SchemaManager schemaManager, String oid )
    {
        try
        {
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );

            return attributeType != null;
        }
        catch ( NoSuchAttributeException nsae )
        {
            return false;
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }


    /**
     * Check if a MR is present in the MR registry
     */
    private boolean isMRPresent( SchemaManager schemaManager, String oid )
    {
        try
        {
            MatchingRule matchingRule = schemaManager.lookupMatchingRuleRegistry( oid );

            return matchingRule != null;
        }
        catch ( NoSuchAttributeException nsae )
        {
            return false;
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }


    /**
     * Check if an OC is present in the OC registry
     */
    private boolean isOCPresent( SchemaManager schemaManager, String oid )
    {
        try
        {
            ObjectClass objectClass = schemaManager.lookupObjectClassRegistry( oid );

            return objectClass != null;
        }
        catch ( NoSuchAttributeException nsae )
        {
            return false;
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }


    /**
     * Check if a S is present in the S registry
     */
    private boolean isSyntaxPresent( SchemaManager schemaManager, String oid )
    {
        try
        {
            LdapSyntax syntax = schemaManager.lookupLdapSyntaxRegistry( oid );

            return syntax != null;
        }
        catch ( NoSuchAttributeException nsae )
        {
            return false;
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }


    //=========================================================================
    // For each test, we will check many different things.
    // If the test is successful, we want to know if the SchemaObject
    // Registry has grown : its size must be one bigger. If the SchemaObject
    // is not loadable, then the GlobalOidRegistry must also have grown.
    //=========================================================================
    // AttributeType addition tests
    //-------------------------------------------------------------------------
    // First, not defined superior
    //-------------------------------------------------------------------------
    /**
     * Try to inject an AttributeType without any superior nor Syntax : it's invalid
     */
    @Test
    public void testAddAttributeTypeNoSupNoSyntaxNoSuperior() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( "2.5.13.1" );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType which is Collective, and userApplication AT
     */
    @Test
    public void testAddAttributeTypeNoSupCollectiveUser() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( "2.5.13.1" );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        attributeType.setUsage( UsageEnum.USER_APPLICATIONS );
        attributeType.setCollective( true );

        // It should not fail
        assertTrue( schemaManager.add( attributeType ) );

        assertTrue( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize + 1, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType which is Collective, but an operational AT
     */
    @Test
    public void testAddAttributeTypeNoSupCollectiveOperational() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( "2.5.13.1" );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        attributeType.setUsage( UsageEnum.DIRECTORY_OPERATION );
        attributeType.setCollective( true );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType which is a NO-USER-MODIFICATION and userApplication
     */
    @Test
    public void testAddAttributeTypeNoSupNoUserModificationUserAplication() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( "2.5.13.1" );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        attributeType.setUsage( UsageEnum.USER_APPLICATIONS );
        attributeType.setUserModifiable( false );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType which is a NO-USER-MODIFICATION and is operational
     */
    @Test
    public void testAddAttributeTypeNoSupNoUserModificationOpAttr() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( "2.5.13.1" );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        attributeType.setUsage( UsageEnum.DISTRIBUTED_OPERATION );
        attributeType.setUserModifiable( false );

        // It should not fail
        assertTrue( schemaManager.add( attributeType ) );

        assertTrue( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize + 1, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType with an invalid EQUALITY MR
     */
    @Test
    public void testAddAttributeTypeNoSupInvalidEqualityMR() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( "0.0" );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        attributeType.setUsage( UsageEnum.USER_APPLICATIONS );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType with an invalid ORDERING MR
     */
    @Test
    public void testAddAttributeTypeNoSupInvalidOrderingMR() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( null );
        attributeType.setOrderingOid( "0.0" );
        attributeType.setSubstringOid( null );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        attributeType.setUsage( UsageEnum.USER_APPLICATIONS );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType with an invalid SUBSTR MR
     */
    @Test
    public void testAddAttributeTypeNoSupInvalidSubstringMR() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( null );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( "0.0" );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        attributeType.setUsage( UsageEnum.USER_APPLICATIONS );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType with valid MRs
     */
    @Test
    public void testAddAttributeTypeNoSupValidMR() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( "2.5.13.1" );
        attributeType.setOrderingOid( "2.5.13.1" );
        attributeType.setSubstringOid( "2.5.13.1" );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        attributeType.setUsage( UsageEnum.USER_APPLICATIONS );

        // It should not fail
        assertTrue( schemaManager.add( attributeType ) );

        assertTrue( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize + 1, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType which already exist
     */
    @Test
    public void testAddAttributeTypeAlreadyExist() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "2.5.18.4" );
        attributeType.setEqualityOid( "2.5.13.1" );
        attributeType.setOrderingOid( "2.5.13.1" );
        attributeType.setSubstringOid( "2.5.13.1" );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        // The AT must be there
        assertTrue( isATPresent( schemaManager, "2.5.18.4" ) );

        // Check that it hasen't changed
        AttributeType original = schemaManager.lookupAttributeTypeRegistry( "2.5.18.4" );
        assertEquals( "distinguishedNameMatch", original.getEqualityOid() );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType with an already attributed name
     */
    @Test
    public void testAddAttributeTypeNameAlreadyExist() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.1.0" );
        attributeType.setEqualityOid( "2.5.13.1" );
        attributeType.setOrderingOid( "2.5.13.1" );
        attributeType.setSubstringOid( "2.5.13.1" );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        attributeType.setNames( "Test", "cn" );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        // The AT must not be there
        assertFalse( isATPresent( schemaManager, "1.1.1.0" ) );

        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType with an ObjectClass name
     */
    @Test
    public void testAddAttributeTypeNameOfAnObjectClass() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.1.0" );
        attributeType.setEqualityOid( "2.5.13.1" );
        attributeType.setOrderingOid( "2.5.13.1" );
        attributeType.setSubstringOid( "2.5.13.1" );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        attributeType.setNames( "Test", "referral" );

        // It should be ok
        assertTrue( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 0, errors.size() );

        // The AT must be present
        assertTrue( isATPresent( schemaManager, "1.1.1.0" ) );

        assertEquals( atrSize + 1, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );

        AttributeType added = schemaManager.lookupAttributeTypeRegistry( "referral" );
        assertNotNull( added );
        assertEquals( "1.1.1.0", added.getOid() );
        assertTrue( added.getNames().contains( "referral" ) );
    }


    //-------------------------------------------------------------------------
    // Then, with a superior
    //-------------------------------------------------------------------------
    /**
     * Try to inject an AttributeType with a superior and no Syntax : it should
     * take its superior' syntax and MR
     */
    @Test
    public void testAddAttributeTypeSupNoSyntaxNoSuperior() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( null );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );
        attributeType.setSuperiorOid( "2.5.18.4" );
        attributeType.setUsage( UsageEnum.DIRECTORY_OPERATION );

        // It should not fail
        assertTrue( schemaManager.add( attributeType ) );

        AttributeType result = schemaManager.lookupAttributeTypeRegistry( "1.1.0" );

        assertEquals( "1.3.6.1.4.1.1466.115.121.1.12", result.getSyntaxOid() );
        assertEquals( "2.5.13.1", result.getEqualityOid() );
        assertEquals( atrSize + 1, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType with a superior and different USAGE
     */
    @Test
    public void testAddAttributeTypeSupDifferentUsage() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( null );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );
        attributeType.setSuperiorOid( "2.5.18.4" );
        attributeType.setUsage( UsageEnum.DISTRIBUTED_OPERATION );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType with itself as a superior
     */
    @Test
    public void testAddAttributeTypeSupWithOwnSup() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( null );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );
        attributeType.setSuperiorOid( "1.1.0" );
        attributeType.setUsage( UsageEnum.DISTRIBUTED_OPERATION );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject an AttributeType with a bad superior
     */
    @Test
    public void testAddAttributeTypeSupBadSup() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( null );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );
        attributeType.setSuperiorOid( "0.0" );
        attributeType.setUsage( UsageEnum.DISTRIBUTED_OPERATION );

        // It should fail
        assertFalse( schemaManager.add( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isATPresent( schemaManager, "1.1.0" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    //=========================================================================
    // Comparator addition tests
    //-------------------------------------------------------------------------
    @Test
    public void testAddNewComparator() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ctrSize = schemaManager.getComparatorRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        String oid = "0.0.0";
        LdapComparator<?> lc = new BooleanComparator( oid );

        assertTrue( schemaManager.add( lc ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 0, errors.size() );

        assertEquals( ctrSize + 1, schemaManager.getComparatorRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        LdapComparator<?> added = schemaManager.lookupComparatorRegistry( oid );

        assertNotNull( added );
        assertEquals( lc.getClass().getName(), added.getFqcn() );
    }


    @Test
    public void testAddAlreadyExistingComparator() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ctrSize = schemaManager.getComparatorRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        String oid = "0.0.0";
        LdapComparator<?> bc = new BooleanComparator( oid );

        assertTrue( schemaManager.add( bc ) );

        LdapComparator<?> added = schemaManager.lookupComparatorRegistry( oid );

        assertNotNull( added );
        assertEquals( bc.getClass().getName(), added.getFqcn() );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 0, errors.size() );
        assertEquals( ctrSize + 1, schemaManager.getComparatorRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        LdapComparator<?> lc = new CsnComparator( oid );

        assertFalse( schemaManager.add( lc ) );

        errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );

        assertEquals( ctrSize + 1, schemaManager.getComparatorRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        added = schemaManager.lookupComparatorRegistry( oid );

        assertNotNull( added );
        assertEquals( bc.getClass().getName(), added.getFqcn() );
    }


    /**
     * Test that we can't add two comparators with the same class code.
     */
    @Test
    public void testAddComparatorWithWrongFQCN() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ctrSize = schemaManager.getComparatorRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        String oid = "0.0.0";
        LdapComparator<?> lc = new BooleanComparator( oid );

        // using java.sql.ResultSet cause it is very unlikely to get loaded
        // in ADS, as the FQCN is not the one expected
        lc.setFqcn( "java.sql.ResultSet" );

        assertFalse( schemaManager.add( lc ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );

        assertEquals( ctrSize, schemaManager.getComparatorRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        try
        {
            schemaManager.lookupComparatorRegistry( oid );
            fail();
        }
        catch ( Exception e )
        {
            // Expected
            assertTrue( true );
        }
    }


    //=========================================================================
    // DITContentRule addition tests
    //-------------------------------------------------------------------------
    // TODO

    //=========================================================================
    // DITStructureRule addition tests
    //-------------------------------------------------------------------------
    // TODO

    //=========================================================================
    // MatchingRule addition tests
    //-------------------------------------------------------------------------
    /**
     * Try to inject a new MatchingRule
     */
    @Test
    public void testAddValidMatchingRule() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        MatchingRule matchingRule = new MatchingRule( "1.1.0" );
        matchingRule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );

        // It should not fail
        assertTrue( schemaManager.add( matchingRule ) );

        assertTrue( isMRPresent( schemaManager, "1.1.0" ) );

        // The C and N must have default values
        MatchingRule added = schemaManager.lookupMatchingRuleRegistry( "1.1.0" );

        assertEquals( NoOpNormalizer.class.getName(), added.getNormalizer().getClass().getName() );
        assertEquals( ComparableComparator.class.getName(), added.getLdapComparator().getClass().getName() );

        assertEquals( mrrSize + 1, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject a new MatchingRule without a syntax
     */
    @Test
    public void testAddMatchingRuleNoSyntax() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        MatchingRule matchingRule = new MatchingRule( "1.1.0" );

        // It should fail (no syntax)
        assertFalse( schemaManager.add( matchingRule ) );

        List<Throwable> errors = schemaManager.getErrors();

        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );
        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isMRPresent( schemaManager, "1.1.0" ) );

        assertEquals( mrrSize, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject a new MatchingRule with an existing OID
     */
    @Test
    public void testAddMatchingRuleExistingOID() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        MatchingRule matchingRule = new MatchingRule( "2.5.13.0" );
        matchingRule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );

        // It should fail (oid already registered)
        assertFalse( schemaManager.add( matchingRule ) );

        List<Throwable> errors = schemaManager.getErrors();

        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );
        assertTrue( error instanceof LdapSchemaViolationException );

        // Check that the existing MR has not been replaced
        assertTrue( isMRPresent( schemaManager, "2.5.13.0" ) );
        MatchingRule existing = schemaManager.lookupMatchingRuleRegistry( "2.5.13.0" );

        assertEquals( "objectIdentifierMatch", existing.getName() );

        assertEquals( mrrSize, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject a new MatchingRule with an existing name
     */
    @Test
    public void testAddMatchingRuleExistingName() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        MatchingRule matchingRule = new MatchingRule( "1.1.0" );
        matchingRule.setNames( "Test", "objectIdentifierMatch" );
        matchingRule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );

        // It should fail (name already registered)
        assertFalse( schemaManager.add( matchingRule ) );

        List<Throwable> errors = schemaManager.getErrors();

        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );
        assertTrue( error instanceof LdapSchemaViolationException );

        assertEquals( mrrSize, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject a new MatchingRule with an existing AT name 
     */
    @Test
    public void testAddMatchingRuleExistingATName() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        MatchingRule matchingRule = new MatchingRule( "1.1.0" );
        matchingRule.setNames( "Test", "cn" );
        matchingRule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );

        // It should not fail
        assertTrue( schemaManager.add( matchingRule ) );

        List<Throwable> errors = schemaManager.getErrors();

        assertEquals( 0, errors.size() );

        // Check that the new MR has been injected
        assertTrue( isMRPresent( schemaManager, "1.1.0" ) );
        MatchingRule added = schemaManager.lookupMatchingRuleRegistry( "1.1.0" );

        assertTrue( added.getNames().contains( "cn" ) );
        assertTrue( added.getNames().contains( "Test" ) );

        assertEquals( mrrSize + 1, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject a new MatchingRule with a not existing Syntax 
     */
    @Test
    public void testAddMatchingRuleNotExistingSyntax() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        MatchingRule matchingRule = new MatchingRule( "1.1.0" );
        matchingRule.setNames( "Test" );
        matchingRule.setSyntaxOid( "1.1.1" );

        // It should fail
        assertFalse( schemaManager.add( matchingRule ) );

        List<Throwable> errors = schemaManager.getErrors();

        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );
        assertTrue( error instanceof LdapSchemaViolationException );

        assertEquals( mrrSize, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject a new MatchingRule with an existing AT name 
     */
    @Test
    public void testAddMatchingRuleNotExistingSchema() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        MatchingRule matchingRule = new MatchingRule( "1.1.0" );
        matchingRule.setNames( "Test" );
        matchingRule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        matchingRule.setSchemaName( "bad" );

        // It should fail
        assertFalse( schemaManager.add( matchingRule ) );

        List<Throwable> errors = schemaManager.getErrors();

        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );
        assertTrue( error instanceof LdapOperationNotSupportedException );

        // Check that the new MR has been injected
        assertFalse( isMRPresent( schemaManager, "1.1.0" ) );

        assertEquals( mrrSize, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    //=========================================================================
    // MatchingRuleUse addition tests
    //-------------------------------------------------------------------------
    // TODO

    //=========================================================================
    // NameForm addition tests
    //-------------------------------------------------------------------------
    // TODO

    //=========================================================================
    // Normalizer addition tests
    //-------------------------------------------------------------------------
    @Test
    public void testAddNewNormalizer() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int nrSize = schemaManager.getNormalizerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        String oid = "0.0.0";
        Normalizer normalizer = new NoOpNormalizer( oid );

        assertTrue( schemaManager.add( normalizer ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 0, errors.size() );

        assertEquals( nrSize + 1, schemaManager.getNormalizerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        Normalizer added = schemaManager.lookupNormalizerRegistry( oid );

        assertNotNull( added );
        assertEquals( normalizer.getClass().getName(), added.getFqcn() );
    }


    @Test
    public void testAddAlreadyExistingNormalizer() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int nrSize = schemaManager.getNormalizerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        String oid = "0.0.0";
        Normalizer normalizer = new NoOpNormalizer( oid );

        assertTrue( schemaManager.add( normalizer ) );

        Normalizer added = schemaManager.lookupNormalizerRegistry( oid );

        assertNotNull( added );
        assertEquals( normalizer.getClass().getName(), added.getFqcn() );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 0, errors.size() );
        assertEquals( nrSize + 1, schemaManager.getNormalizerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        Normalizer normalizer2 = new NoOpNormalizer( oid );

        assertFalse( schemaManager.add( normalizer2 ) );

        errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );

        assertEquals( nrSize + 1, schemaManager.getNormalizerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        added = schemaManager.lookupNormalizerRegistry( oid );

        assertNotNull( added );
        assertEquals( normalizer.getClass().getName(), added.getFqcn() );
    }


    /**
     * Test that we can't add two Normalizers with the same class code.
     */
    @Test
    public void testAddNormalizerWithWrongFQCN() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int nrSize = schemaManager.getNormalizerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        String oid = "0.0.0";
        Normalizer normalizer = new NoOpNormalizer( oid );

        // using java.sql.ResultSet cause it is very unlikely to get loaded
        // in ADS, as the FQCN is not the one expected
        normalizer.setFqcn( "java.sql.ResultSet" );

        assertFalse( schemaManager.add( normalizer ) );

        List<Throwable> errors = schemaManager.getErrors();
        errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );

        assertEquals( nrSize, schemaManager.getNormalizerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        try
        {
            schemaManager.lookupNormalizerRegistry( oid );
            fail();
        }
        catch ( Exception e )
        {
            // Expected
            assertTrue( true );
        }
    }


    //=========================================================================
    // ObjectClass addition tests
    //-------------------------------------------------------------------------
    //-------------------------------------------------------------------------
    // First, not defined superior
    //-------------------------------------------------------------------------
    /**
     * Addition of a valid OC
     */
    @Test
    public void testAddObjectClassNoSuperiorValid() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );

        assertTrue( schemaManager.add( objectClass ) );

        assertEquals( 0, schemaManager.getErrors().size() );

        ObjectClass added = schemaManager.lookupObjectClassRegistry( "1.1.1" );

        assertNotNull( added );

        assertEquals( ocrSize + 1, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with an existing OID
     */
    @Test
    public void testAddObjectClassNoSuperiorWithExistingOid() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "2.5.17.0" );

        assertFalse( schemaManager.add( objectClass ) );

        assertEquals( 1, schemaManager.getErrors().size() );
        Throwable error = schemaManager.getErrors().get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        ObjectClass added = schemaManager.lookupObjectClassRegistry( "2.5.17.0" );

        assertNotNull( added );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with an existing OC name
     */
    @Test
    public void testAddObjectClassNoSuperiorWithExistingOCName() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.0" );
        objectClass.setNames( "Test", "referral" );

        assertFalse( schemaManager.add( objectClass ) );

        assertEquals( 1, schemaManager.getErrors().size() );
        Throwable error = schemaManager.getErrors().get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.0" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with an AT name
     */
    @Test
    public void testAddObjectClassNoSuperiorWithATName() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.setNames( "Test", "cn" );

        assertTrue( schemaManager.add( objectClass ) );

        assertEquals( 0, schemaManager.getErrors().size() );

        ObjectClass added = schemaManager.lookupObjectClassRegistry( "1.1.1" );

        assertNotNull( added );
        assertTrue( added.getNames().contains( "Test" ) );
        assertTrue( added.getNames().contains( "cn" ) );

        assertEquals( ocrSize + 1, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with not existing AT in MAY
     */
    @Test
    public void testAddObjectClassNoSuperiorNonExistingAtInMay() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.addMayAttributeTypeOids( "cn", "none", "sn" );

        assertFalse( schemaManager.add( objectClass ) );

        assertEquals( 1, schemaManager.getErrors().size() );
        Throwable error = schemaManager.getErrors().get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with not existing AT in MUST
     */
    @Test
    public void testAddObjectClassNoSuperiorNonExistingAtInMust() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.addMustAttributeTypeOids( "cn", "none", "sn" );

        assertFalse( schemaManager.add( objectClass ) );

        assertEquals( 1, schemaManager.getErrors().size() );
        Throwable error = schemaManager.getErrors().get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with an AT present more than once in MAY
     */
    @Test
    public void testAddObjectClassNoSuperiorATMoreThanOnceInMay() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.addMayAttributeTypeOids( "cn", "ref", "commonName" );

        assertFalse( schemaManager.add( objectClass ) );

        assertEquals( 1, schemaManager.getErrors().size() );
        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with an AT present more than once in MUST
     */
    @Test
    public void testAddObjectClassNoSuperiorATMoreThanOnceInMust() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.addMustAttributeTypeOids( "cn", "ref", "2.5.4.3" );

        assertFalse( schemaManager.add( objectClass ) );

        assertEquals( 1, schemaManager.getErrors().size() );
        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with an AT present in MUST and MAY.
     */
    @Test
    public void testAddObjectClassNoSuperiorATInMustAndMay() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.addMustAttributeTypeOids( "cn", "ref" );
        objectClass.addMayAttributeTypeOids( "2.5.4.3" );

        // Same AT i MAY and MUST : should fail
        assertFalse( schemaManager.add( objectClass ) );

        assertEquals( 1, schemaManager.getErrors().size() );
        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    //-------------------------------------------------------------------------
    // Then, with superiors
    //-------------------------------------------------------------------------
    /**
     * Addition of a valid OC with some superiors
     */
    @Test
    public void testAddObjectClassSuperiorsValid() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.setNames( "Test" );
        objectClass.setType( ObjectClassTypeEnum.STRUCTURAL );
        objectClass.addSuperiorOids( "alias", "referral", "top" );

        assertTrue( schemaManager.add( objectClass ) );

        assertEquals( 0, schemaManager.getErrors().size() );

        ObjectClass added = schemaManager.lookupObjectClassRegistry( "1.1.1" );

        assertNotNull( added );
        assertTrue( added.getNames().contains( "Test" ) );
        assertNotNull( added.getSuperiors() );
        assertEquals( 3, added.getSuperiors().size() );

        Set<String> expectedSups = new HashSet<String>();
        expectedSups.add( "alias" );
        expectedSups.add( "referral" );
        expectedSups.add( "top" );

        for ( ObjectClass addedOC : added.getSuperiors() )
        {
            assertTrue( expectedSups.contains( addedOC.getName() ) );
            expectedSups.remove( addedOC.getName() );
        }

        assertEquals( ocrSize + 1, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with itself in the SUP list
     */
    @Test
    public void testAddObjectClassSuperiorsWithCycle() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.setNames( "Test" );
        objectClass.setType( ObjectClassTypeEnum.STRUCTURAL );
        objectClass.addSuperiorOids( "alias", "Test", "referral" );

        assertFalse( schemaManager.add( objectClass ) );

        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with the same OC more than once in SUP
     */
    @Test
    public void testAddObjectClassSuperiorsOcMoreThanOnceInSup() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        ObjectClass objectClass = new ObjectClass( "1.1.1" );

        objectClass.setNames( "Test" );
        objectClass.setType( ObjectClassTypeEnum.STRUCTURAL );
        objectClass.addSuperiorOids( "alias", "referral", "2.5.6.1" );

        assertFalse( schemaManager.add( objectClass ) );

        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with a non existing OC in SUP
     */
    @Test
    public void testAddObjectClassSuperiorsNonExistingOCInSup() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.setNames( "Test" );
        objectClass.setType( ObjectClassTypeEnum.STRUCTURAL );
        objectClass.addSuperiorOids( "alias", "refessal" );

        assertFalse( schemaManager.add( objectClass ) );

        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an ABSTRACT OC with some AUXILIARY superior
     */
    @Test
    public void testAddObjectClassSuperiorsAbstractWithAuxiliaryInSup() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.setNames( "Test" );
        objectClass.setType( ObjectClassTypeEnum.ABSTRACT );
        objectClass.addSuperiorOids( "extensibleObject" );

        assertFalse( schemaManager.add( objectClass ) );

        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an ABSTRACT OC with some STRUCTURAL superior
     */
    @Test
    public void testAddObjectClassSuperiorsAbstractWithStructuralInSup() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.setNames( "Test" );
        objectClass.setType( ObjectClassTypeEnum.ABSTRACT );
        objectClass.addSuperiorOids( "referral" );

        assertFalse( schemaManager.add( objectClass ) );

        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an AUXILIARY OC with some STRUCTURAL superior
     */
    @Test
    public void testAddObjectClassSuperiorsAuxiliaryWithStructuralInSup() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.setNames( "Test" );
        objectClass.setType( ObjectClassTypeEnum.AUXILIARY );
        objectClass.addSuperiorOids( "referral" );

        assertFalse( schemaManager.add( objectClass ) );

        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an STRUCTURAL OC with some AUXILIARY superior
     */
    @Test
    public void testAddObjectClassSuperiorsStructuralWithAuxiliaryInSup() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.setNames( "Test" );
        objectClass.setType( ObjectClassTypeEnum.STRUCTURAL );
        objectClass.addSuperiorOids( "extensibleObject" );

        assertFalse( schemaManager.add( objectClass ) );

        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with some AT present in MUST and in MAY in one of its
     * superior
     */
    @Test
    public void testAddObjectClassSuperiorsATInMustPresentInSuperiorsMay() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.setNames( "Test" );
        objectClass.setType( ObjectClassTypeEnum.STRUCTURAL );
        objectClass.addSuperiorOids( "alias", "OpenLDAProotDSE" );
        objectClass.addMustAttributeTypeOids( "aliasedObjectName", "cn" );

        assertTrue( schemaManager.add( objectClass ) );

        assertEquals( 0, schemaManager.getErrors().size() );

        ObjectClass added = schemaManager.lookupObjectClassRegistry( "1.1.1" );

        assertNotNull( added );
        assertTrue( added.getNames().contains( "Test" ) );
        assertNotNull( added.getSuperiors() );
        assertEquals( 2, added.getSuperiors().size() );

        Set<String> expectedSups = new HashSet<String>();
        expectedSups.add( "alias" );
        expectedSups.add( "OpenLDAProotDSE" );

        for ( ObjectClass addedOC : added.getSuperiors() )
        {
            assertTrue( expectedSups.contains( addedOC.getName() ) );
            expectedSups.remove( addedOC.getName() );
        }

        assertEquals( ocrSize + 1, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Addition of an OC with some AT present in MAY and in MUST in one of its
     * superior : not allowed
     */
    @Test
    public void testAddObjectClassSuperiorsATInMayPresentInSuperiorsMust() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int ocrSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        ObjectClass objectClass = new ObjectClass( "1.1.1" );
        objectClass.setNames( "Test" );
        objectClass.setType( ObjectClassTypeEnum.STRUCTURAL );
        objectClass.addSuperiorOids( "alias", "OpenLDAProotDSE" );
        objectClass.addMayAttributeTypeOids( "aliasedObjectName", "cn" );

        assertFalse( schemaManager.add( objectClass ) );

        assertEquals( 1, schemaManager.getErrors().size() );

        assertTrue( schemaManager.getErrors().get( 0 ) instanceof LdapSchemaViolationException );

        assertFalse( isOCPresent( schemaManager, "1.1.1" ) );

        assertEquals( ocrSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    //=========================================================================
    // Syntax addition tests
    //-------------------------------------------------------------------------
    /**
     * Try to inject a new valid Syntax, with no SC : the associated SC 
     * will be the default OctetString SC
     */
    @Test
    public void testAddValidSyntax() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int sSize = schemaManager.getLdapSyntaxRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        LdapSyntax syntax = new LdapSyntax( "1.1.0" );

        // It should not fail
        assertTrue( schemaManager.add( syntax ) );

        LdapSyntax added = schemaManager.lookupLdapSyntaxRegistry( "1.1.0" );

        assertNotNull( added );
        assertEquals( OctetStringSyntaxChecker.class.getName(), added.getSyntaxChecker().getClass().getName() );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 0, errors.size() );

        assertTrue( isSyntaxPresent( schemaManager, "1.1.0" ) );
        assertEquals( sSize + 1, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize + 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Try to inject a Syntax with an existing OID
     */
    @Test
    public void testAddSyntaxExistingOid() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int sSize = schemaManager.getLdapSyntaxRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        LdapSyntax syntax = new LdapSyntax( "2.5.4.3" );

        // It should fail
        assertFalse( schemaManager.add( syntax ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );
        Throwable error = errors.get( 0 );

        assertTrue( error instanceof LdapSchemaViolationException );
        assertEquals( sSize, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    //=========================================================================
    // SyntaxChecker addition tests
    //-------------------------------------------------------------------------
    @Test
    public void testAddNewSyntaxChecker() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int nrSize = schemaManager.getSyntaxCheckerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        String oid = "0.0.0";
        SyntaxChecker syntaxChecker = new RegexSyntaxChecker( oid );

        assertTrue( schemaManager.add( syntaxChecker ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 0, errors.size() );

        assertEquals( nrSize + 1, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        SyntaxChecker added = schemaManager.lookupSyntaxCheckerRegistry( oid );

        assertNotNull( added );
        assertEquals( syntaxChecker.getClass().getName(), added.getFqcn() );
    }


    @Test
    public void testAddAlreadyExistingSyntaxChecker() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int nrSize = schemaManager.getSyntaxCheckerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        String oid = "0.0.0";
        SyntaxChecker syntaxChecker = new RegexSyntaxChecker( oid );

        assertTrue( schemaManager.add( syntaxChecker ) );

        SyntaxChecker added = schemaManager.lookupSyntaxCheckerRegistry( oid );

        assertNotNull( added );
        assertEquals( syntaxChecker.getClass().getName(), added.getFqcn() );

        List<Throwable> errors = schemaManager.getErrors();
        assertEquals( 0, errors.size() );
        assertEquals( nrSize + 1, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        SyntaxChecker syntaxChecker2 = new RegexSyntaxChecker( oid );

        assertFalse( schemaManager.add( syntaxChecker2 ) );

        errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );

        assertEquals( nrSize + 1, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        added = schemaManager.lookupSyntaxCheckerRegistry( oid );

        assertNotNull( added );
        assertEquals( syntaxChecker.getClass().getName(), added.getFqcn() );
    }


    /**
     * Test that we can't add two SyntaxCheckers with the same class code.
     */
    @Test
    public void testAddSyntaxCheckerWithWrongFQCN() throws Exception
    {
        SchemaManager schemaManager = loadSystem();
        int nrSize = schemaManager.getSyntaxCheckerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        String oid = "0.0.0";
        SyntaxChecker syntaxChecker = new RegexSyntaxChecker( oid );

        // using java.sql.ResultSet cause it is very unlikely to get loaded
        // in ADS, as the FQCN is not the one expected
        syntaxChecker.setFqcn( "java.sql.ResultSet" );

        assertFalse( schemaManager.add( syntaxChecker ) );

        List<Throwable> errors = schemaManager.getErrors();
        errors = schemaManager.getErrors();
        assertEquals( 1, errors.size() );

        assertEquals( nrSize, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        try
        {
            schemaManager.lookupSyntaxCheckerRegistry( oid );
            fail();
        }
        catch ( Exception e )
        {
            // Expected
            assertTrue( true );
        }
    }
}
