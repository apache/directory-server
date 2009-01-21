/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.schema.syntaxes;


import java.util.List;


/**
 * Utilities for dealing with various schema descriptions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaDescriptionUtils
{
    /**
     * Checks two schema objectClass descriptions for an exact match.
     *
     * @param ocd0 the first objectClass description to compare
     * @param ocd1 the second objectClass description to compare
     * @return true if both objectClasss descriptions match exactly, false otherwise
     */
    public static boolean objectClassesMatch( ObjectClassDescription ocd0, ObjectClassDescription ocd1 )
    {
        // compare all common description parameters
        if ( ! descriptionsMatch( ocd0, ocd1 ) )
        {
            return false;
        }

        // compare the objectClass type (AUXILIARY, STRUCTURAL, ABSTRACT)
        if ( ocd0.getKind() != ocd1.getKind() )
        {
            return false;
        }
        
        // compare the superior objectClasses (sizes must match)
        if ( ocd0.getSuperiorObjectClasses().size() != ocd1.getSuperiorObjectClasses().size() )
        {
            return false;
        }

        // compare the superior objectClasses (sizes must match)
        for ( int ii = 0; ii < ocd0.getSuperiorObjectClasses().size(); ii++ )
        {
            if ( ! ocd0.getSuperiorObjectClasses().get( ii ).equals( ocd1.getSuperiorObjectClasses().get( ii ) ) )
            {
                return false;
            }
        }
        
        // compare the must attributes (sizes must match)
        for ( int ii = 0; ii < ocd0.getMustAttributeTypes().size(); ii++ )
        {
            if ( ! ocd0.getMustAttributeTypes().get( ii ).equals( ocd1.getMustAttributeTypes().get( ii ) ) )
            {
                return false;
            }
        }
        
        // compare the may attributes (sizes must match)
        for ( int ii = 0; ii < ocd0.getMayAttributeTypes().size(); ii++ )
        {
            if ( ! ocd0.getMayAttributeTypes().get( ii ).equals( ocd1.getMayAttributeTypes().get( ii ) ) )
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * Checks two schema attributeType descriptions for an exact match.
     *
     * @param atd0 the first attributeType description to compare
     * @param atd1 the second attributeType description to compare
     * @return true if both attributeType descriptions match exactly, false otherwise
     */
    public static boolean attributeTypesMatch( AttributeTypeDescription atd0, AttributeTypeDescription atd1 )
    {
        // compare all common description parameters
        if ( ! descriptionsMatch( atd0, atd1 ) )
        {
            return false;
        }

        // check that the same super type is being used for both attributes
        if ( ! atd0.getSuperType().equals( atd1.getSuperType() ) )
        {
            return false;
        }
        
        // check that the same matchingRule is used by both ATs for EQUALITY
        if ( ! atd0.getEqualityMatchingRule().equals( atd1.getEqualityMatchingRule() ) )
        {
            return false;
        }
        
        // check that the same matchingRule is used by both ATs for SUBSTRING
        if ( ! atd0.getSubstringsMatchingRule().equals( atd1.getSubstringsMatchingRule() ) )
        {
            return false;
        }
        
        // check that the same matchingRule is used by both ATs for ORDERING
        if ( ! atd0.getOrderingMatchingRule().equals( atd1.getOrderingMatchingRule() ) )
        {
            return false;
        }
        
        // check that the same syntax is used by both ATs
        if ( ! atd0.getSyntax().equals( atd1.getSyntax() ) )
        {
            return false;
        }
        
        // check that the syntax length constraint is the same for both
        if ( atd0.getSyntaxLength() != atd1.getSyntaxLength() )
        {
            return false;
        }
        
        // check that the ATs have the same single valued flag value
        if ( atd0.isSingleValued() != atd1.isSingleValued() )
        {
            return false;
        }
        
        // check that the ATs have the same collective flag value
        if ( atd0.isCollective() != atd1.isCollective() )
        {
            return false;
        }
        
        // check that the ATs have the same user modifiable flag value
        if ( atd0.isUserModifiable() != atd1.isUserModifiable() )
        {
            return false;
        }
        
        // check that the ATs have the same USAGE
        if ( atd0.getUsage() != atd1.getUsage() )
        {
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Checks to see if two matchingRule descriptions match exactly.
     *
     * @param mrd0 the first matchingRule description to compare
     * @param mrd1 the second matchingRule description to compare
     * @return true if the matchingRules match exactly, false otherwise
     */
    public static boolean matchingRulesMatch( MatchingRuleDescription mrd0, MatchingRuleDescription mrd1 )
    {
        // compare all common description parameters
        if ( ! descriptionsMatch( mrd0, mrd1 ) )
        {
            return false;
        }

        // check that the syntaxes of the matchingRules match
        if ( ! mrd0.getSyntax().equals( mrd1.getSyntax() ) )
        {
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Checks to see if two syntax descriptions match exactly.
     *
     * @param lsd0 the first syntax description to compare
     * @param lsd1 the second syntax description to compare
     * @return true if the syntaxes match exactly, false otherwise
     */
    public static boolean syntaxesMatch( LdapSyntaxDescription lsd0, LdapSyntaxDescription lsd1 )
    {
        return descriptionsMatch( lsd0, lsd1 );
    }
    
    
    /**
     * Checks if two base schema descriptions match for the common components 
     * in every schema description.  NOTE: for syntaxes the obsolete flag is 
     * not compared because doing so would raise an exception since syntax 
     * descriptions do not support the OBSOLETE flag.
     * 
     * @param asd0 the first schema description to compare 
     * @param asd1 the second schema description to compare 
     * @return true if the descriptions match exactly, false otherwise
     */
    public static boolean descriptionsMatch( AbstractSchemaDescription asd0, AbstractSchemaDescription asd1 )
    {
        // check that the OID matches
        if ( ! asd0.getNumericOid().equals( asd1.getNumericOid() ) )
        {
            return false;
        }
        
        // check that the obsolete flag is equal but not for syntaxes
        if ( ( asd0 instanceof LdapSyntaxDescription ) || ( asd1 instanceof LdapSyntaxDescription ) )
        {
            if ( asd0.isObsolete() != asd1.isObsolete() )
            {
                return false;
            }
        }
        
        // check that the description matches
        if ( ! asd0.getDescription().equals( asd1.getDescription() ) )
        {
            return false;
        }
        
        // check alias names for exact match
        if ( ! aliasNamesMatch( asd0, asd1 ) )
        {
            return false;
        }
        
        // check extensions for exact match
        if ( ! extensionsMatch( asd0, asd1 ) )
        {
            return false;
        }

        return true;
    }


    /**
     * Checks to see if the extensions of a schema description match another
     * description.  The order of the extension values must match for a true
     * return.
     *
     * @param asd0 the first schema description to compare the extensions of
     * @param asd1 the second schema description to compare the extensions of
     * @return true if the extensions match exactly, false otherwise
     */
    public static boolean extensionsMatch( AbstractSchemaDescription asd0, AbstractSchemaDescription asd1 )
    {
        // check sizes first
        if ( asd0.getExtensions().size() != asd1.getExtensions().size() )
        {
            return false;
        }
        
        // check contents and order of extension values must match
        for ( String key : asd0.getExtensions().keySet() )
        {
            List<String> values0 = asd0.getExtensions().get( key );
            List<String> values1 = asd1.getExtensions().get( key );
            
            // if the key is not present in asd1
            if ( values1 == null )
            {
                return false;
            }
            
            for ( int ii = 0; ii < values0.size(); ii++ )
            {
                if ( ! values0.get( ii ).equals( values1.get( ii ) ) )
                {
                    return false;
                }
            }
        }
        
        return true;
    }
    

    /**
     * Checks to see if the alias names of a schema description match another 
     * description.  The order of the alias names do matter.
     *
     * @param asd0 the schema description to compare
     * @param asd1 the schema description to compare
     * @return true if alias names match exactly, false otherwise
     */
    public static boolean aliasNamesMatch( AbstractSchemaDescription asd0, AbstractSchemaDescription asd1 )
    {
        // check sizes first
        if ( asd0.getNames().size() != asd1.getNames().size() )
        {
            return false;
        }
        
        // check contents and order must match too
        for ( int ii = 0; ii < asd0.getNames().size(); ii++ )
        {
            if ( ! asd0.getNames().get( ii ).equals( asd1.getNames().get( ii ) ) )
            {
                return false;
            }
        }
        
        return true;
    }
}
