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


import java.util.Iterator ;
import java.util.List ;

import javax.naming.NamingException ;

import org.apache.avalon.framework.activity.Initializable ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;


/**
 * Merlin OidRegistry service.
 *
 * @avalon.component name="oid-registry" lifestyle="singleton"
 * @avalon.service type="org.apache.eve.schema.OidRegistry" version="1.0"
 * 
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class MerlinOidRegistry extends AbstractLogEnabled 
    implements OidRegistry, Initializable
{
    /** the delegate OidRegistry implementation this service uses */
    private OidRegistry m_delegate = null ;

    
    // ------------------------------------------------------------------------
    // OidRegistry Interface Methods
    // ------------------------------------------------------------------------ 
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.schema.OidRegistry#getOid(java.lang.String)
     */
    public String getOid( String a_name ) throws NamingException
    {
        return m_delegate.getOid( a_name ) ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.schema.OidRegistry#getPrimaryName(java.lang.String)
     */
    public String getPrimaryName( String a_oid ) throws NamingException
    {
        return m_delegate.getPrimaryName( a_oid ) ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.schema.OidRegistry#getNameSet(java.lang.String)
     */
    public List getNameSet( String a_oid ) throws NamingException
    {
        return m_delegate.getNameSet( a_oid ) ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.schema.OidRegistry#list()
     */
    public Iterator list()
    {
        return m_delegate.list() ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.schema.OidRegistry#register(java.lang.String, 
     * java.lang.String)
     */
    public void register( String a_name, String a_oid )
    {
        m_delegate.register( a_name, a_oid ) ;
    }


    // ------------------------------------------------------------------------
    // Avalon LifeCycle Methods
    // ------------------------------------------------------------------------ 
    

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() 
    {
        DefaultOidRegistry l_delegate = new DefaultOidRegistry() ;
        AvalonOidRegistryMonitor l_monitor = new AvalonOidRegistryMonitor() ;
        l_monitor.enableLogging( getLogger() ) ;
        l_delegate.setMonitor( l_monitor ) ;
        m_delegate = l_delegate ;
    }
}
