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


/**
 * Default Syntax implementation.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class DefaultSyntax implements Syntax
{
    /** the oid of this Syntax */
    private final String m_oid ;
    /** the SyntaxChecker used to enforce this Syntax */
    private final SyntaxChecker m_checker ;

    /** the human readible flag */
    private boolean m_isHumanReadible ;
    /** a short description of this Syntax */
    private String m_description ;
    /** a human readible identifier for this Syntax */
    private String m_name ;
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    
    /**
     * Creates a Syntax object.
     * 
     * @param a_oid the OID for this Syntax
     * @param a_checker the SyntaxChecker used to enforce this Syntax
     */
    public DefaultSyntax( String a_oid, SyntaxChecker a_checker )
    {
        m_oid = a_oid ;
        m_checker = a_checker ;
    }
    
    
    // ------------------------------------------------------------------------
    // Syntax interface methods
    // ------------------------------------------------------------------------

    
    /**
     * @see org.apache.eve.schema.Syntax#isHumanReadable()
     */
    public boolean isHumanReadable()
    {
        return m_isHumanReadible ;
    }

    
    /**
     * @see org.apache.eve.schema.Syntax#getDescription()
     */
    public String getDescription()
    {
        return m_description ;
    }

    
    /**
     * @see org.apache.eve.schema.Syntax#getName()
     */
    public String getName()
    {
        return m_name ;
    }

    
    /**
     * @see org.apache.eve.schema.Syntax#getOid()
     */
    public final String getOid()
    {
        return m_oid ;
    }
    

    /**
     * @see org.apache.eve.schema.Syntax#getSyntaxChecker()
     */
    public SyntaxChecker getSyntaxChecker()
    {
        return m_checker ;
    }


    // ------------------------------------------------------------------------
    // Protected setters
    // ------------------------------------------------------------------------

    
    /**
     * Sets the description for this Syntax.
     * 
     * @param a_description the description to set
     */
    protected void setDescription( String a_description )
    {
        m_description = a_description ;
    }

    
    /**
     * Sets the human readible flag value.
     * 
     * @param a_isHumanReadible the human readible flag value to set
     */
    protected void setHumanReadible( boolean a_isHumanReadible )
    {
        m_isHumanReadible = a_isHumanReadible ;
    }

    
    /**
     * Sets the name of this Syntax.
     * 
     * @param a_name the name to set.
     */
    protected void setName( String a_name )
    {
        m_name = a_name ;
    }
}
