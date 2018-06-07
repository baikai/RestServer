package com.asiainfo.ocmanager.rest.resource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asiainfo.ocmanager.audit.Audit;
import com.asiainfo.ocmanager.audit.Audit.Action;
import com.asiainfo.ocmanager.audit.Audit.TargetType;
import com.asiainfo.ocmanager.auth.Authenticator;
import com.asiainfo.ocmanager.rest.bean.LoginResponseBean;
import com.asiainfo.ocmanager.rest.constant.Constant;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Created by gq on 17/7/17.
 */
@Path("/authc")
public class AuthcResource {
	private static Logger logger = LoggerFactory.getLogger(AuthcResource.class);

	@POST
	@Path("login")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.LOGIN, targetType = TargetType.USER)
	public Response login(String requestBody) {
		String username = null;
		try {
			JsonElement req = new JsonParser().parse(requestBody);
			JsonObject obj = req.getAsJsonObject();
			if (!(obj.has("username") && obj.has("password"))) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new LoginResponseBean("Login failed!",
								"username or password doesn't exist in request body!",
								Response.Status.BAD_REQUEST.getStatusCode(), null))
						.build();
			}
			username = obj.get("username").getAsString();
			String password = obj.get("password").getAsString();

			Authenticator authenticator = new Authenticator();
			if (authenticator.loginWithUsernamePassword(username, password)) {
				String token = Authenticator.generateToken(username, password);
				logger.info("login successful. Token: " + token);
				return Response.ok().entity(new LoginResponseBean("Login successful!",
						"login successful", 200, token)).tag(username).build();
			} else {
				logger.info("login failed.");
				return Response.status(Response.Status.UNAUTHORIZED)
						.entity(new LoginResponseBean("Login failed!", "Invalid username or password.", 401, null))
						.tag(username).build();
			}
		} catch (Exception e) {
			logger.error("Exception during login: ", e);
			//e.printStackTrace();
			return Response.status(Response.Status.BAD_REQUEST).entity(new LoginResponseBean("Login failed!",
					e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode(), null)).tag(username).build();
		}
	}

	@DELETE
	@Path("logout/{username}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.LOGOUT, targetType = TargetType.USER)
	public Response logout(@PathParam("username") String username, @Context HttpServletRequest request) {
		try {
			String token = request.getHeader("token");
			if (!username.equals(token.split("_")[0])) {
				throw new Exception("Username and token doesn't match!");
			}
			Authenticator.logout(username);
			logger.info("User [{}] logout successfully!", username);
			return Response.ok().entity(new LoginResponseBean("Logout successful!", null, 200, null)).tag(username).build();
		} catch (Exception e) {
			logger.error("User [{}] logout failed : {}", username, e);
			return Response.status(Response.Status.BAD_REQUEST).entity(e.toString()).tag(username).build();
		}
	}

	@GET
	@Path("type")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	public Response getType() {
		try {
			return Response.ok().entity(toTypeString(Authenticator.getAuthType())).build();
		} catch (Exception e) {
			logger.error("Exception while get auth type: ", e);
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	private String toTypeString(int type) {
		String temp = "{\"type\":${type}}";
		return temp.replace("${type}", String.valueOf(type));
	}

}
