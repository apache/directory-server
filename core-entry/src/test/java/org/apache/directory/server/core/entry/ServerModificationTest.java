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
package org.apache.directory.server.core.entry;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.ClientAttribute;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.schema.loader.ldif.LdifSchemaLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test the ServerModification class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerModificationTest
{
    private static LdifSchemaLoader loader;
    private static Registries registries;
    private static AttributeType atCN;
    
    // A SINGLE-VALUE attribute
    private static AttributeType atC;   
    
    
    /**
     * Serialize a ServerModification
     */
    private ByteArrayOutputStream serializeValue( ServerModification value ) throws IOException
    {
        ObjectOutputStream oOut = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try
        {
            oOut = new ObjectOutputStream( out );
            value.serialize( oOut );
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
     * Deserialize a ServerModification
     */
    private ServerModification deserializeValue( ByteArrayOutputStream out ) throws IOException, ClassNotFoundException, NamingException
    {
        ObjectInputStream oIn = null;
        ByteArrayInputStream in = new ByteArrayInputStream( out.toByteArray() );

        try
        {
            oIn = new ObjectInputStream( in );

            ServerModification value = new ServerModification();
            value.deserialize( oIn, registries.getAttributeTypeRegistry() );

            return value;
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if ( oIn != null )
                {
                    oIn.close();
                }
            }
            catch ( IOException ioe )
            {
                throw ioe;
            }
        }
    }
    
    
    /**
     * Initialize the registries once for the whole test suite
     */
    @BeforeClass
    public static void setup() throws Exception
    {
    	String workingDirectory = System.getProperty( "workingDirectory" );
    	File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new SchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();
        loader = new LdifSchemaLoader( schemaRepository );
        registries = new Registries();
        loader.loadAllEnabled( registries );
        atCN = registries.getAttributeTypeRegistry().lookup( "cn" );
        atC = registries.getAttributeTypeRegistry().lookup( "c" );
    }


    @Test public void testCreateServerModification()
    {
        ServerAttribute attribute = new DefaultServerAttribute( atCN );
        attribute.add( "test1", "test2" );
        
        Modification mod = new ServerModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
        Modification clone = mod.clone();
        
        attribute.remove( "test2" );
        
        ServerAttribute clonedAttribute = (ServerAttribute)clone.getAttribute();
        
        assertEquals( 1, mod.getAttribute().size() );
        assertTrue( mod.getAttribute().contains( "test1" ) );

        assertEquals( 2, clonedAttribute.size() );
        assertTrue( clone.getAttribute().contains( "test1" ) );
        assertTrue( clone.getAttribute().contains( "test2" ) );
    }
    
    
    /**
     * Test the copy constructor with a ServerModification
     *
     */
    @Test
    public void testCopyServerModification()
    {
        ServerAttribute attribute = new DefaultServerAttribute( atC );
        attribute.add( "test1", "test2" );
        Modification serverModification = new ServerModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
        
        Modification copy = new ServerModification( registries, serverModification );
        
        assertTrue( copy instanceof ServerModification );
        assertEquals( copy, serverModification );
        
        serverModification.setOperation( ModificationOperation.REMOVE_ATTRIBUTE );
        assertEquals( ModificationOperation.ADD_ATTRIBUTE, copy.getOperation() );
        
        ServerAttribute attribute2 = new DefaultServerAttribute( atCN, "t" );
        serverModification.setAttribute( attribute2 );
        assertNotSame( attribute2, copy.getAttribute() );
    }
    
    
    /**
     * Test the copy constructor with a ClientModification
     *
     */
    @Test
    public void testCopyClientModification()
    {
        ClientAttribute attribute = new DefaultClientAttribute( atC.getName() );
        attribute.add( "test1", "test2" );
        Modification clientModification = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
        
        Modification copy = new ServerModification( registries, clientModification );
        
        assertTrue( copy instanceof ServerModification );
        assertFalse( copy instanceof ClientModification );
        assertFalse( copy.equals(  clientModification ) );
        assertTrue( copy.getAttribute() instanceof ServerAttribute );
        assertEquals( atC, ((ServerAttribute)copy.getAttribute()).getAttributeType() );
        assertEquals( ModificationOperation.ADD_ATTRIBUTE, copy.getOperation() );
        assertTrue( copy.getAttribute().contains( "test1", "test2" ) );
        
        clientModification.setOperation( ModificationOperation.REMOVE_ATTRIBUTE );
        assertEquals( ModificationOperation.ADD_ATTRIBUTE, copy.getOperation() );
        
        ClientAttribute attribute2 = new DefaultClientAttribute( "cn", "t" );
        clientModification.setAttribute( attribute2 );
        assertNotSame( attribute2, copy.getAttribute() );
    }
    
    
    @Test
    public void testSerializationModificationADD() throws ClassNotFoundException, IOException, NamingException
    {
        EntryAttribute attribute = new DefaultServerAttribute( atCN );
        attribute.add( "test1", "test2" );
        
        ServerModification mod = new ServerModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
        
        Modification modSer = deserializeValue( serializeValue( mod ) );
        
        assertEquals( mod, modSer );
    }
    
    
    @Test
    public void testSerializationModificationREPLACE() throws ClassNotFoundException, IOException, NamingException
    {
        EntryAttribute attribute = new DefaultServerAttribute( atCN );
        attribute.add( "test1", "test2" );
        
        ServerModification mod = new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );
        
        Modification modSer = deserializeValue( serializeValue( mod ) );
        
        assertEquals( mod, modSer );
    }
    
    
    @Test
    public void testSerializationModificationREMOVE() throws ClassNotFoundException, IOException, NamingException
    {
        EntryAttribute attribute = new DefaultServerAttribute( atCN );
        attribute.add( "test1", "test2" );
        
        ServerModification mod = new ServerModification( ModificationOperation.REMOVE_ATTRIBUTE, attribute );
        
        Modification modSer = deserializeValue( serializeValue( mod ) );
        
        assertEquals( mod, modSer );
    }
    
    
    @Test
    public void testSerializationModificationNoAttribute() throws ClassNotFoundException, IOException, NamingException
    {
        ServerModification mod = new ServerModification();
        
        mod.setOperation( ModificationOperation.ADD_ATTRIBUTE );
        
        try
        {
            deserializeValue( serializeValue( mod ) );
            fail();
        }
        catch ( IOException ioe )
        {
            assertTrue( true );
        }
    }
}
