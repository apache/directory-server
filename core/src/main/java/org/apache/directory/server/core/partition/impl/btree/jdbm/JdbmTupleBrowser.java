/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.IOException;

import javax.naming.NamingException;

import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.core.partition.impl.btree.TupleBrowser;


/**
 * TupleBrowser wrapper for Jdbm based TupleBrowsers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmTupleBrowser implements TupleBrowser
{
    /** underlying wrapped jdbm.helper.TupleBrowser */
    private jdbm.helper.TupleBrowser jdbmBrowser;
    /** safe temp jdbm.helper.Tuple used to store next/previous tuples */ 
    private jdbm.helper.Tuple jdbmTuple = new jdbm.helper.Tuple();
    
    
    /**
     * Creates a JdbmTupleBrowser.
     *
     * @param jdbmBrowser JDBM browser to wrap.
     */
    public JdbmTupleBrowser( jdbm.helper.TupleBrowser jdbmBrowser )
    {
        this.jdbmBrowser = jdbmBrowser;
    }
    
    
    /**
     * @see TupleBrowser#getNext(org.apache.directory.server.core.partition.impl.btree.Tuple)
     */
    public boolean getNext( Tuple tuple ) throws NamingException
    {
        boolean isSuccess = false;
        
        synchronized ( jdbmTuple )
        {
            try
            {
                isSuccess = jdbmBrowser.getNext( jdbmTuple );
            }
            catch ( IOException ioe )
            {
                NamingException ne = new NamingException( 
                    "Failed on call to jdbm TupleBrowser.getNext()" );
                ne.setRootCause( ioe );
                throw ne;
            }
            
            if ( isSuccess )
            {
                tuple.setKey( jdbmTuple.getKey() );
                tuple.setValue( jdbmTuple.getValue() );
            }
        }

        return isSuccess;
    }
    
    
    /**
     * @see TupleBrowser#getPrevious(Tuple)
     */
    public boolean getPrevious( Tuple tuple ) throws NamingException
    {
        boolean isSuccess = false;
        
        synchronized ( jdbmTuple )
        {
            try
            {
                isSuccess = jdbmBrowser.getPrevious( jdbmTuple );
            }
            catch ( IOException ioe )
            {
                NamingException ne = new NamingException( 
                    "Failed on call to jdbm TupleBrowser.getPrevious()" );
                ne.setRootCause( ioe );
                throw ne;
            }
            
            if ( isSuccess )
            {
                tuple.setKey( jdbmTuple.getKey() );
                tuple.setValue( jdbmTuple.getValue() );
            }
        }

        return isSuccess;
    }
}
