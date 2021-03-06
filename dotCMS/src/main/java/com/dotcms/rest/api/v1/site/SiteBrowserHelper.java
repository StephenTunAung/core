package com.dotcms.rest.api.v1.site;

import static com.dotmarketing.util.Logger.error;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Provides all the utility methods used by the {@link SiteBrowserResource}
 * class to provide the required data to the UI layer or any other type of
 * client.
 *
 * @author jsanca
 */
public class SiteBrowserHelper implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final HostAPI hostAPI;
    private final static HostNameComparator HOST_NAME_COMPARATOR =
            new HostNameComparator();

    public static final String EXT_HOSTADMIN = "EXT_HOSTADMIN";

    @VisibleForTesting
    public SiteBrowserHelper (HostAPI hostAPI) {
        this.hostAPI = hostAPI;
    }

    /**
     * Private constructor for the singleton holder.
     */
    private SiteBrowserHelper () {
        this.hostAPI = APILocator.getHostAPI();
    }

    private static class SingletonHolder {
        private static final SiteBrowserHelper INSTANCE = new SiteBrowserHelper();
    }

    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static SiteBrowserHelper getInstance() {
        return SiteBrowserHelper.SingletonHolder.INSTANCE;
    } // getInstance.


    /**
     * Check if a Site is archived or not, keeping the exception quietly
     * @param showArchived {@link Boolean}
     * @param host {@link Host}
     * @return Boolean
     */
    public boolean checkArchived (final boolean showArchived, final Host host) {
        boolean checkArchived = false;
        try {

            checkArchived = (showArchived || !host.isArchived());
        } catch (Exception e) {
            error(SiteBrowserHelper.class, e.getMessage(), e);
        }

        return checkArchived;
    } // checkArchived.

	/**
	 * Returns the list of sites that the given user has access to.
	 *
	 * @param showArchived
	 *            - Is set to {@code true}, archived sites will be returned.
	 *            Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param filter
	 *            - (Optional) If specified, returns the sites whose name starts
	 *            with the value of the {@code filter} variable.
	 * @return The list of sites that the given user has permissions to access.
	 * @throws DotDataException
	 *             An error occurred when retrieving the sites' data.
	 * @throws DotSecurityException
	 *             A system error occurred.
	 */
    public List<Host> getOrderedSites(final boolean showArchived, final User user, final String filter) throws DotDataException, DotSecurityException {
    	return getOrderedSites(showArchived, user, filter, Boolean.FALSE);
    }

	/**
	 * Returns the list of sites that the given user has access to.
	 *
	 * @param showArchived
	 *            - Is set to {@code true}, archived sites will be returned.
	 *            Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param filter
	 *            - (Optional) If specified, returns the sites whose name starts
	 *            with the value of the {@code filter} variable.
	 * @param respectFrontendRoles
	 *            -
	 * @return The list of sites that the given user has permissions to access.
	 * @throws DotDataException
	 *             An error occurred when retrieving the sites' data.
	 * @throws DotSecurityException
	 *             A system error occurred.
	 */
    public List<Host> getOrderedSites(final boolean showArchived, final User user, final String filter, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
    	final String sanitizedFilter = filter != null ? filter : StringUtils.EMPTY;
    	return this.hostAPI.findAll(user, respectFrontendRoles)
                .stream().sorted(HOST_NAME_COMPARATOR)
                .filter (site ->
                        !site.isSystemHost() && checkArchived(showArchived, site) &&
                                (site.getHostname().toLowerCase().startsWith(sanitizedFilter.toLowerCase())))
                .collect(Collectors.toList());
    }
    
    /**
     * Return a site by user and site id
     *
     * @param user User to filter the host to return
     * @param siteId Id to filter the host to return
     * @return host that the given user has permissions and with id equal to hostId, if any exists then return null
     * @throws DotSecurityException if one is thrown when the sites are search
     * @throws DotDataException if one is thrown when the sites are search
     */
    public Host getSite(User user, String siteId) throws DotSecurityException, DotDataException {
        Optional<Host> siteOptional = this.hostAPI.findAll(user, Boolean.TRUE)
                .stream().filter(site -> !site.isSystemHost() && siteId.equals(site.getIdentifier()))
                .findFirst();
        return siteOptional.isPresent() ? siteOptional.get() : null;
    }

	/**
	 * Determines what site is to be marked as "selected" by a user. If the
	 * currently selected site in the HTTP Session is part of the sites that a
	 * user (actual or impersonated user) has access to, the Identifier of such
	 * a site is returned. If the site in the session is not in the list of
	 * sites, the Identifier of the first site in the list must be returned.
	 * 
	 * @param siteList
	 *            - The list of sites (their metadata) that a user has access
	 *            to.
	 * @param siteInSession
	 *            - The Identifier of the site that is marked as selected in the
	 *            current user session.
	 * @return The Identifier of the site that will be marked as "selected".
	 */
	public String getSelectedSite(final List<Map<String, Object>> siteList, final String siteInSession) {
		String selectedSite = UtilMethods.isSet(siteInSession) ? siteInSession : StringUtils.EMPTY;
		boolean siteFound = false;
		if (siteList != null && !siteList.isEmpty()) {
			for (Map<String, Object> siteData : siteList) {
				if (siteData.get(Contentlet.IDENTIFIER_KEY).equals(siteInSession)) {
					siteFound = true;
					break;
				}
			}
			if (!siteFound) {
				selectedSite = siteList.get(0).get(Contentlet.IDENTIFIER_KEY).toString();
			}
		}
		return selectedSite;
	}

}
