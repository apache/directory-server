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


import javax.naming.NamingException ;

import org.apache.avalon.framework.service.Serviceable ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.activity.Initializable ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;
import org.apache.avalon.framework.configuration.Configurable ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;


/**
 * Merlin specific wrapper for the BootstrapSyntaxRegistry.
 *
 * @avalon.component name="bootstrap-syntax-registry" lifestyle="singleton"
 * @avalon.service type="org.apache.eve.schema.SyntaxRegistry" version="1.0"
 * 
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class MerlinBootstrapSyntaxRegistry
    extends AbstractLogEnabled
    implements Configurable, Serviceable, Initializable, SyntaxRegistry
{
    /** the OID registry we need to construct the SyntaxRegistry */
    private OidRegistry m_oidReg ;
    /** the wrapped delegate bootstrap SyntaxRegistry to use */
    private SyntaxRegistry m_synReg ;
    /** the syntaxes to provide lookups on */
    private Syntax[] m_syntaxes ;
    
    
    // ------------------------------------------------------------------------
    // SyntaxRegistry interface methods
    // ------------------------------------------------------------------------

    
    /**
     * @see org.apache.eve.schema.SyntaxRegistry#lookup(java.lang.String)
     */
    public Syntax lookup( String a_oid ) throws NamingException
    {
        return m_synReg.lookup( a_oid ) ;
    }

    
    /**
     * @see org.apache.eve.schema.SyntaxRegistry#register(
     * org.apache.eve.schema.Syntax)
     */
    public void register( Syntax a_syntax ) throws NamingException
    {
        m_synReg.register( a_syntax ) ;
    }

    
    /**
     * @see org.apache.eve.schema.SyntaxRegistry#hasSyntax(java.lang.String)
     */
    public boolean hasSyntax( String a_oid )
    {
        return m_synReg.hasSyntax( a_oid ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // Avalon life-cycle methods
    // ------------------------------------------------------------------------

    
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(
     * org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure( Configuration a_conf ) throws ConfigurationException
    {
        Configuration [] l_syntaxes = a_conf.getChildren( "syntax" ) ;
        AvalonSyntaxFactory l_factory = AvalonSyntaxFactory.getInstance() ;
        m_syntaxes = new Syntax[ l_syntaxes.length ] ;
        
        for ( int ii = 0 ; ii < l_syntaxes.length ; ii++ )
        {
            m_syntaxes[ii] = l_factory.create( l_syntaxes[ii] ) ;
        }
    }
    
    
    /**
     * @avalon.dependency type="org.apache.eve.schema.OidRegistry"
     *         key="oid-registry" version="1.0" 
     * 
     * @see org.apache.avalon.framework.service.Serviceable#service(
     * org.apache.avalon.framework.service.ServiceManager)
     */
    public void service( ServiceManager a_manager ) throws ServiceException
    {
        m_oidReg = ( OidRegistry ) a_manager.lookup( "oid-registry" ) ;
    }


    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception
    {
        AvalonSyntaxRegistryMonitor l_monitor = 
            new AvalonSyntaxRegistryMonitor() ;
        l_monitor.enableLogging( getLogger() ) ;
        BootstrapSyntaxRegistry l_registry = 
            new BootstrapSyntaxRegistry( m_syntaxes, m_oidReg, l_monitor ) ;
        m_synReg = l_registry ;
    }
}
