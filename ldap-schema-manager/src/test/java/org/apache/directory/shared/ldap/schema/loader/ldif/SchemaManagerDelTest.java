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
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.NoSuchAttributeException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.comparators.BooleanComparator;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.BooleanNormalizer;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.BooleanSyntaxChecker;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * A test class for SchemaManager, testing the deletion of a SchemaObject.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaManagerDelTest
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
            String path = SchemaManagerDelTest.class.getResource( "" ).getPath();
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


    private SchemaManager loadSchema( String schemaName ) throws Exception
    {
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        schemaManager.loadWithDeps( schemaName );

        return schemaManager;
    }


    private boolean isAttributeTypePresent( SchemaManager schemaManager, String oid )
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

    
    private boolean isComparatorPresent( SchemaManager schemaManager, String oid )
    {
        try
        {
            LdapComparator<?> comparator = schemaManager.lookupComparatorRegistry( oid );

            return comparator != null;
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

    
    private boolean isNormalizerPresent( SchemaManager schemaManager, String oid )
    {
        try
        {
            Normalizer normalizer = schemaManager.lookupNormalizerRegistry( oid );

            return normalizer != null;
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


    private boolean isMatchingRulePresent( SchemaManager schemaManager, String oid )
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

    
    private boolean isSyntaxCheckerPresent( SchemaManager schemaManager, String oid )
    {
        try
        {
            SyntaxChecker syntaxChecker = schemaManager.lookupSyntaxCheckerRegistry( oid );

            return syntaxChecker != null;
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
    // Registry has shrunk : its size must be one lower. If the SchemaObject
    // is not loadable, then the GlobalOidRegistry must also have grown.
    //=========================================================================
    // AttributeType deletion tests
    //-------------------------------------------------------------------------
    // First, not defined descendant
    //-------------------------------------------------------------------------
    /**
     * Try to delete an AttributeType not existing in the schemaManager
     */
    @Test
    public void testDeleteNonExistingAttributeType() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "Core" );
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "1.1.0" );
        attributeType.setEqualityOid( "2.5.13.1" );
        attributeType.setOrderingOid( null );
        attributeType.setSubstringOid( null );

        // It should fail
        assertFalse( schemaManager.delete( attributeType ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );

        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Delete an existing AT not referenced by any object
     */
    @Test
    public void testDeleteExistingAttributeType() throws Exception
    {
        // First inject such an AT
        SchemaManager schemaManager = loadSchema( "Core" );
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        AttributeType attributeType = new AttributeType( "generationQualifier" );
        attributeType.setOid( "2.5.4.44" );

        // It should not fail
        assertTrue( schemaManager.delete( attributeType ) );

        assertFalse( isAttributeTypePresent( schemaManager, "generationQualifier" ) );
        assertEquals( atrSize - 1, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Delete an existing AT referenced by some other OC
     */
    @Test
    public void testDeleteExistingAttributeTypeUsedByOC() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "Core" );

        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        // Try to delete an AT which is referenced by at least one OC
        // (modifiersName has one descendant : schemaModifiersName)
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( "cn" );

        // It should fail
        assertFalse( schemaManager.delete( attributeType ) );

        assertTrue( isAttributeTypePresent( schemaManager, "cn" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Delete an existing AT stored in some disabled schema
     */
    @Test
    public void testDelAttributeTypeFromDisabledSchema() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "Core" );

        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        // Try to delete an AT which is contained by a disabled schema
        AttributeType attributeType = new AttributeType( "gecos" );
        attributeType.setOid( "1.3.6.1.1.1.1.2" );

        // It should fail
        assertFalse( schemaManager.delete( attributeType ) );

        assertFalse( isAttributeTypePresent( schemaManager, "gecos" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Delete an existing AT referenced by some descendant
     */
    @Test
    public void testDeleteExistingAttributeTypeUsedByDescendant() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "Apache" );

        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        // Try to delete an AT which has descendant 
        // (modifiersName has one descendant : schemaModifiersName)
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( "modifiersName" );

        // It should fail
        assertFalse( schemaManager.delete( attributeType ) );

        assertTrue( isAttributeTypePresent( schemaManager, "modifiersName" ) );
        assertEquals( atrSize, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    //=========================================================================
    // Comparator deletion tests
    //-------------------------------------------------------------------------

    @Test
    public void testDeleteExistingComparator() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int ctrSize = schemaManager.getComparatorRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        LdapComparator<?> lc = new BooleanComparator( "0.1.1" );
        assertTrue( schemaManager.add( lc ) );

        assertEquals( ctrSize + 1, schemaManager.getComparatorRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        lc = schemaManager.lookupComparatorRegistry( "0.1.1" );
        assertNotNull( lc );
        assertTrue( schemaManager.delete( lc ) );
        
        try
        {
            schemaManager.lookupComparatorRegistry( "0.1.1" );
            fail();
        }
        catch ( Exception e )
        {
            // expected
        }

        assertEquals( ctrSize, schemaManager.getComparatorRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    @Test
    public void testDeleteNonExistingComparator() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int ctrSize = schemaManager.getComparatorRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        LdapComparator<?> lc = new BooleanComparator( "0.0" );
        assertFalse( schemaManager.delete( lc ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );

        assertEquals( ctrSize, schemaManager.getComparatorRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    @Test
    public void testDeleteExistingComaparatorUsedByMatchingRule() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int ctrSize = schemaManager.getComparatorRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        LdapComparator<?> lc = schemaManager.lookupComparatorRegistry( "2.5.13.0" );
        
        // shouldn't be deleted cause there is a MR associated with it
        assertFalse( schemaManager.delete( lc ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );
        assertTrue( errors.get( 0 ) instanceof LdapSchemaViolationException );

        assertNotNull( schemaManager.lookupComparatorRegistry( "2.5.13.0" ) );
        assertEquals( ctrSize, schemaManager.getComparatorRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Check that a Comparator which has been used by a deleted MatchingRule
     * can be removed
     */
    @Test
    public void testDeleteExistingComparatorUsedByRemovedMatchingRule() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int ctrSize = schemaManager.getComparatorRegistry().size();
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        String OID = "2.5.13.33";

        // Check that the MR and C are present
        assertTrue( isMatchingRulePresent( schemaManager, OID ) );
        assertTrue( isComparatorPresent( schemaManager, OID ) );

        // Now try to remove the C
        LdapComparator<?> lc = schemaManager.lookupComparatorRegistry( OID );
        
        // shouldn't be deleted cause there is a MR associated with it
        assertFalse( schemaManager.delete( lc ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );
        assertTrue( errors.get( 0 ) instanceof LdapSchemaViolationException );

        // Now delete the using MR : it should be OK
        MatchingRule mr = new MatchingRule( OID );
        assertTrue( schemaManager.delete( mr ) );

        assertEquals( mrrSize - 1, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );

        assertFalse( isMatchingRulePresent( schemaManager, OID ) );
        
        // and try to delete the Comparator again
        assertTrue( schemaManager.delete( lc ) );

        assertFalse( isComparatorPresent( schemaManager, OID ) );
        assertEquals( ctrSize - 1, schemaManager.getComparatorRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );
    }
    

    //=========================================================================
    // DITContentRule deletion tests
    //-------------------------------------------------------------------------
    // TODO

    //=========================================================================
    // DITStructureRule deletion tests
    //-------------------------------------------------------------------------
    // TODO

    //=========================================================================
    // MatchingRule deletion tests
    //-------------------------------------------------------------------------
    
    @Test
    public void testDeleteExistingMatchingRule() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int mrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        MatchingRule mr = new MatchingRule( "2.5.13.33" );
        assertTrue( schemaManager.delete( mr ) );
        
        assertEquals( mrSize - 1, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );
    }

    
    @Test
    public void testDeleteNonExistingMatchingRule() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int mrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        MatchingRule mr = new MatchingRule( "0.1.1" );
        assertFalse( schemaManager.delete( mr ) );
        
        assertEquals( mrSize, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }

    
    @Test
    public void testDeleteExistingMatchingRuleUsedByAttributeType() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int mrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        // AT with OID 2.5.18.4 has syntax 1.3.6.1.4.1.1466.115.121.1.12 which is used by MR 2.5.13.1
        MatchingRule mr = new MatchingRule( "2.5.13.1" );
        assertFalse( schemaManager.delete( mr ) );
        
        assertEquals( mrSize, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }

    
    /**
     * Check that a MatcingRule which has been used by a deleted AttributeType
     * can be removed
     */
    @Test
    public void testDeleteExistingMatchingRuleUsedByRemovedAttributeType() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        String AT_OID = "2.5.18.9";
        String MR_OID = "2.5.13.13";

        // Check that the AT and MR are present
        assertTrue( isAttributeTypePresent( schemaManager, AT_OID ) );
        assertTrue( isMatchingRulePresent( schemaManager, MR_OID ) );

        // Now try to remove the MR
        MatchingRule matchingRule = schemaManager.lookupMatchingRuleRegistry( MR_OID );
        
        // shouldn't be deleted cause there is a AT associated with it
        assertFalse( schemaManager.delete( matchingRule ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );
        assertTrue( errors.get( 0 ) instanceof LdapSchemaViolationException );

        // Now delete the using AT : it should be OK
        AttributeType at = new AttributeType( AT_OID );
        assertTrue( schemaManager.delete( at ) );

        assertEquals( atrSize - 1, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );

        assertFalse( isAttributeTypePresent( schemaManager, AT_OID ) );
        
        // and try to delete the MatchingRule again
        assertTrue( schemaManager.delete( matchingRule ) );

        assertFalse( isMatchingRulePresent( schemaManager, MR_OID ) );
        assertEquals( mrrSize - 1, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize - 2, schemaManager.getGlobalOidRegistry().size() );
    }


    //=========================================================================
    // MatchingRuleUse deletion tests
    //-------------------------------------------------------------------------
    // TODO

    //=========================================================================
    // NameForm deletion tests
    //-------------------------------------------------------------------------
    // TODO

    //=========================================================================
    // Normalizer deletion tests
    //-------------------------------------------------------------------------

    @Test
    public void testDeleteExistingNormalizer() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int nrSize = schemaManager.getNormalizerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        Normalizer nr = new BooleanNormalizer();
        nr.setOid( "0.1.1" );
        assertTrue( schemaManager.add( nr ) );

        assertEquals( nrSize + 1, schemaManager.getNormalizerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        // FIXME this lookup is failing ! but it shouldn't be
        nr = schemaManager.lookupNormalizerRegistry( "0.1.1" );
        assertNotNull( nr );
        assertTrue( schemaManager.delete( nr ) );

        try
        {
            schemaManager.lookupNormalizerRegistry( "0.1.1" );
            fail();
        }
        catch ( Exception e )
        {
            // expected
        }

        assertEquals( nrSize, schemaManager.getNormalizerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }
    
    
    @Test
    public void testDeleteNonExistingNormalizer() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int nrSize = schemaManager.getNormalizerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        Normalizer nr = new BooleanNormalizer();
        nr.setOid( "0.0" ); 
        assertFalse( schemaManager.delete( nr ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );

        assertEquals( nrSize, schemaManager.getNormalizerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }

    
    @Test
    public void testDeleteExistingNormalizerUsedByMatchingRule() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int nrSize = schemaManager.getNormalizerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        Normalizer nr = schemaManager.lookupNormalizerRegistry( "2.5.13.0" );
        // shouldn't be deleted cause there is a MR associated with it
        assertFalse( schemaManager.delete( nr ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );
        assertTrue( errors.get( 0 ) instanceof LdapSchemaViolationException );

        assertNotNull( schemaManager.lookupNormalizerRegistry( "2.5.13.0" ) );
        assertEquals( nrSize, schemaManager.getNormalizerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }

    
    /**
     * Check that a Normalizer which has been used by a deleted MatchingRule
     * can be removed
     */
    @Test
    public void testDeleteExistingNormalizerUsedByRemovedMatchingRule() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int nrSize = schemaManager.getNormalizerRegistry().size();
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        String OID = "2.5.13.33";

        // Check that the MR and N are present
        assertTrue( isMatchingRulePresent( schemaManager, OID ) );
        assertTrue( isNormalizerPresent( schemaManager, OID ) );

        // Now try to remove the N
        Normalizer normalizer = schemaManager.lookupNormalizerRegistry( OID );
        
        // shouldn't be deleted cause there is a MR associated with it
        assertFalse( schemaManager.delete( normalizer ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );
        assertTrue( errors.get( 0 ) instanceof LdapSchemaViolationException );

        // Now delete the using MR : it should be OK
        MatchingRule mr = new MatchingRule( OID );
        assertTrue( schemaManager.delete( mr ) );

        assertEquals( mrrSize - 1, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );

        assertFalse( isMatchingRulePresent( schemaManager, OID ) );
        
        // and try to delete the normalizer again
        assertTrue( schemaManager.delete( normalizer ) );

        assertFalse( isNormalizerPresent( schemaManager, OID ) );
        assertEquals( nrSize - 1, schemaManager.getNormalizerRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );
    }
    

    //=========================================================================
    // ObjectClass deletion tests
    //-------------------------------------------------------------------------

    @Test
    public void testDeleteExistingObjectClass() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int ocSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        ObjectClass oc = new ObjectClass( "2.5.17.2" );
        
        assertTrue( schemaManager.delete( oc ) );
        
        assertEquals( ocSize - 1, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );
        
        try
        {
            schemaManager.lookupObjectClassRegistry( "2.5.17.2" );
            fail();
        }
        catch( Exception e )
        {
            // expected
        }
    }
    

    @Test
    public void testDeleteNonExistingObjectClass() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int ocSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        ObjectClass oc = new ObjectClass( "0.1.1" );
        
        assertFalse( schemaManager.delete( oc ) );
        
        assertEquals( ocSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }
    

    @Test
    public void testDeleteExistingObjectClassUsedByAnotherObjectClass() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int ocSize = schemaManager.getObjectClassRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        ObjectClass oc = new ObjectClass( "2.5.6.0" );
        
        // shouldn't delete the 'top' OC
        assertFalse( schemaManager.delete( oc ) );
        
        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );
        assertTrue( errors.get( 0 ) instanceof LdapSchemaViolationException );

        assertEquals( ocSize, schemaManager.getObjectClassRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }
    

    //=========================================================================
    // Syntax deletion tests
    //-------------------------------------------------------------------------
    
    @Test
    public void testDeleteExistingSyntax() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int sSize = schemaManager.getLdapSyntaxRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        // delete a existing syntax not used by AT and MR
        LdapSyntax syntax = schemaManager.lookupLdapSyntaxRegistry( "1.3.6.1.4.1.1466.115.121.1.10" );
        assertTrue( schemaManager.delete( syntax ) );
        
        assertEquals( sSize - 1, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize -1, schemaManager.getGlobalOidRegistry().size() );

        // add a syntax and then delete (should behave same as above )
        syntax = new LdapSyntax( "0.1.1" );
        assertTrue( schemaManager.add( syntax ) );

        assertEquals( sSize, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
        
        syntax = schemaManager.lookupLdapSyntaxRegistry( "0.1.1" );
        assertTrue( schemaManager.delete( syntax ) );

        try
        {
            schemaManager.lookupLdapSyntaxRegistry( "0.1.1" );
            fail( "shouldn't find the syntax" );
        }
        catch( Exception e )
        {
            // expected behaviour
        }
        
        assertEquals( sSize - 1, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );
    }

    
    @Test
    public void testDeleteNonExistingSyntax() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int sSize = schemaManager.getLdapSyntaxRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        LdapSyntax syntax = new LdapSyntax( "0.1.1" );
        
        assertFalse( schemaManager.delete( syntax ) );
        
        assertEquals( sSize, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }

    
    @Test
    public void testDeleteExistingSyntaxUsedByMatchingRule() throws Exception
    {

        SchemaManager schemaManager = loadSchema( "system" );
        int sSize = schemaManager.getLdapSyntaxRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        //1.3.6.1.4.1.1466.115.121.1.26 is used by MR 1.3.6.1.4.1.1466.109.114.2
        LdapSyntax syntax = new LdapSyntax( "1.3.6.1.4.1.1466.115.121.1.26" );
        assertFalse( schemaManager.delete( syntax ) );
        
        // syntax 1.3.6.1.4.1.1466.115.121.1.12 is used by MR 2.5.13.1 and many AT
        syntax = new LdapSyntax( "1.3.6.1.4.1.1466.115.121.1.12" );
        
        assertFalse( schemaManager.delete( syntax ) );
        
        assertEquals( sSize, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }

    
    @Test
    public void testDeleteExistingSyntaxUsedByAttributeType() throws Exception
    {
       // syntax 1.3.6.1.4.1.1466.115.121.1.15 is used by AT 1.3.6.1.1.4

        SchemaManager schemaManager = loadSchema( "system" );
        int sSize = schemaManager.getLdapSyntaxRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        LdapSyntax syntax = new LdapSyntax( "1.3.6.1.4.1.1466.115.121.1.15" );
        
        assertFalse( schemaManager.delete( syntax ) );
        
        assertEquals( sSize, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }
    
    
    /**
     * Check that a Syntax which has been used by a deleted MatchingRule
     * can be removed
     */
    @Test
    public void testDeleteExistingSyntaxUsedByRemovedMatchingRule() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int srSize = schemaManager.getLdapSyntaxRegistry().size();
        int mrrSize = schemaManager.getMatchingRuleRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        String MR_OID = "2.5.13.11";
        String S_OID =  "1.3.6.1.4.1.1466.115.121.1.41";

        // Check that the MR and S are present
        assertTrue( isMatchingRulePresent( schemaManager, MR_OID ) );
        assertTrue( isSyntaxPresent( schemaManager, S_OID ) );

        // Now try to remove the S
        LdapSyntax syntax = schemaManager.lookupLdapSyntaxRegistry( S_OID );
        
        // shouldn't be deleted cause there is a MR associated with it
        assertFalse( schemaManager.delete( syntax ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );
        assertTrue( errors.get( 0 ) instanceof LdapSchemaViolationException );

        // Now delete the using MR : it should be OK
        MatchingRule mr = new MatchingRule( MR_OID );
        assertTrue( schemaManager.delete( mr ) );

        assertEquals( mrrSize - 1, schemaManager.getMatchingRuleRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );

        assertFalse( isMatchingRulePresent( schemaManager, MR_OID ) );
        
        // and try to delete the syntax again
        assertTrue( schemaManager.delete( syntax ) );

        assertFalse( isSyntaxPresent( schemaManager, S_OID ) );
        assertEquals( srSize - 1, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize - 2, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Check that a Syntax which has been used by a deleted AttributeType
     * can be removed
     */
    @Test
    public void testDeleteExistingSyntaxUsedByRemovedAttributeType() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int srSize = schemaManager.getLdapSyntaxRegistry().size();
        int atrSize = schemaManager.getAttributeTypeRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        String AT_OID = "1.3.6.1.4.1.1466.101.120.16";
        String S_OID =  "1.3.6.1.4.1.1466.115.121.1.54";

        // Check that the AT and S are present
        assertTrue( isAttributeTypePresent( schemaManager, AT_OID ) );
        assertTrue( isSyntaxPresent( schemaManager, S_OID ) );

        // Now try to remove the S
        LdapSyntax syntax = schemaManager.lookupLdapSyntaxRegistry( S_OID );
        
        // shouldn't be deleted cause there is a AT associated with it
        assertFalse( schemaManager.delete( syntax ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );
        assertTrue( errors.get( 0 ) instanceof LdapSchemaViolationException );

        // Now delete the using AT : it should be OK
        AttributeType at = new AttributeType( AT_OID );
        assertTrue( schemaManager.delete( at ) );

        assertEquals( atrSize - 1, schemaManager.getAttributeTypeRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );

        assertFalse( isAttributeTypePresent( schemaManager, AT_OID ) );
        
        // and try to delete the syntax again
        assertTrue( schemaManager.delete( syntax ) );

        assertFalse( isSyntaxPresent( schemaManager, S_OID ) );
        assertEquals( srSize - 1, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize - 2, schemaManager.getGlobalOidRegistry().size() );
    }


    /**
     * Check that a SyntaxChecker which has been used by a deleted Syntax
     * can be removed
     */
    @Test
    public void testDeleteExistingSyntaxCheckerUsedByRemovedSyntax() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int scrSize = schemaManager.getSyntaxCheckerRegistry().size();
        int srSize = schemaManager.getLdapSyntaxRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();
        
        String OID = "1.3.6.1.4.1.1466.115.121.1.33";

        // Check that the S and SC are present
        assertTrue( isSyntaxCheckerPresent( schemaManager, OID ) );
        assertTrue( isSyntaxPresent( schemaManager, OID ) );

        // Now try to remove the SC
        SyntaxChecker sc = schemaManager.lookupSyntaxCheckerRegistry( OID );
        
        // shouldn't be deleted cause there is a S associated with it
        assertFalse( schemaManager.delete( sc ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );
        assertTrue( errors.get( 0 ) instanceof LdapSchemaViolationException );

        // Now delete the using S : it should be OK
        LdapSyntax syntax = new LdapSyntax( OID );
        assertTrue( schemaManager.delete( syntax ) );

        assertEquals( srSize - 1, schemaManager.getLdapSyntaxRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );

        assertFalse( isSyntaxPresent( schemaManager, OID ) );
        
        // and try to delete the SC again
        assertTrue( schemaManager.delete( sc ) );

        assertFalse( isSyntaxCheckerPresent( schemaManager, OID ) );
        assertEquals( scrSize - 1, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( goidSize - 1, schemaManager.getGlobalOidRegistry().size() );
    }
    

    //=========================================================================
    // SyntaxChecker deletion tests
    //-------------------------------------------------------------------------
    
    @Test
    public void testDeleteExistingSyntaxChecker() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int scrSize = schemaManager.getSyntaxCheckerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        SyntaxChecker sc = new BooleanSyntaxChecker();
        sc.setOid( "0.1.1" );
        assertTrue( schemaManager.add( sc ) );

        assertEquals( scrSize + 1, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );

        sc = schemaManager.lookupSyntaxCheckerRegistry( "0.1.1" );
        assertNotNull( sc );
        assertTrue( schemaManager.delete( sc ) );

        try
        {
            schemaManager.lookupSyntaxCheckerRegistry( "0.1.1" );
            fail();
        }
        catch ( Exception e )
        {
            // expected
        }

        assertEquals( scrSize, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }
    
    
    @Test
    public void testDeleteNonExistingSyntaxChecker() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int scrSize = schemaManager.getSyntaxCheckerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        SyntaxChecker sc = new BooleanSyntaxChecker();
        sc.setOid( "0.0" ); 
        assertFalse( schemaManager.delete( sc ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );

        assertEquals( scrSize, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }

    
    @Test
    public void testDeleteExistingSyntaxCheckerUsedBySyntax() throws Exception
    {
        SchemaManager schemaManager = loadSchema( "system" );
        int scrSize = schemaManager.getSyntaxCheckerRegistry().size();
        int goidSize = schemaManager.getGlobalOidRegistry().size();

        SyntaxChecker sc = schemaManager.lookupSyntaxCheckerRegistry( "1.3.6.1.4.1.1466.115.121.1.1" );
        
        //FIXME should return false but is returning true
        assertFalse( schemaManager.delete( sc ) );

        List<Throwable> errors = schemaManager.getErrors();
        assertFalse( errors.isEmpty() );
        assertTrue( errors.get( 0 ) instanceof LdapSchemaViolationException );

        assertEquals( scrSize, schemaManager.getSyntaxCheckerRegistry().size() );
        assertEquals( goidSize, schemaManager.getGlobalOidRegistry().size() );
    }
}
