/*
 * $Id: SchemaModule.java,v 1.12 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.schema ;


import java.io.File;
import java.io.FileInputStream ;
import java.io.FileNotFoundException ;

import java.util.HashMap ;
import java.util.Iterator ;

import javax.naming.NamingException ;
import javax.naming.NameNotFoundException ;

import org.apache.eve.AbstractModule ;
import org.apache.ldap.common.util.StringTools ;

import antlr.RecognitionException ;
import antlr.TokenStreamException ;

import org.apache.avalon.phoenix.BlockContext ;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.context.Context ;
import org.apache.avalon.framework.context.ContextException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;


/**
 * Schema manager module/block implementation used to parse schema files and
 * manage DIB schemas and subschema authoritative areas.
 * 
 * @phoenix:block
 * @phoenix:service name="org.apache.eve.schema.SchemaManager"
 * @testcase org.apache.eve.schema.TestSchemaModule
 */
public class SchemaModule
    extends AbstractModule implements SchemaManager
{
	public static final String BLOCK_CONFIG_NODE = "schema-manager" ;

    private BlockContext m_context ;
    /** all schema definitions in one place */
    private final SchemaImpl m_complete = new SchemaImpl() ;
    /** syntax OID : SyntaxChecker */
	private final HashMap m_syntaxCheckerMap = new HashMap() ;
    /** equality matching rule : Normalizer */
	private final HashMap m_normalizerMap = new HashMap() ;
    /** schema name : SchemaImpl */
    private final HashMap m_schemaFileMap = new HashMap() ;
    /** suffix : SchemaImpl */
    private final HashMap m_suffixSchemaMap = new HashMap() ;


    public void enableLogging(Logger a_logger)
    {
        super.enableLogging(a_logger) ;
        m_complete.enableLogging(a_logger) ;
    }


    public Schema getCompleteSchema()
    {
        return m_complete ;
    }


	public Schema getSchema(String a_dn)
        throws NamingException
    {
		// Give it a try without wasting cycles.
        if(m_suffixSchemaMap.containsKey(a_dn)) {
            return (Schema) m_suffixSchemaMap.get(a_dn) ;
        }

        // Now normalize the DN and try again.
        String l_canonical = m_complete.normalize(Schema.DN_ATTR, a_dn) ;
        if(m_suffixSchemaMap.containsKey(a_dn)) {
            return (Schema) m_suffixSchemaMap.get(a_dn) ;
        }

        throw new NameNotFoundException("No SAA matches suffix: " + a_dn) ;
    }


    public String getImplementationRole()
    {
        return ROLE ;
    }


	public String getImplementationName()
    {
        return "Schema Manager Module" ;
    }


	public String getImplementationClassName()
    {
        return SchemaModule.class.getName() ;
    }


    public void contextualize(Context a_context)
        throws ContextException
    {
        super.contextualize(a_context) ;

        try {
        	m_context = (BlockContext) a_context ;
            if(getLogger().isDebugEnabled()) {
        		getLogger().debug("Got handle on block context with base "
                    + "directory "
                    + m_context.getBaseDirectory().getAbsolutePath()) ;
            }
        } catch(ClassCastException e) {
			getLogger().debug("Context is not an instance of BlockContext!") ;
            throw new ContextException(
                "Context is not an instance of BlockContext!", e) ;
        }
    }


    /*
		    =========================================
    		E X A M P L E   C O N F I G U R A T I O N
		    =========================================

	<toptag>
    	<!-- S T A G E : I  -->
		<!-- files are parsed first to build m_schemaFileMap -->

    	<schema name="core"
        	filepath="schema/core.schema" />
    	<schema name="corba"
        	filepath="schema/corba.schema" />
    	<schema name="cosine"
        	filepath="schema/cosine.schema" />
    	<schema name="java"
        	filepath="schema/java.schema" />
    	<schema name="misc"
        	filepath="schema/misc.schema" />
    	<schema name="nis"
        	filepath="schema/nis.schema" />
    	<schema name="krb5-kdc"
        	filepath="schema/krb5-kdc.schema" />
    	<schema name="inetorgperson"
        	filepath="schema/inetorgperson.schema" />

    	<!-- S T A G E : II  -->
		<!-- built-ins: deepTrimToLower, deepTrim, trim, dnNormalize, asis -->
        <!-- regex version is perl5 -->

		<normalization default="asis">
        	<normalizer op="deepTrimToLower" rule="caseIgnoreMatch"/>
        	<normalizer op="deepTrimToLower" rule="caseIgnoreListMatch"/>
        	<normalizer op="deepTrimToLower" rule="caseIgnoreIA5Match"/>
        	<normalizer op="deepTrim" rule="caseExactIA5Match"/>
        	<normalizer op="deepTrim" rule="telephoneNumberMatch"/>
        	<normalizer op="dnNormalize" rule="distinguishedNameMatch"/>

            <normalizer rule="someNewBogusRule">
            	<regex value="s/abc/123/g"/>
            	<regex value="/ /d"/>
            	<regex value="y/def/DEF/"/>
            </normalizer>
        </normalization>

    	<!-- S T A G E : III  -->
		<!-- built-ins: accept, . . ., TBA -->
        <!-- regex version is perl5 -->
		<syntax-checkers default="accept">
        	<!-- fictitious: matches yes, Yes, YES etc -->
			<syntax-checker oid="1.2.34.2.1.2.3.4523.1">
            	<regex value="/yes/i"/>
            </syntax-checker>
        </syntax-checkers>

        <!-- S T A G E : IV  -->
	    <!-- Subschema Administrative Area -->
	    <SAA dn="dc=sales,dc=example,dc=com">
	        <schema-ref schema="core"/>
	        <schema-ref schema="inetorgperson"/>
        </SAA>

        <!-- Subschema Administrative Area -->
	    <SAA dn="dc=engineering,dc=example,dc=com">
	        <schema-ref schema="core"/>
	        <schema-ref schema="cosine"/>
	        <schema-ref schema="corba"/>
	        <schema-ref schema="java"/>
	        <schema-ref schema="misc"/>
	        <schema-ref schema="inetorgperson"/>
        </SAA>

        <!-- Subschema Administrative Area -->
	    <SAA dn="nisDomain=unix,dc=example,dc=com">
	        <schema-ref schema="core"/>
	        <schema-ref schema="cosine"/>
	        <schema-ref schema="inetorgperson"/>
	        <schema-ref schema="nis"/>
        </SAA>
	</toptag>

	*/


	public void configure(Configuration a_config)
        throws ConfigurationException
    {
        //
        // Right now the only way to add these files to the sar is via the
        // <lib> or <classes> nexted sar directive in the ant build.  This
        // unfortunately packages the schema files under the
        // apps/ldapd/SAR-INF/lib directory rather than at the top level.
        //
        // I tried to update the sar by using jar to add the schema directory
        // and its contents to the top level directory but updates fail on jar.
        //

		String l_baseDir = m_context.getBaseDirectory().getAbsolutePath() ;
        l_baseDir = l_baseDir + File.separator + "SAR-INF" + File.separator +
            "lib" ;

		// Stage I:
        Configuration [] l_schemaNodes = a_config.getChildren("schema") ;
        String l_schemaName = null ;
        String l_schemaFile = null ;
        SchemaImpl l_schema = null ;
		for(int ii = 0; ii < l_schemaNodes.length; ii++) {
            l_schemaName = l_schemaNodes[ii].getAttribute("name") ;
            l_schemaFile = l_baseDir + File.separator +
                l_schemaNodes[ii].getAttribute("filepath") ;
            l_schema = parse(l_schemaFile) ;
            m_complete.addSchema(l_schema) ;
            m_schemaFileMap.put(l_schemaName, l_schema) ;
        }

        // Stage II:
		Configuration [] l_normalizers =
            a_config.getChild("normalization").getChildren("normalizer") ;
        String l_rule = null ;
        Normalizer l_normalizer = null ;
        for(int ii = 0; ii < l_normalizers.length; ii++) {
			l_rule = l_normalizers[ii].getAttribute("rule").toLowerCase() ;
			l_normalizer = buildNormalizer(l_rule, l_normalizers[ii]) ;
            m_normalizerMap.put(l_rule, l_normalizer) ;
        }


        // Stage III:
		Configuration [] l_syntaxes =
            a_config.getChild("syntax-checkers").getChildren("syntax-checker") ;
        String l_oid = null ;
		SyntaxChecker l_checker = null ;
        for(int ii = 0; ii < l_syntaxes.length; ii++) {
			l_oid = l_syntaxes[ii].getAttribute("oid") ;
			l_checker = buildSyntaxChecker(l_oid, l_syntaxes[ii]) ;
            m_syntaxCheckerMap.put(l_oid, l_checker) ;
        }
        m_complete.setNormalizers(m_normalizerMap) ;
        m_complete.setSyntaxCheckers(m_syntaxCheckerMap) ;
        Iterator l_list = m_schemaFileMap.values().iterator() ;
        while(l_list.hasNext()) {
            l_schema = (SchemaImpl) l_list.next() ;
            l_schema.setNormalizers(m_normalizerMap) ;
            l_schema.setSyntaxCheckers(m_syntaxCheckerMap) ;
        }

        // Stage IV:
        // This stage must be last since we need the normalizers to be able to
        // process the keys for distinguished names used to refer to a SAA
        //

	    Configuration [] l_saaNodes = a_config.getChildren("SAA") ;
        String l_saaDN = null ;
        for(int ii = 0; ii < l_saaNodes.length; ii++) {
            try {
                l_saaDN =
                    m_complete.normalize(Schema.DN_ATTR,
                        l_saaNodes[ii].getAttribute("dn")) ;
            } catch(NamingException e) {
                throw new ConfigurationException("Failed to normalize SAA DN",
                    e) ;
            }

            l_schema = buildSAASchema(l_saaNodes[ii]) ;
            l_schema.setNormalizers(m_normalizerMap) ;
            l_schema.setSyntaxCheckers(m_syntaxCheckerMap) ;
            m_suffixSchemaMap.put(l_saaDN, l_schema) ;
        }
    }


	SyntaxChecker buildSyntaxChecker(String a_oid, Configuration a_syntaxNode)
        throws ConfigurationException
    {
		Configuration [] l_regexes = a_syntaxNode.getChildren("regex") ;
        String [] l_exprArray = new String [l_regexes.length] ;
        for(int ii = 0; ii < l_regexes.length; ii++) {
            l_exprArray[ii] = l_regexes[ii].getAttribute("value") ;
        }

        return new RegexSyntaxChecker(a_oid, l_exprArray) ;
    }


	Normalizer buildNormalizer(final String a_rule, Configuration a_node)
        throws ConfigurationException
    {
		// Regex Form:
		if(null == a_node.getAttribute("op", null)) {
			Configuration [] l_regexes =
				a_node.getChildren("regex") ;
			String [] l_exprArray = new String [l_regexes.length] ;

			for(int jj = 0; jj < l_regexes.length; jj++) {
				l_exprArray[jj] = l_regexes[jj].getAttribute("value") ;
			}

            return new RegexNormalizer(a_rule, l_exprArray) ;
		} else { // Non-Regex Form:
			String l_op = a_node.getAttribute("op", null) ;
            if(l_op.equals("deepTrimToLower")) {
                return new AbstractNormalizer(a_rule, "deepTrimToLower") {
                    public String normalize(String a_value) {
                        return StringTools.deepTrimToLower(a_value) ;
                    }
                } ;
            } else if(l_op.equals("deepTrim")) {
                return new AbstractNormalizer(a_rule, "deepTrim") {
                    public String normalize(String a_value) {
                        return StringTools.deepTrim(a_value) ;
                    }
                } ;
            } else if(l_op.equals("dnNormalize")) {
                return new AbstractNormalizer(a_rule, "dnNormalize") {
                    public String normalize(String a_value)
						throws NamingException
                    {
                        return m_complete.getNormalizingParser().
                            parse(a_value).toString() ;
                    }
                } ;
            } else if(l_op.equals("trim")) {
                return new AbstractNormalizer(a_rule, "trim") {
                    public String normalize(String a_value) {
                        return a_value.trim() ;
                    }
                } ;
            } else {
                return new AbstractNormalizer(a_rule, "asis") {
                    public String normalize(String a_value) {
                        return a_value ;
                    }
                } ;
            }
		}
    }


    public abstract class AbstractNormalizer
        implements Normalizer
    {
        final String m_rule ;
        final String m_func ;

        AbstractNormalizer(String a_rule, String a_func) {
            m_rule = a_rule ;
            m_func = a_func ;
        }

        public String getEqualityMatch() { return m_rule ; }
        public String toString() {
            StringBuffer l_buf = new StringBuffer() ;
            l_buf.append("Normalizer(Rule: ").append(m_rule) ;
            l_buf.append(", Function: ").append(m_func).append(')') ;
            return l_buf.toString() ;
        }
    }


    SchemaImpl buildSAASchema(Configuration a_saaNode)
        throws ConfigurationException
    {
        SchemaImpl l_saaSchema = new SchemaImpl() ;
        l_saaSchema.enableLogging(getLogger()) ;
        SchemaImpl l_fileSchema = null ;
		Configuration [] l_refs = a_saaNode.getChildren("schema-ref") ;
        for(int ii = 0; ii < l_refs.length; ii++) {
            l_fileSchema = (SchemaImpl)
                m_schemaFileMap.get(l_refs[ii].getAttribute("schema")) ;

            Iterator l_attributes = l_fileSchema.listAttributeSpecs() ;
			while(l_attributes.hasNext()) {
                l_saaSchema.addAttributeSpec(
                    (AttributeSpec) l_attributes.next()) ;
            }

            Iterator l_objectclasses = l_fileSchema.listObjectClassSpecs() ;
			while(l_objectclasses.hasNext()) {
                l_saaSchema.addObjectClassSpec(
                    (ObjectClassSpec) l_objectclasses.next()) ;
            }
        }

        return l_saaSchema ;
    }


    SchemaImpl parse(String a_filename)
        throws ConfigurationException
    {
        SchemaImpl l_schema = null ;

        try {
			antlrSchemaSyntaxLexer lexer =
				new antlrSchemaSyntaxLexer( new FileInputStream(a_filename ) ) ;
			l_schema = new SchemaImpl() ;
            l_schema.enableLogging(getLogger()) ;
			antlrSchemaParser l_parser = 
				new antlrSchemaParser(lexer) ;
			l_parser.schemafile(l_schema) ;
        } catch(FileNotFoundException e) {
            throw new ConfigurationException(a_filename + " not found.", e) ;
        } catch(TokenStreamException e) {
            throw new ConfigurationException("parser failed on "
                + a_filename, e) ;
        } catch(RecognitionException e) {
            throw new ConfigurationException("parser failed on "
                + a_filename, e) ;
        }

        return l_schema ;
    }
}
