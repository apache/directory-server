/*
 * $Id: SchemaImpl.java,v 1.14 2003/08/06 04:34:18 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.schema ;


import org.apache.eve.schema.Schema ;
import org.apache.eve.backend.LdapEntry ;
import org.apache.ldap.common.ldif.LdifParser ;
import org.apache.ldap.common.ldif.LdifParserImpl ;
import org.apache.ldap.common.ldif.LdifComposer ;
import org.apache.ldap.common.ldif.LdifComposerImpl ;

import javax.naming.NameParser ;
import javax.naming.directory.SchemaViolationException ;
import javax.naming.directory.InvalidAttributesException ;

import java.util.Map ;
import java.util.HashMap ;
import java.util.Iterator ;

import org.apache.commons.collections.MultiMap ;
import org.apache.commons.collections.MultiHashMap ;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import javax.naming.NamingException;
import org.apache.ldap.common.name.NameComponentNormalizer;
import org.apache.ldap.common.name.DnParser;
import java.io.IOException;
import org.apache.avalon.framework.CascadingRuntimeException;


/**
 * A quick and dirty schema implementation.
 */
public class SchemaImpl
    extends AbstractLogEnabled
    implements Schema
{
    public static boolean debug = false ;

    static {
        // Octet String
        BINARY_SYNTAX_OIDS.put("1.3.6.1.4.1.1466.115.121.1.40",
            "1.3.6.1.4.1.1466.115.121.1.40") ;
        // Certificate Pair
        BINARY_SYNTAX_OIDS.put("1.3.6.1.4.1.1466.115.121.1.10",
            "1.3.6.1.4.1.1466.115.121.1.10") ;
        // JPEG File Interchange Format JFIF
        BINARY_SYNTAX_OIDS.put("1.3.6.1.4.1.1466.115.121.1.28",
            "1.3.6.1.4.1.1466.115.121.1.28") ;
        // Binary
        BINARY_SYNTAX_OIDS.put("1.3.6.1.4.1.1466.115.121.1.5",
            "1.3.6.1.4.1.1466.115.121.1.5") ;
        // Certificate
        BINARY_SYNTAX_OIDS.put("1.3.6.1.4.1.1466.115.121.1.8",
            "1.3.6.1.4.1.1466.115.121.1.8") ;
        // Certificate List
        BINARY_SYNTAX_OIDS.put("1.3.6.1.4.1.1466.115.121.1.9",
            "1.3.6.1.4.1.1466.115.121.1.9") ;
        // Authentication Password Syntax
        // (Not all binary but we can treat it as such)
        BINARY_SYNTAX_OIDS.put("1.3.6.1.4.1.4203.1.1.2",
            "1.3.6.1.4.1.4203.1.1.2") ;

        // INTEGER 1.3.6.1.4.1.1466.115.121.1.27
        NUMERIC_SYNTAX_OIDS.put("1.3.6.1.4.1.1466.115.121.1.27",
            "1.3.6.1.4.1.1466.115.121.1.27") ;
        // Numeric String 1.3.6.1.4.1.1466.115.121.1.36
        NUMERIC_SYNTAX_OIDS.put("1.3.6.1.4.1.1466.115.121.1.36",
            "1.3.6.1.4.1.1466.115.121.1.36") ;
    }

    Map m_syntaxCheckers = new HashMap() ;
    Map m_normalizers = new HashMap() ;

    LdifComposer m_ldifComposer = new LdifComposerImpl() ;
    LdifParser m_ldifParser = new LdifParserImpl() ;

    Map m_attributeByOid = new HashMap() ;
    Map m_attributeByName = new HashMap() ;

    Map m_attribute2super = new HashMap() ;

	MultiMap m_attribute2objectclass = new MultiHashMap() ;
    MultiMap m_attribute2subordinates = new MultiHashMap() ;

    Map m_objectClassByName = new HashMap() ;
    Map m_objectClassByOid = new HashMap() ;

	MultiMap m_objectClass2super = new MultiHashMap() ;
    MultiMap m_objectClass2subordinates = new MultiHashMap() ;

    DnParser m_normalizingParser = null ;
    DnParser m_parser = null ;

    /////////////////
    // Constructor //
    /////////////////


	SchemaImpl()
    {
        NameComponentNormalizer l_dnNormalizer = new NameComponentNormalizer() {
			public String normalizeByName(String a_name, String a_value)
                throws NamingException
            {
                return getNormalizer(a_name, true).normalize(a_value) ;
            }


			public String normalizeByOid(String a_oid, String a_value)
                throws NamingException
            {
                return getNormalizer(a_oid, false).normalize(a_value) ;
            }
        } ;

        try {
            m_parser = new DnParser() ;
        	m_normalizingParser = new DnParser(l_dnNormalizer) ;
        } catch(IOException e) {
            throw new CascadingRuntimeException("Could not initialize DnParser "
                + "for SchemaImpl ", e) ;
        }
    }


	//////////////////////
    // Setup Interfaces //
	//////////////////////


    AttributeSpec getAttributeSpec(String a_key, boolean byName)
        throws NamingException
    {
		AttributeSpec l_spec = null ;

        if(byName) {
			l_spec = (AttributeSpec) m_attributeByName.get(a_key) ;
        } else {
            l_spec = (AttributeSpec) m_attributeByOid.get(a_key) ;
        }

		if(l_spec == null) {
			throw new NamingException("Unknown AttributeType "
				+ a_key) ;
		}

        return l_spec ;
    }


    void setNormalizers(HashMap a_map)
    {
        m_normalizers = a_map ;
    }


    void setSyntaxCheckers(HashMap a_map)
    {
        m_syntaxCheckers = a_map ;
    }


    Iterator listAttributeSpecs()
    {
        return m_attributeByOid.values().iterator() ;
    }


    Iterator listObjectClassSpecs()
    {
        return m_objectClassByOid.values().iterator() ;
    }


    void addSchema(SchemaImpl a_schema)
    {
        Iterator l_list = a_schema.listAttributeSpecs() ;
        while(l_list.hasNext()) {
            addAttributeSpec((AttributeSpec) l_list.next()) ;
        }

        l_list = a_schema.listObjectClassSpecs() ;
        while(l_list.hasNext()) {
            this.addObjectClassSpec((ObjectClassSpec) l_list.next()) ;
        }
    }


    void addAttributeSpec(AttributeSpec an_attribute)
    {
        if(getLogger().isDebugEnabled() && debug) {
            getLogger().debug("Adding attribute spec for " +
                an_attribute.getAllNames()) ;
        }

        m_attributeByOid.put(an_attribute.getOid(), an_attribute) ;

        Iterator l_list = an_attribute.getAllNames().iterator() ;
        String l_name = null ;
        String l_superClass = an_attribute.getSuperClass() ;

        if(l_superClass != null) {
            l_superClass = l_superClass.toLowerCase() ;

            // Equality matching rule should be inherited by topmost superclass
            // if not specified.
            if(null == an_attribute.getEqualityMatch()) {
                AttributeSpec l_superSpec =
                    (AttributeSpec) m_attributeByName.get(l_superClass) ;

                if(l_superSpec == null) {
                    throw new RuntimeException("Attribute spec of super class '"
                        + l_superClass + "' for attribute '" + an_attribute
                        + "' was not found") ;
                } else if(null == l_superSpec.m_equality) {
                    throw new RuntimeException("Super class " + l_superClass
                        + " of " + an_attribute + " does not have an equality "
                        + "matching rule") ;
                } else {
                    an_attribute.m_equality = l_superSpec.m_equality ;
                }
            }
        }

        while(l_list.hasNext()) {
            l_name = ((String) l_list.next()).toLowerCase() ;
        	m_attributeByName.put(l_name, an_attribute) ;

            if(l_superClass != null) {
        		m_attribute2super.put(l_name, l_superClass) ;
                m_attribute2subordinates.put(l_superClass, l_name) ;
            }
        }
    }


    void addObjectClassSpec(ObjectClassSpec an_objectClass)
    {
        if(getLogger().isDebugEnabled() && debug) {
            getLogger().debug("Adding objectclass spec for " +
                an_objectClass.getAllNames()) ;
        }

        try {
			m_objectClassByOid.put(an_objectClass.getOid(), an_objectClass) ;
	
			Iterator l_list = an_objectClass.getAllNames().iterator() ;
			String l_name = null ;
			Iterator l_superClasses =
                an_objectClass.getSuperClasses().iterator() ;
			String l_superClass = null ;
			while(l_list.hasNext()) {
				l_name = ((String) l_list.next()).toLowerCase() ;
				m_objectClassByName.put(l_name, an_objectClass) ;
	
				while(l_superClasses.hasNext()) {
					l_superClass =
                        ((String) l_superClasses.next()).toLowerCase() ;
					if(l_superClass != null) {
						m_objectClass2super.put(l_name, l_superClass) ;
						m_objectClass2subordinates.put(l_superClass, l_name) ;
					}
				}
			}
        } catch(Exception e) {
            e.printStackTrace() ;
        }
    }


    ///////////////////////
    // Schema Interfaces //
    ///////////////////////


    public boolean isBinary(String an_attributeName)
    {
        AttributeSpec l_attribute = (AttributeSpec)
            m_attributeByName.get(an_attributeName.toLowerCase()) ;
        return BINARY_SYNTAX_OIDS.containsKey(l_attribute.getSyntaxOid()) ;
    }


    public boolean isNumeric(String an_attributeName)
    {
        AttributeSpec l_attribute = (AttributeSpec)
            m_attributeByName.get(an_attributeName.toLowerCase()) ;
        return NUMERIC_SYNTAX_OIDS.containsKey(l_attribute.getSyntaxOid()) ;
    }


    public boolean isDecimal(String an_attributeName)
    {
        AttributeSpec l_attribute = (AttributeSpec)
            m_attributeByName.get(an_attributeName.toLowerCase()) ;
        return DECIMAL_SYNTAX_OIDS.containsKey(l_attribute.getSyntaxOid()) ;
    }


    public LdifComposer getLdifComposer()
    {
        return m_ldifComposer ;
    }

    public LdifParser getLdifParser()
    {
        return m_ldifParser ;
    }


    public NameParser getNameParser()
    {
        return m_parser ;
    }

    public NameParser getNormalizingParser()
    {
        return m_normalizingParser ;
    }


    public Normalizer getNormalizer(String a_key, boolean byName)
        throws NamingException
    {
        AttributeSpec l_spec = getAttributeSpec(a_key, byName) ;
        String l_equalityMatch = l_spec.getEqualityMatch() ;
        if(null == l_equalityMatch) {
            throw new SchemaViolationException("Attribute " + a_key
                + " does not have an equality matching rule in its spec") ;
        }

        /*
        if(getLogger().isInfoEnabled()) {
			getLogger().info("SchemaImpl.getNormalizer(): attribute '"
				+ a_key + "' with equality match '" + l_equalityMatch
				+ "' has the following normalizer '"
				+ m_normalizers.get(l_equalityMatch) + "'") ;
        }
        */

        return (Normalizer) m_normalizers.get(l_equalityMatch) ;
    }


	public void check(LdapEntry an_entry)
        throws SchemaViolationException, InvalidAttributesException
    {
        throw new RuntimeException("N O T   I M P L E M E N T E D   Y E T !") ;
    }


    public boolean hasAttribute(String an_attributeName)
    {
        return m_attributeByName.containsKey(an_attributeName.toLowerCase()) ;
    }
    
    
    public boolean isOperational( String an_attributeName )
    {
        /* 
         * There are two kinds of operational attributes:
         *      1). Those defined by the protocol
         *      2). Those defined by backends for their own use
         * 
         * The schema and schema manager should only have to be aware of those
         * operational attributes that are defined by the protocol.  Backend
         * specific attributes should be managed by the backend.  Outside of the
         * backend these attributes are just regular user attributes.  The 
         * functionality of filtering out operational attributes on search 
         * entry responses is the job of both the backend and the search 
         * processing code.  Each should do it for the attributes under its
         * jurisdiction.  Here we breach this sensible contract by having the
         * schema manager be aware of backend operational attributes.  
         * 
         * TODO this method pretends to know about the special operational 
         * attributes used by the modjdbm backend and hence is imposing a 
         * dependency.  The sooner operational or attribute filtering is
         * implemented within the backend the better.
         */
         
         if ( 
            an_attributeName.toLowerCase().equals( Schema.DN_ATTR ) ||
            an_attributeName.toLowerCase().equals( Schema.HIERARCHY_ATTR ) ||
            an_attributeName.toLowerCase().equals( "parentdn" ) ||
            an_attributeName.toLowerCase().equals( "entryid" )
            )
         {
             return true ;
         }
         
        AttributeSpec l_attribute = ( AttributeSpec )
            m_attributeByName.get( an_attributeName.toLowerCase() ) ;
            
        if ( l_attribute == null )
        {
            return false ;
        }
        else if ( l_attribute.getUsage() == null )
        {
            return false ;
        }
        else if ( l_attribute.getUsage().toLowerCase().equals( "directoryoperation" ) )
        {
            return true ;
        }
        else 
        {
            return false ;
        }
    }


    public boolean isMultiValue(String an_attributeName)
    {
        AttributeSpec l_attribute = (AttributeSpec)
            m_attributeByName.get(an_attributeName.toLowerCase()) ;
        return !l_attribute.isSingleValue() ;
    }


    public boolean isSingleValue(String an_attributeName)
    {
        AttributeSpec l_attribute = (AttributeSpec)
            m_attributeByName.get(an_attributeName.toLowerCase()) ;
        return l_attribute.isSingleValue() ;
    }


    public boolean isValidSyntax(String an_attributeName,
        Object [] an_attributeValue)
    {
        // We need to create regular expressions for the various
        // syntax types and use them to see if we match.
        //throw new RuntimeException("N O T   I M P L E M E N T E D   Y E T !") ;

        //getLogger().warn("N O T   I M P L E M E N T E D   Y E T :\n\t" +
        //    "SchemaImpl.isValidSyntax() ") ;
        return true ;
    }


    public boolean isValidSyntax(String an_attributeName,
        Object an_attributeValue)
    {
        // We need to create regular expressions for the various
        // syntax types and use them to see if we match.
        //throw new RuntimeException("N O T   I M P L E M E N T E D   Y E T !") ;

        // getLogger().warn("N O T   I M P L E M E N T E D   Y E T :\n\t" +
        //    "SchemaImpl.isValidSyntax() ") ;
        return true ;
    }


    public String normalize(String an_attributeName, String an_attributeValue)
        throws NamingException
    {
        if(getLogger().isDebugEnabled() && debug) {
            getLogger().debug("normalize called on attribute " +
                an_attributeName + " with value " + an_attributeValue) ;
            getLogger().debug("Normalizer map = " + m_normalizers) ;
        }

        AttributeSpec l_attribute = (AttributeSpec)
            m_attributeByName.get(an_attributeName.toLowerCase()) ;

		if(getLogger().isDebugEnabled() && an_attributeName.equals("parentId")
             && debug)
        {
            getLogger().debug("Dumping attribute keys in schema") ;
            Iterator l_list = m_attributeByName.keySet().iterator() ;
            while(l_list.hasNext()) {
                String l_name = (String) l_list.next() ;
                if(l_name.startsWith("p"))
                	getLogger().debug(l_name) ;
            }
        }

        if(getLogger().isDebugEnabled() && debug) {
            getLogger().debug("spec for attribute " +
                an_attributeName + " = " + l_attribute) ;
            getLogger().debug("Equality Match Rule for attribute " +
                an_attributeName + " = " + l_attribute.getEqualityMatch()) ;
        }

        String l_equalityMatch = l_attribute.getEqualityMatch() ;
        if(null != l_equalityMatch) {
            l_equalityMatch = l_equalityMatch.toLowerCase() ;
        } else {
            if(getLogger().isDebugEnabled() && debug) {
                getLogger().debug("Equality matching rule not defined for "
                    + "attribute " + an_attributeName + " returning value '" +
                    an_attributeValue + "' as is") ;
            }

            return an_attributeValue ;
        }

        if(!m_normalizers.containsKey(l_equalityMatch)) {
            if(getLogger().isDebugEnabled() && debug) {
                getLogger().debug("Could not find normalizer for attribute "
                    + an_attributeName + " with equality matching rule "
                    + l_equalityMatch + " returning value '" +
                    an_attributeValue + "' as is") ;
                getLogger().debug(getNormalizerDump()) ;
            }
            return an_attributeValue ;
        }

        Normalizer l_normalizer = (Normalizer)
            m_normalizers.get(l_attribute.getEqualityMatch().toLowerCase()) ;
        String l_canonical = l_normalizer.normalize(an_attributeValue) ;

        if(getLogger().isDebugEnabled() && debug) {
            getLogger().debug("Found normalizer for attribute "
                + an_attributeName + " returning normalized value '" +
                l_canonical + "'.") ;
        }

        return l_canonical ;
    }

	/** JUST FARTING AROUND */
    public String toString()
    {
        String l_str = "\n\nAttribute Subordinates Map:\n\n"
            + "==============================================================\n"
        	+ m_attribute2subordinates.toString() + "\n\n"
            + "Attribute SuperClass Map: "
            + "==============================================================\n"
            + m_attribute2super.toString() + "\n\n"
            + "Attribute By Name Map: "
            + "==============================================================\n"
            + m_attributeByName.toString() + "\n\n"
            + "Attribute By OID Map: "
            + "==============================================================\n"
            + m_attributeByOid.toString()
			+ "Object Class Subordinates Map:\n\n"
            + "==============================================================\n"
        	+ m_objectClass2subordinates.toString() + "\n\n"
            + "Object Class SuperClass Map: "
            + "==============================================================\n"
            + m_objectClass2super.toString() + "\n\n"
            + "Object Class By Name Map: "
            + "==============================================================\n"
            + m_objectClassByName.toString() + "\n\n"
            + "Object Class By OID Map: "
            + "==============================================================\n"
            + m_objectClassByOid.toString() ;

        return l_str ;
    }


	String getNormalizerDump()
    {
        StringBuffer l_buf = new StringBuffer() ;
        l_buf.append("Normalizer Key Dump:\n") ;
        Iterator l_list = m_normalizers.keySet().iterator() ;

        while(l_list.hasNext()) {
            l_buf.append("\t").append(l_list.next()).append("\n") ;
        }

        l_buf.append("\nDump Complete\n") ;

        return l_buf.toString() ;
    }
}
