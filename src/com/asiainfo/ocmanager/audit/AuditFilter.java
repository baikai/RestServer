package com.asiainfo.ocmanager.audit;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asiainfo.ocmanager.audit.Audit.Action;
import com.asiainfo.ocmanager.audit.Audit.TargetType;
import com.asiainfo.ocmanager.rest.resource.exception.bean.ResponseExceptionBean;
import com.google.common.base.Strings;

/**
 * Filter for auditting user-end operations
 * 
 * @author Ethan
 *
 */
@Provider
public class AuditFilter implements ContainerResponseFilter {
	private static final Logger LOG = LoggerFactory.getLogger("AuditFilter.audit");
	@Context
	private HttpServletRequest sr;

	@Context
	private ResourceInfo ri;

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		try {
			Audit anno = ri.getResourceMethod().getAnnotation(Audit.class);
			if (anno == null) {
				return;
			}
			String uri = sr.getRequestURI();
			int status = responseContext.getStatus();
			String target = responseContext.getEntityTag() == null ? "tagNull" : responseContext.getEntityTag().getValue();
			String ip = sr.getRemoteAddr();
			String user = isLogin(requestContext) ? target : getUser(requestContext);
			Action action = anno.action();
			TargetType type = anno.targetType();
			Object entity = responseContext.getEntity();
			AuditString audit = new AuditString().status(status).user(user).ip(ip).action(action.name())
					.targetType(type.name()).target(target).uri(uri).desc(extract(entity));
			LOG.info(audit.toString());
		} catch (Exception e) {
			LOG.error("Exception doing auditting: ", e);
		}

	}

	private String extract(Object entity) {
		if (entity instanceof Exception) {
			return ((Exception)entity).getMessage();
		}
		else if (entity instanceof ResponseExceptionBean) {
			return ((ResponseExceptionBean)entity).getException();
		}
		else if (entity instanceof String) {
			return (String)entity;
		}
		return "null";
	}

	/**
	 * audit 403 auth breach
	 * 
	 * @param request
	 */
	public static void audit403(HttpServletRequest request) {
		String ip = request.getRemoteAddr();
		String user = Strings.isNullOrEmpty(request.getHeader("token")) ? "null"
				: request.getHeader("token").split("_")[0];
		String method = request.getMethod();
		String requestUrl = request.getRequestURI();
		AuditString audit = new AuditString().status(403).user(user).ip(ip).action(method)
				.targetType(TargetType.HTTP_REQUEST.name()).target(requestUrl).uri(requestUrl).desc("Auth Forbidden");
		LOG.info(audit.toString());
	}

	private boolean isLogin(ContainerRequestContext requestContext) {
		return requestContext.getUriInfo().getPath().contains("authc/login");
	}

	private String getUser(ContainerRequestContext requestContext) {
		String tokenString = requestContext.getHeaderString("token");
		if (tokenString == null || tokenString.isEmpty()) {
			throw new RuntimeException("Request token is null, headers: " + requestContext.getHeaders());
		}
		return tokenString.split("_")[0];
	}

	private static class AuditString {
		private String template = "user=$USER  ip=$IP  status=$STATUS  action=$ACTION  tType=$TARGETTYPE  tValue=$TARGET  uri=$URI  desc=$DESCRIPTION";

		public AuditString status(int status) {
			template = template.replace("$STATUS", String.valueOf(status));
			return this;
		}

		public AuditString user(String user) {
			if (user == null) {
				return this;
			}
			template = template.replace("$USER", user);
			return this;
		}
		
		public AuditString uri(String uri) {
			if (uri == null) {
				return this;
			}
			template = template.replace("$URI", uri);
			return this;
		}
		
		public AuditString ip(String ip) {
			if (ip == null) {
				return this;
			}
			template = template.replace("$IP", ip);
			return this;
		}

		public AuditString action(String action) {
			if (action == null) {
				return this;
			}
			template = template.replace("$ACTION", action);
			return this;
		}

		public AuditString targetType(String targetType) {
			if (targetType == null) {
				return this;
			}
			template = template.replace("$TARGETTYPE", targetType);
			return this;
		}

		public AuditString target(String target) {
			if (target == null) {
				return this;
			}
			template = template.replace("$TARGET", target);
			return this;
		}

		public AuditString desc(String description) {
			if (description == null) {
				return this;
			}
			template = template.replace("$DESCRIPTION", description);
			return this;
		}

		@Override
		public String toString() {
			return this.template;
		}
	}
}
