/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


import org.apache.directory.api.util.FileUtils;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.changelog.Tag;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link TestRule} for processing {@link CreateDS @CreateDS} annotations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CreateDsRule implements TestRule
{
    private static final Logger LOG = LoggerFactory.getLogger( CreateDsRule.class );

    private DirectoryService directoryService;
    private CreateDsRule outerCreateDsRule;


    public CreateDsRule()
    {
    }


    public CreateDsRule( CreateDsRule outerCreateDsRule )
    {
        this.outerCreateDsRule = outerCreateDsRule;
    }


    public DirectoryService getDirectoryService()
    {
        return directoryService == null
            ? ( outerCreateDsRule == null
                ? null
                : outerCreateDsRule.getDirectoryService() )
            : directoryService;
    }


    @Override
    public Statement apply( final Statement base, final Description description )
    {
        final CreateDS createDs = description.getAnnotation( CreateDS.class );
        
        if ( createDs == null )
        {
            final DirectoryService directoryService = getDirectoryService();
            if ( directoryService != null && directoryService.getChangeLog().isEnabled() )
            {
                return new Statement()
                {
                    @Override
                    public void evaluate() throws Throwable
                    {
                        Tag tag = directoryService.getChangeLog().tag();
                        DSAnnotationProcessor.applyLdifs( description, directoryService );
                        LOG.debug( "Tagged change log: {}", tag );
                        try
                        {
                            base.evaluate();
                        }
                        finally
                        {
                            if ( directoryService.getChangeLog().getCurrentRevision() > tag.getRevision() )
                            {
                                LOG.debug( "Reverting to tag: {}", tag );
                                directoryService.revert( tag.getRevision() );
                            }
                            else
                            {
                                LOG.debug( "No changes made, nothing to revert" );
                            }
                        }
                    }
                };
            }
            else
            {
                LOG.trace( "no @CreateDS and no outer @CreateDS on: {}", description );
                return base;
            }
        }
        else
        {
            return new Statement()
            {
                @Override
                public void evaluate() throws Throwable
                {
                    LOG.trace( "Creating directory service" );
                    directoryService = DSAnnotationProcessor.getDirectoryService( description );
                    DSAnnotationProcessor.applyLdifs( description, directoryService );

                    try
                    {
                        base.evaluate();
                    }
                    finally
                    {
                        LOG.trace( "Shutting down directory service" );
                        directoryService.shutdown();
                        FileUtils.deleteDirectory( directoryService.getInstanceLayout().getInstanceDirectory() );
                    }
                }
            };
        }
    }
}
