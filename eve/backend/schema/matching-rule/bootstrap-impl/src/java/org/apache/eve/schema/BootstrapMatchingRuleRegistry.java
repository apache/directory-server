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
 * A MatchingRuleRegistry service available during server startup when other 
 * resources like a system backend for a backing store is unavailable to 
 * solid state registries.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Rev: 6196 $
 */
public class BootstrapMatchingRuleRegistry implements MatchingRuleRegistry
{
    /** a map using an OID for the key and a MatchingRule for the value */
    private final Map m_matchingRules ;
    /** the OID registry used to register new MatchingRule OIDs */
    private final OidRegistry m_registry ;
    /** a monitor used to track noteable registry events */
    private MatchingRuleRegistryMonitor m_monitor = null ; 
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates a BootstrapMatchingRuleRegistry using existing MatchingRulees 
     * for lookups.
     * 
     * @param a_matchingRules a map of OIDs to their respective MatchingRule 
     *      objects
     */
    public BootstrapMatchingRuleRegistry( MatchingRule[] a_matchingRules, 
                                    OidRegistry a_registry )
    {
        this ( a_matchingRules, a_registry, 
               new MatchingRuleRegistryMonitorAdapter() ) ;
    }

        
    /**
     * Creates a BootstrapMatchingRuleRegistry using existing MatchingRulees 
     * for lookups.
     * 
     * @param a_matchingRules a map of OIDs to their respective MatchingRule 
     *      objects
     */
    public BootstrapMatchingRuleRegistry( MatchingRule[] a_matchingRules, 
                                    OidRegistry a_registry,
                                    MatchingRuleRegistryMonitor a_monitor )
    {
        m_monitor = a_monitor ; 
        m_registry = a_registry ;
        m_matchingRules = new HashMap() ;
        
        for ( int ii = 0; ii < a_matchingRules.length; ii++ )
        {
            m_matchingRules.put( a_matchingRules[ii].getOid(), 
                    a_matchingRules[ii] ) ;
            m_registry.register( a_matchingRules[ii].getOid(), 
                    a_matchingRules[ii].getOid() ) ;
            if ( a_matchingRules[ii].getName() != null )
            {    
                m_registry.register( a_matchingRules[ii].getName(), 
                        a_matchingRules[ii].getOid() ) ;
            }
            
            m_monitor.registered( a_matchingRules[ii] ) ;
        }
    }
    

    // ------------------------------------------------------------------------
    // MatchingRuleRegistry interface methods
    // ------------------------------------------------------------------------
    
    
    /**
     * @see org.apache.eve.schema.MatchingRuleRegistry#lookup(java.lang.String)
     */
    public MatchingRule lookup( String a_oid ) throws NamingException
    {
        if ( m_matchingRules.containsKey( a_oid ) )
        {
            MatchingRule l_MatchingRule = ( MatchingRule ) 
                m_matchingRules.get( a_oid ) ;
            m_monitor.lookedUp( l_MatchingRule ) ;
            return l_MatchingRule ;
        }
        
        NamingException l_fault = new NamingException( 
                "Unknown MatchingRule OID " + a_oid ) ;
        m_monitor.lookupFailed( a_oid, l_fault ) ;
        throw l_fault ;
    }
    

    /**
     * @see org.apache.eve.schema.MatchingRuleRegistry#register(
     * org.apache.eve.schema.MatchingRule)
     */
    public void register( MatchingRule a_MatchingRule ) throws NamingException
    {
        NamingException l_fault = new OperationNotSupportedException( 
                "MatchingRule registration on read-only bootstrap " +
                "MatchingRuleRegistry not supported." ) ;
        m_monitor.registerFailed( a_MatchingRule, l_fault ) ;
        throw l_fault ;
    }

    
    /**
     * @see org.apache.eve.schema.MatchingRuleRegistry#hasMatchingRule(
     * java.lang.String)
     */
    public boolean hasMatchingRule( String a_oid )
    {
        return m_matchingRules.containsKey( a_oid ) ;
    }


    // ------------------------------------------------------------------------
    // package friendly monitor methods
    // ------------------------------------------------------------------------
    
    
    /**
     * Gets the monitor for this registry.
     * 
     * @return the monitor
     */
    MatchingRuleRegistryMonitor getMonitor()
    {
        return m_monitor ;
    }

    
    /**
     * Sets the monitor for this registry.
     * 
     * @param a_monitor the monitor to set
     */
    void setMonitor( MatchingRuleRegistryMonitor a_monitor )
    {
        m_monitor = a_monitor ;
    }
}
