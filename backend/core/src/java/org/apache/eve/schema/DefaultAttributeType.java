package org.apache.ldap.common.schema ;


import java.io.Serializable ;

import java.util.ArrayList ;
import java.util.Collection ;
import java.util.Comparator ;

import javax.naming.NamingException ;
import javax.naming.directory.Attribute ;
import javax.naming.directory.Attributes ;

import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;

import org.apache.ldap.server.schema.syntax.Syntax ;
import org.apache.ldap.common.NotImplementedException ;
import org.apache.ldap.server.schema.matching.Normalizer ;
import org.apache.ldap.server.schema.syntax.SyntaxChecker ;
import org.apache.ldap.server.schema.syntax.SyntaxManager ;
import org.apache.ldap.server.schema.matching.MatchingRule ;
import org.apache.ldap.server.schema.matching.MatchingRuleManager ;


/**
 * Attribute specification bean used to store the schema information for an
 * attribute definition.
 * 
 */
public class DefaultAttributeType implements Serializable, AttributeType
{
    // ------------------------------------------------------------------------
    // Specification Attributes 
    // ------------------------------------------------------------------------

    /** */
    private String m_oid ;
    /** */
    private ArrayList m_nameList = new ArrayList() ;
    /** */
    private String m_desc ;
    /** */
    private MatchingRule m_equality ;
    /** */
    private MatchingRule m_substr ;
    /** */
    private MatchingRule m_ordering ;
    /** */
    private Syntax m_syntax ;
    /** */
    private boolean m_isSingleValue = false ;
    /** */
    private boolean m_isCollective = false ;
    /** */
    private boolean m_canUserModify = true ;
    /** */
    private UsageEnum m_usage ;
    /** */
    private String m_superior ;
    /** */
    private int m_length = Integer.MAX_VALUE ;


    // ------------------------------------------------------------------------
    // Inferred Attributes 
    // ------------------------------------------------------------------------
    

    /** Directly references other child AttributeSpecs */
    private ArrayList m_children = new ArrayList() ;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    
    
    /**
     * TODO Document me!
     * 
     * @param a_oid TODO
     */
    protected DefaultAttributeType( String a_oid )
    {
        m_oid = a_oid ;
    }
    

    // ------------------------------------------------------------------------
    // Accessor Methods for Specification Properties
    // ------------------------------------------------------------------------

    
    /**
     * Gets the unique IANA registered Object IDentifier (OID) associated with
     * this attribute specification.
     *
     * @return String the object identifier.
     */
    public String getOid()
    {
        return m_oid ;
    }


    /**
     * Gets the first name in the list of names for this AttributeTypeImpl.
     *
     * @return the first name in the list of names
     */
    public String getName()
    {
        return ( String ) m_nameList.get( 0 ) ;
    }
    

    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public Collection getAllNames()
    {
        return m_nameList ;
    }


    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public String getDescription()
    {
        return m_desc ;
    }


    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public String getSyntaxOid()
    {
        return m_syntax.getOid() ;
    }


    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public boolean isSingleValue()
    {
        return m_isSingleValue ;
    }


    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public boolean isCanUserModify()
    {
        return m_canUserModify ;
    }


    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public UsageEnum getUsage()
    {
        return m_usage ;
    }


    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public String getSuperior()
    {
        return m_superior ;
    }


    /**
     * Returns the OID of this attribute.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return m_oid ;
    }


    // ------------------------------------------------------------------------
    //  
    // ------------------------------------------------------------------------
    
    
    /**
     * Gets whether or not his attribute
     * 
     * @return  TODO 
     */
    public boolean isBinary()
    {
        throw new NotImplementedException() ;
    }
    
    
    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public boolean isInteger()
    {
        throw new NotImplementedException() ;
    }


    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public boolean isDecimal()
    {
        throw new NotImplementedException() ;
    }


    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public boolean isString()
    {
        throw new NotImplementedException() ;
    }
    
    
    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public Normalizer getNormalizer()
    {
        return m_equality.getNormalizer() ;
    }
    
    
    /**
     * TODO Document me!
     *
     * @return the sytax checker
     */
    public SyntaxChecker getSyntaxChecker()
    {
        return m_syntax.getSyntaxChecker() ;
    }


    /**
     * 
     * TODO Document me!
     *
     * @return TODO
     */
    public Comparator getComparator()
    {
        if ( null != m_ordering )
        {
            return m_ordering.getComparator() ;
        }
        else if ( null != m_equality )
        {
            return m_equality.getComparator() ;
        }
        
        throw new IllegalStateException( "Must have a comparator for "
            + "AttributeTypeImpl " + m_oid ) ;
    }


    /**
     * TODO Document me!
     *
     * @return TODO
     */
    public ArrayList getChildren()
    {
        return m_children ;
    }


    /**
     * TODO Document me!
     *
     * @return TODO
     */
    public String getDesc()
    {
        return m_desc ;
    }


    /**
     * TODO Document me!
     *
     * @return TODO
     */
    public MatchingRule getEquality()
    {
        return m_equality ;
    }


    /**
     * TODO Document me!
     *
     * @return TODO
     */
    public ArrayList getNameList()
    {
        return m_nameList ;
    }

    /**
     * TODO Document me!
     *
     * @return TODO
     */
    public MatchingRule getOrdering()
    {
        return m_ordering ;
    }

    /**
     * TODO Document me!
     *
     * @return TODO
     */
    public MatchingRule getSubstr()
    {
        return m_substr ;
    }


    /**
     * TODO Document me!
     *
     * @return TODO
     */
    public Syntax getSyntax()
    {
        return m_syntax ;
    }
    
    
    // ------------------------------------------------------------------------
    // M U T A T O R S
    // ------------------------------------------------------------------------


    /**
     * TODO Document me!
     *
     * @param a_b TODO
     */
    public void setCanUserModify( boolean a_b )
    {
        m_canUserModify = a_b ;
    }

    /**
     * TODO Document me!
     *
     * @param a_list TODO
     */
    public void setChildren( ArrayList a_list )
    {
        m_children = a_list ;
    }

    /**
     * TODO Document me!
     *
     * @param a_string TODO
     */
    public void setDesc( String a_string )
    {
        m_desc = a_string ;
    }


    /**
     * TODO Document me!
     *
     * @param a_rule TODO
     */
    public void setEquality( MatchingRule a_rule )
    {
        m_equality = a_rule ;
    }


    /**
     * TODO Document me!
     *
     * @param a_b TODO
     */
    public void setSingleValue( boolean a_b )
    {
        m_isSingleValue = a_b ;
    }


    /**
     * TODO Document me!
     *
     * @param a_list TODO
     */
    public void setNameList( ArrayList a_list )
    {
        m_nameList = a_list ;
    }


    /**
     * TODO Document me!
     *
     * @param a_rule TODO
     */
    public void setOrdering( MatchingRule a_rule )
    {
        m_ordering = a_rule ;
    }

    /**
     * TODO Document me!
     *
     * @param a_rule TODO
     */
    public void setSubstr( MatchingRule a_rule )
    {
        m_substr = a_rule ;
    }


    /**
     * TODO Document me!
     *
     * @param a_string TODO
     */
    public void setSuperior( String a_string )
    {
        m_superior = a_string ;
    }


    /**
     * TODO Document me!
     *
     * @param a_syntax TODO
     */
    public void setSyntax( Syntax a_syntax )
    {
        m_syntax = a_syntax ;
    }


    /**
     * TODO Document me!
     *
     * @param a_usage TODO
     */
    public void setUsage( UsageEnum a_usage )
    {
        m_usage = a_usage ;
    }


    /**
     * TODO Document me!
     * 
     * @return the length of the attribute
     */
    public int getLength()
    {
        return m_length ;
    }


    /**
     * TODO Document me!
     * 
     * @param a_length the length of the attribute
     */
    public void setLength( int a_length )
    {
        m_length = a_length ;
    }
    
    
    /**
     * TODO Document me!
     * 
     * @return true if the attribute is collective, false otherwise
     */
    public boolean isCollective()
    {
        return m_isCollective ;
    }


    /**
     * TODO Document me!
     * 
     * @param a_isCollective true if the attribute is collective, false 
     * otherwise
     */
    public void setCollective( boolean a_isCollective )
    {
        m_isCollective = a_isCollective ;
    }
    
    
    /**
     * @see org.apache.ldap.server.schema.attribute.AttributeType#
     * getAttributeTypeDescription()
     */
    public String getAttributeTypeDescription()
    {
        return "Not implemented yet" ;
    }


    // ------------------------------------------------------------------------
    // Attribute Specification Configuration
    // ------------------------------------------------------------------------
    
    
    /**
     * An AttributeTypeImpl bean builder. 
     */
    public static class Builder
    {
        /**
         * Factory method that builds an AttributeTypeImpl form a configuration.
         *
         * @param a_config the spec configuration
         * @param a_ruleManager $todo Doc me!
         * @param a_syntaxManager $todo Doc me!
         * @return the attribute type definition stored in a configuration
         * @throws ConfigurationException if the configuration is incomplete
         */
        public static AttributeTypeImpl create( Configuration a_config,
            MatchingRuleManager a_ruleManager, SyntaxManager a_syntaxManager )
            throws ConfigurationException
        {
            AttributeTypeImpl l_type = new AttributeTypeImpl( a_config
                .getChild( "oid" ).getValue() ) ;
                
            l_type.setCanUserModify( a_config
                .getChild( "can-user-modify" ).getValueAsBoolean( true ) ) ;
            l_type.setCollective( a_config
                .getChild( "collective" ).getValueAsBoolean( false ) ) ;
            l_type.setDesc( a_config
                .getChild( "description" ).getValue( null ) ) ;
                
            try
            {
                l_type.setEquality( a_ruleManager.lookup( a_config
                    .getChild( "equality" ).getValue() ) ) ;
            }
            catch ( NamingException e )
            {
                throw new ConfigurationException( 
                    "Failed on satisfing dependency for equality MatchingRule "
                    + a_config.getChild( "equality" ).getValue() 
                    + " on AttributeType " + l_type.getOid(), e ) ;
            }
                
            l_type.setLength( a_config
                .getChild( "length" ).getValueAsInteger( Integer.MAX_VALUE ) ) ;
                
                
            Configuration l_ordering = a_config.getChild( "ordering", false ) ;
            if ( null == l_ordering )
            {
                l_type.setOrdering( l_type.getEquality() ) ;
            }
            else
            {
                try 
                {
                    l_type.setOrdering( a_ruleManager.lookup( 
                        l_ordering.getValue() ) ) ;
                }
                catch ( NamingException e )
                {
                    throw new ConfigurationException( "MatchingRule " 
                        + l_ordering.getValue() + " does not exist for "
                        + "attributeType " + l_type.getName(), e ) ;
                }
            }
                
            l_type.setSingleValue( a_config
                .getChild( "single-value" ).getValueAsBoolean( false ) ) ;

            Configuration l_substr = a_config.getChild( "substr", false ) ;
            if ( null == l_substr )
            {
                l_type.setSubstr( l_type.getEquality() ) ;
            }
            else
            {
                try
                {                
                    l_type.setSubstr( a_ruleManager.lookup( 
                        l_substr.getValue( null ) ) ) ;
                }
                catch ( NamingException e )
                {
                    throw new ConfigurationException( 
                        "Failed satisfing dependency for substr MatchingRule "
                        + a_config.getChild( "substr" ).getValue() 
                        + " on AttributeType " + l_type.getOid(), e ) ;
                }
            }
                
            l_type.setSuperior( a_config
                .getChild( "superior", true ).getValue( null ) ) ;

            try
            {                
                l_type.setSyntax( a_syntaxManager.lookup( a_config
                    .getChild( "syntax" ).getValue() ) ) ;
            }
            catch ( NamingException e )
            {
                throw new ConfigurationException( 
                    "Failed on satisfing dependency for Syntax "
                    + a_config.getChild( "syntax" ).getValue() 
                    + " on AttributeType " + l_type.getOid(), e ) ;
            }
                
            l_type.setUsage( UsageEnum.getUsage( a_config
                .getChild( "usage" ).getValue( 
                    UsageEnum.USERAPPLICATIONS.getName() ) ) ) ;
                    
            Configuration [] l_names = a_config
                .getChild( "names" ).getChildren() ;
            ArrayList l_list = new ArrayList( 2 ) ;
            
            for ( int ii = 0; ii < l_names.length; ii++ )
            {
                l_list.add( l_names[ii].getValue() ) ;
            }
            
            l_type.setNameList( l_list ) ;
            return l_type ;
        }


        /**
         * Factory method that builds an AttributeTypeImpl form a configuration.
         *
         * @param a_attributes $todo Doc me!
         * @param a_ruleManager $todo Doc me!
         * @param a_syntaxManager $todo Doc me!
         * @return the attribute type definition stored in a configuration
         * @throws NamingException if the required attributes are not found
         */
        public static AttributeTypeImpl create( Attributes a_attributes,
            MatchingRuleManager a_ruleManager, SyntaxManager a_syntaxManager )
            throws NamingException
        {
            AttributeTypeImpl l_type = null ;
            Attribute l_oid = a_attributes.get( "oid" ) ;
            
            if ( null == l_oid )
            {
                throw new NamingException( "OID required for attributeType" ) ;
            }
            
            l_type = new AttributeTypeImpl( ( String ) l_oid.get() ) ;
            l_type.setCanUserModify( getBoolean( 
                a_attributes.get( "can-user-modify" ) ) ) ;
            l_type.setCollective( getBoolean(
                a_attributes.get( "collective" ) ) ) ;
            l_type.setDesc( ( String ) 
                a_attributes.get( "description" ).get() ) ;
            l_type.setEquality( a_ruleManager.lookup( ( String )
                a_attributes.get( "equality" ).get() ) ) ;
            l_type.setLength( Integer.parseInt( ( String )
                a_attributes.get( "length" ).get() ) ) ;
            l_type.setOrdering( a_ruleManager.lookup( ( String )
                a_attributes.get( "ordering" ).get() ) ) ;
            l_type.setSingleValue( getBoolean( 
                a_attributes.get( "single-value" ) ) ) ;
            l_type.setSubstr( a_ruleManager.lookup( ( String ) 
                a_attributes.get( "substr" ).get() ) ) ;
            l_type.setSuperior( ( String ) 
                a_attributes.get( "superior" ).get() ) ; 
            l_type.setSyntax( a_syntaxManager.lookup( ( String )
                a_attributes.get( "syntax" ).get() ) ) ;
            l_type.setUsage( UsageEnum.getUsage( ( String ) 
                a_attributes.get( "usage" ).get() ) ) ;    
                
            return l_type ;
        }
        
        
        /**
         * Gets a boolean from an Attribute.
         * 
         * @param a_attr the attribute to convert to a boolean
         * @return true if attribute has a '1', 'true' or 'yes' value false
         * otherwise
         * @throws NamingException on failure to access attribute data
         */
        private static boolean getBoolean( Attribute a_attr )
            throws NamingException
        {
            String l_value = ( String ) a_attr.get() ;
            
            return l_value.equals( "1" ) || 
                l_value.equalsIgnoreCase( "true" ) ||
                l_value.equalsIgnoreCase( "yes" ) ;
        }
    }
}
