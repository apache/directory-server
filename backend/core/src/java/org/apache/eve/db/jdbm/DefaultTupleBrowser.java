package org.apache.eve.db.jdbm;


import org.apache.eve.db.Tuple;
import org.apache.eve.db.TupleBrowser;

import java.io.IOException;

import javax.naming.NamingException;


/**
 * TupleBrowser wrapper for Jdbm based TupleBrowsers.
 *
 * @todo rename this to JdbmTupleBrowser
 */
public class DefaultTupleBrowser implements TupleBrowser
{
    /** underlying wrapped jdbm.helper.TupleBrowser */
    private jdbm.helper.TupleBrowser jdbmBrowser;
    /** safe temp jdbm.helper.Tuple used to store next/previous tuples */ 
    private jdbm.helper.Tuple jdbmTuple = new jdbm.helper.Tuple();
    
    
    /**
     * Creates a DefaultTupleBrowser.
     *
     * @param jdbmBrowser JDBM browser to wrap.
     */
    public DefaultTupleBrowser( jdbm.helper.TupleBrowser jdbmBrowser )
    {
        this.jdbmBrowser = jdbmBrowser;
    }
    
    
    /**
     * @see TupleBrowser#getNext(Tuple)
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
