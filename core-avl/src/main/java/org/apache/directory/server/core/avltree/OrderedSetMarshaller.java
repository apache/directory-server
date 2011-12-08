/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.avltree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.util.Strings;

public class OrderedSetMarshaller<V> implements Marshaller<OrderedSet<V>>
{
    /** used for serialized form of an empty AvlTree */
    private static final byte[] EMPTY_SET = new byte[1];
    
    /** marshaller to be used for marshalling the keys */
    private Marshaller<V> valueMarshaller;
    
    /** value Comparator for the ordered set */
    private Comparator<V> comparator;
    
    /**
     * Creates a new instance of AvlTreeMarshaller with a custom key
     * Marshaller.
     *
     * @param comparator Comparator to be used for key comparision
     * @param keyMarshaller marshaller for keys
     */
    public OrderedSetMarshaller( Comparator<V> comparator, Marshaller<V> valueMarshaller )
    {
        this.comparator = comparator;
        this.valueMarshaller = valueMarshaller;
    }


    /**
     * Creates a new instance of AvlTreeMarshaller with the default key
     * Marshaller which uses Java Serialization.
     *
     * @param comparator Comparator to be used for key comparision
     */
    public OrderedSetMarshaller( Comparator<V> comparator )
    {
        this.comparator = comparator;
        this.valueMarshaller = ( Marshaller<V> ) DefaultMarshaller.INSTANCE;
    }


    /**
     * Marshals the given tree to bytes
     * @param tree the tree to be marshalled
     */
    public byte[] serialize( OrderedSet<V> set )
    {
        if ( ( set == null ) || ( set.getSize() == 0 ) )
        {
            return EMPTY_SET;
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream( byteStream );
        byte[] data = null;
        Iterator<V> it = set.getBackingMap().keySet().iterator(); 
        
        try
        {
            out.writeByte( 0 ); // represents the start of an Array byte stream
            out.writeInt( set.getSize() );

            while ( it.hasNext() )
            {
                V value = it.next();
                byte[] bytes = valueMarshaller.serialize( value );
                
                // Write the key length
                out.writeInt( bytes.length );
                
                // Write the key if its length is not null
                if ( bytes.length != 0 )
                {
                    out.write( bytes );
                }
            }
            
            out.flush();
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                out.close();
            }
            catch ( IOException e ) 
            {
                e.printStackTrace();
            }
        }
        
        return data;
    }

    
    /**
     * Creates an Array from given bytes of data.
     * 
     * @param data byte array to be converted into an array  
     */
    public OrderedSet<V> deserialize( byte[] data ) throws IOException
    {
        OrderedSet<V> set = new OrderedSet( comparator );
        
        //LOG.debug( "Deserializing the tree, called by {}", Reflection.getCallerClass( 2 ).getSimpleName() );

        ByteArrayInputStream bin = null;
        DataInputStream din = null;
        
        try
        {
            if ( ( data == null ) || ( data.length == 0 ) )
            {
                throw new IOException( I18n.err( I18n.ERR_439 ) );
            }
    
            if ( ( data.length == 1 ) && ( data[0] == 0 ) )
            {
                // Return empty set
                
                return set;
            }
    
            bin = new ByteArrayInputStream( data );
            din = new DataInputStream( bin );
            
            byte startByte = din.readByte();
            
            if( startByte != 0 )
            {
                throw new IOException( I18n.err( I18n.ERR_440 ) );
            }
            
            V value;
            int size = din.readInt();
            
            for ( int i = 0; i < size; i++ )
            {
                // Read the object's size
                int dataSize = din.readInt();
                
                if ( dataSize != 0 )
                {
                    byte[] bytes = new byte[ dataSize ];
                    
                    din.readFully( bytes );
                    value = valueMarshaller.deserialize( bytes );
                    set.insert( value );
                }
            }
            
            return set;
        }
        catch (NullPointerException npe )
        {
            System.out.println( I18n.err( I18n.ERR_441, Strings.dumpBytes(data) ) );
            throw npe;
        }
        finally
        {
         
            if ( bin != null )
            {
                bin.close();
            }
            
            if ( din != null )
            {
                din.close();
            }
        }
    }
}
