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
package org.apache.eve.schema.bootstrap;


import org.apache.ldap.common.schema.AbstractSyntax;
import org.apache.ldap.common.schema.SyntaxChecker;
import org.apache.eve.schema.SyntaxCheckerRegistry;

import javax.naming.NamingException;


/**
 * Document me.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractBootstrapProducer implements BootstrapProducer
{
    private final ProducerTypeEnum type;


    public AbstractBootstrapProducer( ProducerTypeEnum type )
    {
        this.type = type;
    }


    public ProducerTypeEnum getType()
    {
        return type;
    }


    protected static class MutableSyntax extends AbstractSyntax
    {
        final SyntaxCheckerRegistry registry;


        protected MutableSyntax( String oid, SyntaxCheckerRegistry registry )
        {
            super( oid );
            this.registry = registry;
        }


        public void setDescription( String description )
        {
            super.setDescription( description );
        }


        public void setHumanReadible( boolean isHumanReadible )
        {
            super.setHumanReadible( isHumanReadible );
        }


        public void setName( String name )
        {
            super.setName( name );
        }


        public SyntaxChecker getSyntaxChecker( ) throws NamingException
        {
            return registry.lookup( getOid() );
        }
    }
}
