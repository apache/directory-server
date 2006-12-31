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
import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
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
public class BTreeEnumeration implements NamingEnumeration
{
    private final jdbm.helper.Tuple jdbmTuple = new jdbm.helper.Tuple();
    private TupleBrowser browser;
    private boolean success = false;
    
    
    BTreeEnumeration( BTree tree ) throws NamingException 
    {
        try
        {
            browser = tree.browse();
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
        success = browser.getNext( jdbmTuple );
    }
    
    
    public void close() throws NamingException
    {
        success = false;
    }


    public boolean hasMore() throws NamingException
    {
        return success;
    }


    public Object next() throws NamingException
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
            LdapNamingException lne = new LdapNamingException( "Failure on btree: " + e.getMessage(), 
                ResultCodeEnum.OTHER );
            lne.setRootCause( e );
            throw lne;
        }
        return next;
    }


    public boolean hasMoreElements()
    {
        return success;
    }


    public Object nextElement()
    {
        try
        {
            return next();
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
            throw new NoSuchElementException( "Got IO Failure on btree: " + e.getCause().getMessage() );
        }
    }
}
