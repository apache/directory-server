/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.tools.schema;

import junit.framework.TestCase;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.ldap.common.schema.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;


/**
 * Document me.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeTypesTemplateTest extends TestCase
{
    private FileReader getResourceReader( String res ) throws Exception
    {
        String path = getClass().getResource( res ).getFile() ;
        return new FileReader( path );
    }


    private boolean mkdirs( String base, String path )
    {
        String[] comps = path.split( "/" );
        File file = new File( base );

        if ( ! file.exists() )
        {
            file.mkdirs();
        }

        for ( int ii = 0; ii < comps.length; ii++ )
        {
            file = new File( file, comps[ii] );
            if ( ! file.exists() )
            {
                file.mkdirs();
            }
        }

        return file.exists();
    }


    private FileWriter getResourceWriter( String srcBase, String pkg,
                                          String classname ) throws Exception
    {
        mkdirs( srcBase, pkg.replace( '.', File.separatorChar ) );
        File base = new File( srcBase );
        String relativePath = pkg.replace( '.', File.separatorChar );
        File dir = new File( base, relativePath );
        return new FileWriter( new File( dir, classname + ".java" ) );
    }


    public void testGeneration() throws Exception
    {
        Syntax syntax = new Syntax(){
            public boolean isHumanReadable()
            {
                return false;
            }

            public String getName()
            {
                return null;
            }

            public String getOid()
            {
                return "2.3.3.6";
            }

            public SyntaxChecker getSyntaxChecker()
            {
                return null;
            }

            public String getDescription()
            {
                return null;
            }
        };

        TestAttributeType[] attributeTypes = new TestAttributeType[2];
        attributeTypes[0] = new TestAttributeType( "1.1.1.1" );
        attributeTypes[0].setUsage( UsageEnum.USERAPPLICATIONS );
        attributeTypes[0].setSyntax( syntax );

        attributeTypes[1] = new TestAttributeType( "1.1.1.2" );
        attributeTypes[1].setUsage( UsageEnum.DIRECTORYOPERATION );

        VelocityContext context = new VelocityContext();
        context.put( "package", "org.apache.eve.schema.config" );
        context.put( "classname", "CoreAttributeTypes" );
        context.put( "schema", "core" );
        context.put( "owner", "uid=admin,ou=system" ) ;
        context.put( "schemaDepCount", new Integer( 2 ) );
        context.put( "schemaDeps", new String[] { "dep1", "dep2" }  ) ;
        context.put( "attrTypeCount", new Integer( attributeTypes.length ) );
        context.put( "attrTypes", attributeTypes );

        FileReader template = getResourceReader( "AttributeTypes.template" );
        FileWriter writer = getResourceWriter( "target/schema",
            "org.apache.eve.schema.config", "CoreAttributeTypes" );
        Velocity.init();
        Velocity.evaluate( context, writer, "LOG", template );
        writer.flush();
        writer.close();
    }


    class TestAttributeType extends BaseAttributeType
    {
        protected TestAttributeType( String oid )
        {
            super( oid );
        }

        protected void setSuperior( AttributeType superior )
        {
            super.setSuperior( superior );
        }

        protected void setNameList( ArrayList nameList )
        {
            super.setNameList( nameList );
        }

        protected void setEquality( MatchingRule equality )
        {
            super.setEquality( equality );
        }

        protected void setSubstr( MatchingRule substr )
        {
            super.setSubstr( substr );
        }

        protected void setOrdering( MatchingRule ordering )
        {
            super.setOrdering( ordering );
        }

        protected void setSyntax( Syntax syntax )
        {
            super.setSyntax( syntax );
        }

        protected void setSingleValue( boolean singleValue )
        {
            super.setSingleValue( singleValue );
        }

        protected void setCollective( boolean collective )
        {
            super.setCollective( collective );
        }

        protected void setCanUserModify( boolean canUserModify )
        {
            super.setCanUserModify( canUserModify );
        }

        protected void setObsolete( boolean obsolete )
        {
            super.setObsolete( obsolete );
        }

        protected void setUsage( UsageEnum usage )
        {
            super.setUsage( usage );
        }

        protected void setLength( int length )
        {
            super.setLength( length );
        }

        public String getSuperiorOid()
        {
            return super.getSuperior() != null ? super.getSuperior().getOid() : null;
        }

        public String getSubstrOid()
        {
            return super.getSubstr() != null ? super.getSubstr().getOid() : null;
        }

        public String getOrderingOid()
        {
            return super.getOrdering() != null ? super.getOrdering().getOid() : null;
        }

        public String getEqualityOid()
        {
            return super.getEquality() != null ? super.getEquality().getOid() : null;
        }

        public String getSyntaxOid()
        {
            return super.getSyntax() != null ? super.getSyntax().getOid() : null;
        }
    }
}
