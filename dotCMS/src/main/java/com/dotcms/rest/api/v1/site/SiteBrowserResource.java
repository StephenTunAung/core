package com.dotcms.rest.api.v1.site;

import static com.dotcms.util.CollectionsUtils.map;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;

/**
 * This resource provides all the different end-points associated to information
 * and actions that the front-end can perform on the Site Browser page.
 * 
 * @author jsanca
 */
@Path("/v1/site")
public class SiteBrowserResource implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String NO_FILTER = "*";

	private final UserAPI userAPI;
    private final WebResource webResource;
    private final SiteBrowserHelper siteBrowserHelper;
    private final I18NUtil i18NUtil;

    public SiteBrowserResource() {
        this(new WebResource(),
                SiteBrowserHelper.getInstance(),
                I18NUtil.INSTANCE, APILocator.getUserAPI());
    }

    @VisibleForTesting
    public SiteBrowserResource(final WebResource webResource,
                               final SiteBrowserHelper siteBrowserHelper,
                               final I18NUtil i18NUtil, final UserAPI userAPI) {
        this.webResource = webResource;
        this.siteBrowserHelper  = siteBrowserHelper;
        this.i18NUtil    = i18NUtil;
        this.userAPI = userAPI;
    }

	/**
	 * Returns the list of Sites that the currently logged-in user has access
	 * to. In the front-end, this list is displayed in the Site Selector
	 * component. Its contents will also be refreshed when performing the "Login
	 * As".
	 * <p>
	 * The site that will be selected in the UI component will be retrieved from
	 * the HTTP session. If such a site does not exist in the list of sites, the
	 * first site in it will be selected.
	 * 
	 * @param req
	 *            - The {@link HttpServletRequest} object.
	 * @return The {@link Response} containing the list of Sites.
	 */
    @GET
    @Path ("/currentSite")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response currentSite(@Context final HttpServletRequest req) {
        final List<Map<String, Object>> siteList;
        Response response = null;
        this.webResource.init(null, true, req, true, null);
        final HttpSession session = req.getSession();
        try {
			// Get user from session, not request. This is required to make this
			// work with the 'Login As' user as well.
			final User user = this.userAPI
					.loadUserById((String) session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID));
            siteList = siteBrowserHelper.getOrderedSites(false, user, StringUtils.EMPTY)
                    .stream()
                    .map(site -> site.getMap())
                    .collect(Collectors.toList());
			final String currentSite = this.siteBrowserHelper.getSelectedSite(siteList,
					(String) session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID));
            response = Response.ok( new ResponseEntityView( map("sites", siteList,
                    "currentSite", currentSite))).build();
        } catch (Exception e) {
        	// Unknown error, so we report it as a 500
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @GET
    @Path ("/filter/{filter}/archived/{archived}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response sites(
                                @Context final HttpServletRequest req,
                                @PathParam("filter")   final String filterParam,
                                @PathParam("archived") final boolean showArchived
                                ) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, req, true, null);
        final List<Map<String, Object>> siteResults;
        final User user = initData.getUser();
         final String filter;

        try {

            Locale locale = LocaleUtil.getLocale(user, req);

            filter = (null != filterParam && filterParam.endsWith(NO_FILTER))?
                    filterParam.substring(0, filterParam.length() - 1):
                    (null != filterParam)? filterParam: StringUtils.EMPTY;

            siteResults = siteBrowserHelper.getOrderedSites(showArchived, user, filter)
                    .stream()
                    .map(site -> site.getMap())
                    .collect(Collectors.toList());

            response = Response.ok(new ResponseEntityView
                    (map(   "result",         siteResults
                            //,"hostManagerUrl", getHostManagerUrl(req, this.layoutAPI.loadLayoutsForUser(user)) // NOTE: this is not needed yet.
                            ),
                     this.i18NUtil.getMessagesMap(locale, "select-host",
                         "select-host-nice-message", "Invalid-option-selected",
                         "manage-hosts", "cancel", "Change-Host"))
                    ).build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // sites.


    @PUT
    @Path ("/switch/{id}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response switchSite(
            @Context final HttpServletRequest req,
            @PathParam("id")   final String hostId
    ) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, req, true, null); // should logged in
        final HttpSession session = req.getSession();
        final User user = initData.getUser();
        boolean switchDone = false;
        Host hostFound = null;

        try {

            if (UtilMethods.isSet(hostId)) {

                // we verified if the host id pass by parameter is one of the user's hosts
                hostFound = siteBrowserHelper.getSite( user, hostId);

                if (hostFound != null) {

                    session.setAttribute(
                            com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, hostId);
                    session.removeAttribute(WebKeys.CONTENTLET_LAST_SEARCH);

                    switchDone = true;
                }
            }

            response = (switchDone) ?
                    Response.ok(new ResponseEntityView(map("hostSwitched",
                            switchDone))).build(): // 200
                    Response.status(Response.Status.NOT_FOUND).build();

        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // sites.

} // E:O:F:SiteBrowserResource.
