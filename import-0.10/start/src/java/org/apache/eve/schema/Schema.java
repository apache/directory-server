/*
 * $Id: Schema.java,v 1.10 2003/08/06 04:34:18 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.schema ;


import java.util.Map ;
import java.util.HashMap ;

import javax.naming.NameParser ;
import javax.naming.NamingException ;
import javax.naming.directory.SchemaViolationException ;
import javax.naming.directory.InvalidAttributesException ;

import org.apache.ldap.common.ldif.LdifParser ;
import org.apache.ldap.common.ldif.LdifComposer ;
import org.apache.eve.backend.LdapEntry ;


/**
 * Primitive representation of a schem interface from the perspective of a
 * backend.
 */
public interface Schema
{
    String DN_ATTR = "distinguishedname" ;
    String EXISTANCE_ATTR = "existance" ;
    String HIERARCHY_ATTR = "parentid" ;

    Map BINARY_SYNTAX_OIDS = new HashMap() ;
    Map NUMERIC_SYNTAX_OIDS = new HashMap() ;
    Map DECIMAL_SYNTAX_OIDS = new HashMap() ;

    String normalize( String an_attributeName, String an_attributeValue )
        throws NamingException ;
    Normalizer getNormalizer( String a_key, boolean byName )
        throws NamingException ; 
	void check( LdapEntry an_entry )
        throws SchemaViolationException, InvalidAttributesException ;

    boolean isOperational( String an_attributeName ) ;
    boolean isMultiValue( String an_attributeName ) ;
    boolean isSingleValue( String an_attributeName ) ;
    boolean hasAttribute( String an_attributeName ) ;
    boolean isValidSyntax( String an_attributeName, Object an_attributeValue ) ;
    boolean isValidSyntax( String an_attributeName, 
        Object [] an_attributeValue ) ;
    boolean isBinary( String an_attributeName ) ;
    boolean isNumeric( String an_attributeName ) ;
    boolean isDecimal( String an_attributeName ) ;
    NameParser getNameParser() ;
    NameParser getNormalizingParser() ;
    LdifParser getLdifParser() ;
    LdifComposer getLdifComposer() ;
}
