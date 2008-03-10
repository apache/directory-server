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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;

import org.apache.directory.server.core.avltree.AvlTree;
import org.junit.Before;
import org.junit.Test;

/**
 * TestCase for AvlTreeMarshaller.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlTreeMarshallerTest
{

    AvlTree<Integer> tree;
    Comparator<Integer> comparator;
    AvlTreeMarshaller<Integer> treeMarshaller;
    
    static AvlTree<Integer> savedTree;
    
    File treeFile = new File( System.getProperty( "java.io.tmpdir" ) + File.separator + "avl.tree");
    
    @Before
    public void createTree()
    {
        comparator = new Comparator<Integer>() 
        {

          public int compare( Integer i1, Integer i2 )
          {
              return i1.compareTo( i2 );
          }
        
        };
        
      
      tree = new AvlTree<Integer>( comparator );  
      treeMarshaller = new AvlTreeMarshaller<Integer>( comparator, new IntegerKeyMarshaller() );
    }
    
    @Test
    public void testMarshal() throws FileNotFoundException, IOException
    {
        
        tree.insert( 37 );
        tree.insert( 7 );
        tree.insert( 25 );
        tree.insert( 8 );
        tree.insert( 9 );

        FileOutputStream fout = new FileOutputStream( treeFile );
        fout.write( treeMarshaller.serialize( tree ) );
        fout.close();
        
        savedTree = tree; // to reference in other tests
        
        System.out.println("saved tree\n--------");
        tree.printTree();
        
        assertTrue( true );
    }


    @Test
    public void testUnMarshal() throws FileNotFoundException, IOException
    {
        FileInputStream fin = new FileInputStream(treeFile);
        
        byte[] data = new byte[ ( int )treeFile.length() ];
        fin.read( data );
        
        AvlTree<Integer> unmarshalledTree = treeMarshaller.deserialize( data );
        
        System.out.println("\nunmarshalled tree\n---------------");
        unmarshalledTree.printTree();
        
        assertTrue( savedTree.getRoot().getKey() == unmarshalledTree.getRoot().getKey() );

        unmarshalledTree.insert( 6 ); // will change the root as part of balancing
        
        assertTrue( savedTree.getRoot().getKey() == unmarshalledTree.getRoot().getKey() );
        assertTrue( 8 == unmarshalledTree.getRoot().getKey() ); // new root
        
        assertTrue( 37 == unmarshalledTree.getLast().getKey() );
        unmarshalledTree.insert( 99 );
        assertTrue( 99 == unmarshalledTree.getLast().getKey() );

        assertTrue( 6 == unmarshalledTree.getFirst().getKey() );
        
        unmarshalledTree.insert( 0 );
        assertTrue( 0 == unmarshalledTree.getFirst().getKey() );
        
        System.out.println("\nmodified tree after unmarshalling\n---------------");
        unmarshalledTree.printTree();
        
        assertNotNull(unmarshalledTree.getFirst());
        assertNotNull(unmarshalledTree.getLast());
    }

}
