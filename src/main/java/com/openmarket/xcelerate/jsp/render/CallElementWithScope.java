package com.openmarket.xcelerate.jsp.render;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import COM.FutureTense.Interfaces.ICS;

@SuppressWarnings("serial")
public class CallElementWithScope extends CallElement {
    static Log log = LogFactory.getLog(CallElement.class);

    private String scope;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.openmarket.xcelerate.jsp.render.CallElement#doEndTag(COM.FutureTense
     * .Interfaces.ICS, boolean)
     */
    @Override
    protected int doEndTag(ICS ics, boolean arg1) throws Exception {
        if ("stacked".equalsIgnoreCase(scope)) {
            log.warn(ics.ResolveVariables("CS.elementname") + " called with scoped stacked.");
            this.putAttributeValue("ft_ss", ics.GetVar("ft_ss"));
            this.putAttributeValue("pagename", ics.GetVar("pagename"));
        }
        scope = null;
        return super.doEndTag(ics, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.openmarket.xcelerate.jsp.render.CallElement#setScoped(java.lang.String
     * )
     */
    @Override
    public void setScoped(String scope) {
        this.scope = scope;
        super.setScoped(scope);
    }

}
