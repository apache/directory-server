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


import java.util.Comparator ;


/**
 * The default MatchingRule implementation.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class DefaultMatchingRule implements MatchingRule
{
    /** the object identifier */
    private final String m_oid ;
    /** the syntax this matching rule can be applied to */
    private final Syntax m_syntax ;
    /** comparator used to compare and match values of the associated syntax */
    private final Comparator m_comparator ;
    /** normalizer used to transform values to a canonical form */
    private final Normalizer m_normalizer ;

    /** isObsolete boolean flag */
    private boolean m_isObsolete = false ;
    /** a short descriptive name */
    private String m_name = null ;
    /** a description about this MatchingRule */
    private String m_description = null ;
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    
    /**
     * Creates a MatchingRule using the minimal set of required information.
     * 
     * @param a_oid the object identifier for this matching rule
     * @param a_syntax the syntax this matching rule is applicable to
     * @param a_comparator the comparator used by this matching rule to compare
     *      and match values of the associated syntax
     * @param a_normalizer the normalizer used to transform syntax values to a
     *      canonical form.
     */
    public DefaultMatchingRule( String a_oid, 
                                Syntax a_syntax, 
								Comparator a_comparator, 
                                Normalizer a_normalizer )
    {
        m_oid = a_oid ;
        m_syntax = a_syntax ;
        m_comparator = a_comparator ;
        m_normalizer = a_normalizer ;
    }
    

    // ------------------------------------------------------------------------
    // P U B L I C   A C C E S S O R S 
    // ------------------------------------------------------------------------

    
    /**
     * @see org.apache.eve.schema.MatchingRule#getDescription()
     */
    public String getDescription()
    {
        return m_description ;
    }
    

    /**
     * @see org.apache.eve.schema.MatchingRule#getName()
     */
    public String getName()
    {
        return m_name ;
    }

    
    /**
     * @see org.apache.eve.schema.MatchingRule#getOid()
     */
    public String getOid()
    {
        return m_oid ;
    }
    

    /**
     * @see org.apache.eve.schema.MatchingRule#getSyntax()
     */
    public Syntax getSyntax()
    {
        return m_syntax ;
    }
    

    /**
     * @see org.apache.eve.schema.MatchingRule#isObsolete()
     */
    public boolean isObsolete()
    {
        return m_isObsolete ;
    }
    

    /**
     * @see org.apache.eve.schema.MatchingRule#getComparator()
     */
    public Comparator getComparator()
    {
        return m_comparator ;
    }

    
    /**
     * @see org.apache.eve.schema.MatchingRule#getNormalizer()
     */
    public Normalizer getNormalizer()
    {
        return m_normalizer ;
    }


    // ------------------------------------------------------------------------
    // P R O T E C T E D   M U T A T O R S
    // ------------------------------------------------------------------------

    
    /**
     * Sets a short description for this MatchingRule.
     * 
     * @param a_description the description to set
     */
    protected void setDescription(String a_description)
    {
        m_description = a_description;
    }

    
    /**
     * Sets this MatchingRule's isObsolete flag.
     * 
     * @param a_isObsolete whether or not this object is obsolete.
     */
    protected void setObsolete( boolean a_isObsolete )
    {
        m_isObsolete = a_isObsolete ;
    }

    
    /**
     * Sets the short descriptive name for this MatchingRule.
     * 
     * @param a_name The name to set
     */
    protected void setName( String a_name )
    {
        m_name = a_name ;
    }
}
