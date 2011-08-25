package jdbm;

import jdbm.helper.ActionContext;


public interface ActionRecordManager extends RecordManager

{
    ActionContext beginAction( boolean readOnly );
    
    void endAction( ActionContext context );
    
    void abortAction( ActionContext context );
    
    void setCurrentActionContext( ActionContext context );
    
    void unsetCurrentActionContext( ActionContext context );
}
