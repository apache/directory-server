package jdbm.helper;

/**
 * Used to store Action specific context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ActionContext
    {

        boolean readOnly;
        ActionVersioning.Version version;
        
        public void beginAction( boolean readOnly, ActionVersioning.Version version )
        {
            this.readOnly = readOnly;
            this.version = version;
        }
        
        public void endAction()
        {
            assert( version != null );
            version = null;
        }
        
        public boolean isReadOnlyAction()
        {
            return ( readOnly && this.version != null );
        }
        
        public boolean isWriteAction()
        {
            return ( !readOnly && this.version != null );
            
        }
        
        public boolean isActive()
        {
            return ( this.version != null );
        }
        
        public ActionVersioning.Version getVersion()
        {
            return version;
        }
    
        
    }
    