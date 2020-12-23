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
package org.apache.directory.server.core.api.entry;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * Test the DefaultModification class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaAwareModificationTest
{
    private static LdifSchemaLoader loader;
    private static SchemaManager schemaManager;
    private static AttributeType atCN;

    // A SINGLE-VALUE attribute
    private static AttributeType atC;


    /**
     * Serialize a DefaultModification
     */
    private ByteArrayOutputStream serializeValue( DefaultModification value ) throws IOException
    {
        ObjectOutputStream oOut = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try
        {
            oOut = new ObjectOutputStream( out );
            value.writeExternal( oOut );
            oOut.flush();
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if ( oOut != null )
                {
                    oOut.flush();
                    oOut.close();
                }
            }
            catch ( IOException ioe )
            {
                throw ioe;
            }
        }

        return out;
    }


    /**
     * Deserialize a DefaultModification
     */
    private DefaultModification deserializeValue( ByteArrayOutputStream out ) throws IOException,
        ClassNotFoundException, LdapException
    {

        try ( ByteArrayInputStream in = new ByteArrayInputStream( out.toByteArray() );
            ObjectInputStream oIn = new ObjectInputStream( in ) )
        {

            DefaultModification value = new DefaultModification();
            value.readExternal( oIn );

            return value;
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
    }


    /**
     * Initialize the registries once for the whole test suite
     */
    @BeforeAll
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SchemaAwareModificationTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        loader = new LdifSchemaLoader( schemaRepository );

        schemaManager = new DefaultSchemaManager( loader );
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( errors ) );
        }

        atCN = schemaManager.lookupAttributeTypeRegistry( "cn" );
        atC = schemaManager.lookupAttributeTypeRegistry( "c" );
    }


    @Test
    public void testCreateClientModification() throws LdapException
    {
        Attribute attribute = new DefaultAttribute( atCN );
        attribute.add( "test1", "test2" );

        Modification mod = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
        Modification clone = mod.clone();

        attribute.remove( "test2" );

        Attribute clonedAttribute = clone.getAttribute();

        assertEquals( 1, mod.getAttribute().size() );
        assertTrue( mod.getAttribute().contains( "test1" ) );

        assertEquals( 2, clonedAttribute.size() );
        assertTrue( clone.getAttribute().contains( "test1" ) );
        assertTrue( clone.getAttribute().contains( "test2" ) );
    }


    /**
     * Test the copy constructor with a DefaultModification
     *
     */
    @Test
    public void testCopyClientModification() throws LdapException
    {
        Attribute attribute = new DefaultAttribute( atC );
        attribute.add( "test1", "test2" );
        Modification serverModification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attribute );

        Modification copy = new DefaultModification( schemaManager, serverModification );

        assertTrue( copy instanceof DefaultModification );
        assertEquals( copy, serverModification );

        serverModification.setOperation( ModificationOperation.REMOVE_ATTRIBUTE );
        assertEquals( ModificationOperation.ADD_ATTRIBUTE, copy.getOperation() );

        Attribute attribute2 = new DefaultAttribute( atCN, "t" );
        serverModification.setAttribute( attribute2 );
        assertNotSame( attribute2, copy.getAttribute() );
    }


    /**
     * Test the copy constructor with a DefaultModification
     *
     */
    @Test
    public void testCopyModification() throws LdapException
    {
        Attribute attribute = new DefaultAttribute( atC.getName() );
        attribute.add( "test1", "test2" );
        Modification clientModification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attribute );

        Modification copy = new DefaultModification( schemaManager, clientModification );

        assertTrue( copy instanceof DefaultModification );
        assertTrue( copy instanceof DefaultModification );
        assertFalse( copy.equals( clientModification ) );
        assertTrue( copy.getAttribute() instanceof Attribute );
        assertEquals( atC, copy.getAttribute().getAttributeType() );
        assertEquals( ModificationOperation.ADD_ATTRIBUTE, copy.getOperation() );
        assertTrue( copy.getAttribute().contains( "test1", "test2" ) );

        clientModification.setOperation( ModificationOperation.REMOVE_ATTRIBUTE );
        assertEquals( ModificationOperation.ADD_ATTRIBUTE, copy.getOperation() );

        Attribute attribute2 = new DefaultAttribute( "cn", "t" );
        clientModification.setAttribute( attribute2 );
        assertNotSame( attribute2, copy.getAttribute() );
    }


    @Test
    public void testSerializationModificationADD() throws ClassNotFoundException, IOException, LdapException
    {
        Attribute attribute = new DefaultAttribute( atCN );
        attribute.add( "test1", "test2" );

        DefaultModification mod = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attribute );

        Modification modSer = deserializeValue( serializeValue( mod ) );
        modSer.setAttribute( attribute );

        assertEquals( mod, modSer );
    }


    @Test
    public void testSerializationModificationREPLACE() throws ClassNotFoundException, IOException, LdapException
    {
        Attribute attribute = new DefaultAttribute( atCN );
        attribute.add( "test1", "test2" );

        DefaultModification mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );

        Modification modSer = deserializeValue( serializeValue( mod ) );
        modSer.setAttribute( attribute );

        assertEquals( mod, modSer );
    }


    @Test
    public void testSerializationModificationREMOVE() throws ClassNotFoundException, IOException, LdapException
    {
        Attribute attribute = new DefaultAttribute( atCN );
        attribute.add( "test1", "test2" );

        DefaultModification mod = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attribute );

        Modification modSer = deserializeValue( serializeValue( mod ) );
        modSer.setAttribute( attribute );

        assertEquals( mod, modSer );
    }


    @Test
    public void testSerializationModificationNoAttribute() throws ClassNotFoundException, IOException, LdapException
    {
        Modification mod = new DefaultModification();

        mod.setOperation( ModificationOperation.ADD_ATTRIBUTE );

        Modification modSer = deserializeValue( serializeValue( ( DefaultModification ) mod ) );

        assertEquals( mod, modSer );
    }
}
