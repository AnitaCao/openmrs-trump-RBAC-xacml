package org.openmrs.module.basicmodule.aop;

import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.annotation.AuthorizedAnnotationAttributes;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.basicmodule.authorization.TmacEnforceServiceImpl;
import org.springframework.aop.MethodBeforeAdvice;

public class AuthorizationAdvice implements MethodBeforeAdvice {
	
	/**
	 * Logger for this class and subclasses
	 */
	protected final Log log = LogFactory.getLog(AuthorizationAdvice.class);
	
	/**
	 * Allows us to check whether a user is authorized to access a particular method.
	 * 
	 * @param method
	 * @param args
	 * @param target
	 * @throws Throwable
	 * @should notify listeners about checked privileges
	 */
	@SuppressWarnings( { "unchecked" })
	public void before(Method method, Object[] args, Object target) throws Throwable {
		if (log.isDebugEnabled()) {
			log.debug("Calling authorization advice before " + method.getName());
		}
		System.err.println("Calling authorization advice before " + method.getName());
		
		//get current user 
		User user = Context.getAuthenticatedUser();
		
		if (log.isDebugEnabled()) {
			log.debug("User " + user);
			if (user != null) {
				log.debug("has roles " + user.getAllRoles());
			}
		}
		
		AuthorizedAnnotationAttributes attributes = new AuthorizedAnnotationAttributes();
		Collection<String> privileges = attributes.getAttributes(method);  //get the strings such as "view patient", which is a combination of action and resource in Luca's code
		
		boolean requireAll = attributes.getRequireAll(method);
		System.out.println("!!!!!requireAll: " + requireAll);
		if (!privileges.isEmpty()) {		
			
			//TmacEnforceServiceImpl pepService = Context.getService(TmacEnforceServiceImpl.class);
			TmacEnforceServiceImpl pepService = new TmacEnforceServiceImpl(args);
			
			
			for (String privilege : privileges) {
				
				if (privilege == null || privilege.isEmpty())
					return;
				
			    if(pepService.isAuthorized(privilege, user)){
			    	//obligation 
			    	
			    	String message = pepService.acceptResponse();
			    	System.out.println("this is the obligation result message : "+message);
			    	
			    	System.out.println(user.getUsername() + "Anita !!!!!!! is Authorized !!!");
			    	if(!requireAll){ 
			    		return; 
			    		}
			    	
			    }
			    
			    else {
					if (requireAll) {
						// if all are required, the first miss causes them
						// to "fail"
						throwUnauthorized(user, method, privilege);
					}
			    }
			}
			if (requireAll == false) {
				// If there's no match, then we know there are privileges and
				// that the user didn't have any of them. The user is not
				// authorized to access the method
				System.out.println("1 . Calling me !");
				throwUnauthorized(user, method, privileges);
				
			}
		}
		else if (attributes.hasAuthorizedAnnotation(method)) {
			
			// if there are no privileges defined, just require that 
			// the user be authenticated
			if (Context.isAuthenticated() == false)
				throwUnauthorized(user, method);
		}		
		
	}
		
	
	/**
	 * Throws an APIAuthorization exception stating why the user failed
	 * 
	 * @param user authenticated user
	 * @param method acting method
	 * @param attrs Collection of String privilege names that the user must have
	 */
	private void throwUnauthorized(User user, Method method, Collection<String> attrs) {
		if (log.isDebugEnabled())
			log.debug("User " + user + " is not authorized to access " + method.getName());
		throw new APIAuthenticationException("Privileges required: " + attrs+ " Sorry! User : " + user.getAllRoles()+" "+user.getUsername()+ " do not have the privilege!");
	}
	
	/**
	 * Throws an APIAuthorization exception stating why the user failed
	 * 
	 * @param user authenticated user
	 * @param method acting method
	 * @param attrs privilege names that the user must have
	 */
	private void throwUnauthorized(User user, Method method, String attr) {
		if (log.isDebugEnabled())
			log.debug("User " + user + " is not authorized to access " + method.getName());
		throw new APIAuthenticationException("Privilege required: " + attr + " Sorry! User : " + user.getName()+ " do not have the privilege!");
	}
	
	/**
	 * Throws an APIAuthorization exception stating why the user failed
	 * 
	 * @param user authenticated user
	 * @param method acting method
	 */
	private void throwUnauthorized(User user, Method method) {
		if (log.isDebugEnabled())
			log.debug("User " + user + " is not authorized to access " + method.getName());
		throw new APIAuthenticationException("Basic authentication required");
	}
	

}

