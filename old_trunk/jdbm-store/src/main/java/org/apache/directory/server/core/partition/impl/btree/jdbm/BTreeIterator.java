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
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;

import jdbm.btree.BTree;
import jdbm.helper.TupleBrowser;


/**
 * A NamingEnumeration that returns keys in a BTree.  This is used specifically 
 * for situations when tables support duplicate keys and a BTree is used for 
 * storing the values for that key.  This enumeration thus advances a browser forwards 
 * returning keys from a BTree as values.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BTreeIterator implements Iterator
{
    private final jdbm.helper.Tuple jdbmTuple = new jdbm.helper.Tuple();
    private TupleBrowser browser;
    private boolean success = false;
    private boolean doAscending = true;
    
    
    BTreeIterator( BTree tree, boolean doAscending ) throws NamingException 
    {
        this.doAscending = doAscending;
        
        try
        {
            if ( doAscending )
            {
                browser = tree.browse();
            }
            else
            {
                browser = tree.browse( null );
            }
            
            prefetch();
        }
        catch ( IOException e )
        {
            LdapNamingException lne = new LdapNamingException( "Failure on btree: " + e.getMessage(), 
                ResultCodeEnum.OTHER );
            lne.setRootCause( e );
            throw lne;
        }
    }

    
    private void prefetch() throws IOException
    {
        if ( doAscending )
        {
            success = browser.getNext( jdbmTuple );
        }
        else
        {
            success = browser.getPrevious( jdbmTuple );
        }
    }
    
    
    public boolean hasNext()
    {
        return success;
    }


    public Object next()
    {
        if ( ! success )
        {
            throw new NoSuchElementException();
        }
        
        Object next = jdbmTuple.getKey();
        try
        {
            prefetch();
        }
        catch ( IOException e )
        {
            throw new NoSuchElementException( "Failure on btree: " + e.getMessage() );
        }
        return next;
    }


    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
