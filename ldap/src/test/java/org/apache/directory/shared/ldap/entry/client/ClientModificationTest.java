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
package org.apache.directory.shared.ldap.entry.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * Test the ClientModification class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ClientModificationTest
{
    /**
     * Serialize a ClientModification
     */
    private ByteArrayOutputStream serializeValue( ClientModification value ) throws IOException
    {
        ObjectOutputStream oOut = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try
        {
            oOut = new ObjectOutputStream( out );
            oOut.writeObject( value );
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
     * Deserialize a ClientModification
     */
    private ClientModification deserializeValue( ByteArrayOutputStream out ) throws IOException, ClassNotFoundException
    {
        ObjectInputStream oIn = null;
        ByteArrayInputStream in = new ByteArrayInputStream( out.toByteArray() );

        try
        {
            oIn = new ObjectInputStream( in );

            ClientModification value = ( ClientModification ) oIn.readObject();

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
    
    
    @Test 
    public void testCreateServerModification()
    {
        EntryAttribute attribute = new DefaultClientAttribute( "cn" );
        attribute.add( "test1", "test2" );
        
        Modification mod = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
        Modification clone = mod.clone();
        
        attribute.remove( "test2" );
        
        EntryAttribute clonedAttribute = (ClientAttribute)clone.getAttribute();
        
        assertEquals( 1, mod.getAttribute().size() );
        assertTrue( mod.getAttribute().contains( "test1" ) );

        assertEquals( 2, clonedAttribute.size() );
        assertTrue( clone.getAttribute().contains( "test1" ) );
        assertTrue( clone.getAttribute().contains( "test2" ) );
    }
    
    
    @Test
    public void testSerializationModificationADD() throws ClassNotFoundException, IOException
    {
        EntryAttribute attribute = new DefaultClientAttribute( "cn" );
        attribute.add( "test1", "test2" );
        
        ClientModification mod = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attribute );
        
        Modification modSer = deserializeValue( serializeValue( mod ) );
        
        assertEquals( mod, modSer );
    }
    
    
    @Test
    public void testSerializationModificationREPLACE() throws ClassNotFoundException, IOException
    {
        EntryAttribute attribute = new DefaultClientAttribute( "cn" );
        attribute.add( "test1", "test2" );
        
        ClientModification mod = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute );
        
        Modification modSer = deserializeValue( serializeValue( mod ) );
        
        assertEquals( mod, modSer );
    }
    
    
    @Test
    public void testSerializationModificationREMOVE() throws ClassNotFoundException, IOException
    {
        EntryAttribute attribute = new DefaultClientAttribute( "cn" );
        attribute.add( "test1", "test2" );
        
        ClientModification mod = new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, attribute );
        
        Modification modSer = deserializeValue( serializeValue( mod ) );
        
        assertEquals( mod, modSer );
    }
    
    
    @Test
    public void testSerializationModificationNoAttribute() throws ClassNotFoundException, IOException
    {
        ClientModification mod = new ClientModification();
        
        mod.setOperation( ModificationOperation.ADD_ATTRIBUTE );
        
        Modification modSer = deserializeValue( serializeValue( mod ) );
        
        assertEquals( mod, modSer );
    }
}
