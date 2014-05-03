package org.apache.directory.server.core.integ;


import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.changelog.Tag;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CreateDsRule implements TestRule
{
    private static Logger LOG = LoggerFactory.getLogger( CreateDsRule.class );

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
