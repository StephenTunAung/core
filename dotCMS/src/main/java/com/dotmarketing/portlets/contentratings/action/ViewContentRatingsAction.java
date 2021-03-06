package com.dotmarketing.portlets.contentratings.action;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;

import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;

import com.dotmarketing.util.Logger;
import com.liferay.portal.struts.PortletAction;

public class ViewContentRatingsAction extends PortletAction {

    public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {
        Logger.debug(this, "Going to: portlet.ext.contentratings.view");
        return mapping.findForward("portlet.ext.contentratings.view");
    }
}