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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.IOException;

import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.util.ArrayUtils;

import junit.framework.TestCase;


/**
 * Tests the {@link AttributeSerializer}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AttributesSerializerTest extends TestCase
{
    public void testFullCycle() throws IOException
    {
        AttributesImpl attrs = new AttributesImpl();
        AttributeImpl attr0 = new AttributeImpl( "attr0" );
        attr0.add( "value0" );
        attr0.add( "val1" );
        attr0.add( "anything over here!" );
        
        AttributeImpl attr1 = new AttributeImpl( "attr1" );
        byte[] ba0 = new byte[2];
        ba0[0] = 7;
        ba0[1] = 23;
        attr1.add( ba0 );
        byte[] ba1 = new byte[3];
        ba1[0] = 34;
        ba1[1] = 111;
        ba1[2] = 67;
        attr1.add( ba1 );
        
        attrs.put( attr0 );
        attrs.put( attr1 );
        AttributesSerializer serializer = new AttributesSerializer();
        byte[] buf = serializer.serialize( attrs );
        AttributesImpl deserialized = ( AttributesImpl ) serializer.deserialize( buf );

        AttributeImpl attrDeserialized0 = ( AttributeImpl ) deserialized.get( "attr0" );
        assertEquals( "value0", attrDeserialized0.get() );
        assertEquals( "val1", attrDeserialized0.get( 1 ) );
        assertEquals( "anything over here!", attrDeserialized0.get( 2 ) );
        
        AttributeImpl attrDeserialized1 = ( AttributeImpl ) deserialized.get( "attr1" );
        ArrayUtils.isEquals( ba0, attrDeserialized1.get() );
        ArrayUtils.isEquals( ba1, attrDeserialized1.get( 1 ) );
    }
    
    
//    public void doSerializerSpeedTest() throws IOException
//    {
//        final int limit = 1000000;
//        long start = System.currentTimeMillis();
//        for ( int ii = 0; ii < limit; ii++ )
//        {
//            AttributeImpl attr = new AttributeImpl( "testing" );
//            AttributeSerializer serializer = new AttributeSerializer();
//            attr.add( "value0" );
//            attr.add( "val1" );
//            attr.add( "anything over here!" );
//            
//            byte[] serialized = serializer.serialize( attr );
//            serializer.deserialize( serialized );
//        }
//        
//        System.out.println( limit + " attributes with 3 values each were serialized and deserialized in " 
//            + ( System.currentTimeMillis() - start ) + " (ms)" );
//    }
}
