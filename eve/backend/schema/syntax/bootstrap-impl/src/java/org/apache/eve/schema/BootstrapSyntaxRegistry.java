/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.eve.schema ;


import java.util.Map ;
import java.util.HashMap ;

import javax.naming.NamingException ;
import javax.naming.OperationNotSupportedException ;


/**
 * A SyntaxRegistry service available during server startup when other resources
 * like a syntax backing store is unavailable.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class BootstrapSyntaxRegistry implements SyntaxRegistry
{
    /** a map of entries using an OID for the key and a Syntax for the value */
    private final Map m_syntaxes ;
    /** the OID registry this registry uses to register new syntax OIDs */
    private final OidRegistry m_registry ;
    /** a monitor used to track noteable registry events */
    private SyntaxRegistryMonitor m_monitor = null ; 
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates a BootstrapSyntaxRegistry using existing Syntaxes for lookups.
     * 
     * @param a_syntaxes a map of OIDs to their respective Syntax objects
     */
    public BootstrapSyntaxRegistry( Syntax[] a_syntaxes, 
                                    OidRegistry a_registry )
    {
        this ( a_syntaxes, a_registry, new SyntaxRegistryMonitorAdapter() ) ;
    }

        
    /**
     * Creates a BootstrapSyntaxRegistry using existing Syntaxes for lookups.
     * 
     * @param a_syntaxes a map of OIDs to their respective Syntax objects
     */
    public BootstrapSyntaxRegistry( Syntax[] a_syntaxes, 
                                    OidRegistry a_registry,
                                    SyntaxRegistryMonitor a_monitor )
    {
        m_monitor = a_monitor ; 
        m_registry = a_registry ;
        m_syntaxes = new HashMap() ;
        
        for ( int ii = 0; ii < a_syntaxes.length; ii++ )
        {
            m_syntaxes.put( a_syntaxes[ii].getOid(), a_syntaxes[ii] ) ;
            
            m_registry.register( a_syntaxes[ii].getOid(), 
                    a_syntaxes[ii].getOid() ) ;
            if ( a_syntaxes[ii].getName() != null )
            {    
                m_registry.register( a_syntaxes[ii].getName(), 
                        a_syntaxes[ii].getOid() ) ;
            }
            
            m_monitor.registered( a_syntaxes[ii] ) ;
        }
    }
    

    // ------------------------------------------------------------------------
    // SyntaxRegistry interface methods
    // ------------------------------------------------------------------------
    
    
    /**
     * @see org.apache.eve.schema.SyntaxRegistry#lookup(java.lang.String)
     */
    public Syntax lookup( String a_oid ) throws NamingException
    {
        if ( m_syntaxes.containsKey( a_oid ) )
        {
            Syntax l_syntax = ( Syntax ) m_syntaxes.get( a_oid ) ;
            m_monitor.lookedUp( l_syntax ) ;
            return l_syntax ;
        }
        
        NamingException l_fault = new NamingException( "Unknown syntax OID " 
                + a_oid ) ;
        m_monitor.lookupFailed( a_oid, l_fault ) ;
        throw l_fault ;
    }
    

    /**
     * @see org.apache.eve.schema.SyntaxRegistry#register(
     * org.apache.eve.schema.Syntax)
     */
    public void register( Syntax a_syntax ) throws NamingException
    {
        NamingException l_fault = new OperationNotSupportedException( 
                "Syntax registration on read-only bootstrap SyntaxRegistry not "
                + "supported." ) ;
        m_monitor.registerFailed( a_syntax, l_fault ) ;
        throw l_fault ;
    }

    
    /**
     * @see org.apache.eve.schema.SyntaxRegistry#hasSyntax(java.lang.String)
     */
    public boolean hasSyntax( String a_oid )
    {
        return m_syntaxes.containsKey( a_oid ) ;
    }


    // ------------------------------------------------------------------------
    // package friendly monitor methods
    // ------------------------------------------------------------------------
    
    
    /**
     * Gets the monitor for this registry.
     * 
     * @return the monitor
     */
    SyntaxRegistryMonitor getMonitor()
    {
        return m_monitor ;
    }

    
    /**
     * Sets the monitor for this registry.
     * 
     * @param a_monitor the monitor to set
     */
    void setMonitor( SyntaxRegistryMonitor a_monitor )
    {
        m_monitor = a_monitor ;
    }
}
