package com.dotcms.rest.api.v1.authentication;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.RestUtilTest;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.IntegrationTestInitService;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyPool;
import com.liferay.portal.ejb.UserManager;
import com.liferay.portal.model.Company;

public class ResetPasswordResourceIntegrationTest{

    HttpServletRequest request;
    ResponseUtil responseUtil;
    ResetPasswordForm  resetPasswordForm;
    
    @BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
        final Company company = new Company() {

            @Override
            public String getAuthType() {

                return Company.AUTH_TYPE_ID;
            }
        };
        CompanyPool.put(RestUtilTest.DEFAULT_COMPANY, company);
	}

    @Before
    public void initTest(){
        request = RestUtilTest.getMockHttpRequest();
        RestUtilTest.initMockContext();
        responseUtil = ResponseUtil.INSTANCE;
        resetPasswordForm = this.getForm();

    }

    @Test
    public void testNoSuchUserException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException, SystemException, PortalException {
        UserManager userManager = getUserManagerThrowingException( new NoSuchUserException("") );
        final JsonWebTokenService jsonWebTokenService = mock(JsonWebTokenService.class);
        final JWTBean jwtBean = new JWTBean("dotcms.org.1",
                "token",
                "dotcms.org.1", 100000);

        when(jsonWebTokenService.parseToken(eq("token"))).thenReturn(jwtBean);
        
        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil, jsonWebTokenService);

        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);

        RestUtilTest.verifyErrorResponse(response,  Response.Status.BAD_REQUEST.getStatusCode(), "please-enter-a-valid-login");

    }

    @Test
    public void testTokenInvalidException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException {
        UserManager userManager = getUserManagerThrowingException( new DotInvalidTokenException("") );
        final JsonWebTokenService jsonWebTokenService = mock(JsonWebTokenService.class);
        final JWTBean jwtBean = new JWTBean("dotcms.org.1",
                "token",
                "dotcms.org.1", 100000);        
        when(jsonWebTokenService.parseToken(eq("token"))).thenReturn(jwtBean);
        
        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil, jsonWebTokenService);
        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);
        RestUtilTest.verifyErrorResponse(response,  Response.Status.BAD_REQUEST.getStatusCode(), "reset-password-token-invalid");
    }

    @Test
    public void testTokenExpiredException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException {
        UserManager userManager = getUserManagerThrowingException( new DotInvalidTokenException("", true) );
        final JsonWebTokenService jsonWebTokenService = mock(JsonWebTokenService.class);
        final JWTBean jwtBean = new JWTBean("dotcms.org.1",
                "token",
                "dotcms.org.1", 100000);

        when(jsonWebTokenService.parseToken(eq("token"))).thenReturn(jwtBean);
        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil, jsonWebTokenService);
        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);

        RestUtilTest.verifyErrorResponse(response,  Response.Status.UNAUTHORIZED.getStatusCode(), "reset-password-token-expired");
    }

    private UserManager getUserManagerThrowingException(Exception e)
            throws NoSuchUserException, DotSecurityException, DotInvalidTokenException {
        UserManager userManager = mock( UserManager.class );
        doThrow( e ).when( userManager ).resetPassword("dotcms.org.1",
                resetPasswordForm.getToken(), resetPasswordForm.getPassword());
        return userManager;
    }

    private ResetPasswordForm getForm(){
        final String password = "admin";
        final String token = "token";

        return new ResetPasswordForm.Builder()
                .password(password)
                .token(token)
                .build();
    }
    
    @AfterClass
    public static void cleanUp(){
    	CompanyPool.remove(RestUtilTest.DEFAULT_COMPANY);
    }
}
