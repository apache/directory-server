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
package org.apache.directory.server.core.splay;

import static junit.framework.Assert.assertEquals;

import java.util.Comparator;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests the linking of <code>LinkedBinaryNode</code>s present in the splay tree.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SplayTreeTest
{
   SplayTree<Integer> tree; 
    
   @Before
   public void createTree()
   {
       tree = new SplayTree<Integer>( new Comparator<Integer>()
        {

            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }
        });
   }
   
   @Test
   public void testLinkedNodes()
   {
       for( int i=0; i< 3; i++)
       {
          tree.insert( i ); 
       }
       
       assertEquals( "[0]-->[1]-->[2]-->NULL", getLinkedText());
       
       tree.remove( 1 );
       assertEquals( "[0]-->[2]-->NULL", getLinkedText());
       
       tree.insert( 4 );
       tree.insert( 3 );
       
       assertEquals( "[0]-->[2]-->[4]-->[3]-->NULL", getLinkedText());
       
       tree.remove( 0 );
       assertEquals( "[2]-->[4]-->[3]-->NULL", getLinkedText());
       
       tree.remove( 3 );
       assertEquals( "[2]-->[4]-->NULL", getLinkedText());
   }
   
   private String getLinkedText() 
   {
       LinkedBinaryNode<Integer> first = tree.getFirst();
       StringBuilder sb = new StringBuilder();
       
       while( first != null )
       {
           sb.append( first )
             .append( "-->" );
           
           first = first.next;
       }
       sb.append( "NULL" );
       return sb.toString();
   }
}
