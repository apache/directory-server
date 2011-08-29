package jdbm.helper;

/**
 * Used to store Action specific context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ActionContext
    {

        /** track whether action is read only */ 
        boolean readOnly;
        
        /** Version associated with the context */
        ActionVersioning.Version version;
        
        /** Who started the action. Usefule for debugging */
        String whoStarted;
        
        public void beginAction( boolean readOnly, ActionVersioning.Version version, String whoStarted )
        {
            this.readOnly = readOnly;
            this.version = version;
            this.whoStarted = whoStarted;
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
        
        public String getWhoStarted()
        {
            return whoStarted;
        }
    
        
    }
    