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
package org.apache.directory.server.core.partition.impl.btree.mavibot;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.mavibot.btree.Tuple;
import org.apache.directory.mavibot.btree.persisted.BulkDataSorter;
import org.junit.Ignore;

/**
 * TODO LdifBulkLoaderTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Ignore
public class LdifBulkLoaderTest
{
    String personTemplate =  "# just a comment\n"+
                    "dn: uid={uid},{parent}\n" +
                    "objectClass: top\n" +
                    "objectClass: person\n" +
                    "objectClass: organizationalPerson\n" +
                    "objectClass: inetOrgPerson\n" +
                    "givenName: {uid}_{uid}\n" +
                    "sn: {uid}_sn\n" +
                    "cn: {uid}_cn\n" +
                    "uid: {uid}\n\n";

    String ouTemplate = "# just another comment\n"+
                        "dn: {ouDn}\n" +
                        "objectclass: top\n" +
                        "objectclass: organizationalUnit\n" +
                        "ou: {ou}\n\n";
    
    private void sort() throws Exception
    {
        LdifTupleReaderWriter readerWriter = new LdifTupleReaderWriter();
        LdifTupleComparator tupleComparator = new LdifTupleComparator();
        
        BulkDataSorter bs = new BulkDataSorter<Dn, String>( readerWriter, tupleComparator, 2 );
        
        File file = createLdif();
        
        bs.sort( file );
        
        Iterator<Tuple<Dn, String>> itr = bs.getMergeSortedTuples();
        while( itr.hasNext() )
        {
            Tuple t = itr.next();
            System.out.println( t.getKey() + " - " + t.getValue() );
        }
        
        //BTreeBuilder<, V>
    }
    
    private File createLdif() throws Exception
    {
        File file = File.createTempFile( "bulkload-test", ".ldif" );
        file.deleteOnExit();
        
        FileWriter fw = new FileWriter( file );
        
        Dn parentDn = new Dn( "ou=grandchildren,ou=children,ou=parent,ou=system" );
        
        Dn currentDn = parentDn;
        
        for( Rdn rdn : parentDn.getRdns() )
        {
            for( int i =0; i< 2; i++ )
            {
                String user = personTemplate.replace( "{uid}", "user"+i );
                user = user.replace( "{parent}", currentDn.getName() );

                fw.write( user );
            }
            
            
            String userBranch = ouTemplate.replace( "{ou}", rdn.getValue().getString() );
            userBranch = userBranch.replace( "{ouDn}", currentDn.getName() );
            
            fw.write( userBranch );
            
            currentDn = currentDn.getParent();
        }
        
        fw.close();

        return file;
    }
    
    public static void main( String[] args ) throws Exception
    {
        LdifBulkLoaderTest bl = new LdifBulkLoaderTest();
        bl.sort();
    }
}
