package com.asiainfo.ocmanager.rest.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.asiainfo.ocmanager.audit.Audit;
import com.asiainfo.ocmanager.audit.Audit.Action;
import com.asiainfo.ocmanager.audit.Audit.TargetType;
import com.asiainfo.ocmanager.auth.Authenticator;
import com.asiainfo.ocmanager.auth.LdapWrapper;
import com.asiainfo.ocmanager.auth.utils.TokenPaserUtils;
import com.asiainfo.ocmanager.persistence.model.ServiceInstance;
import com.asiainfo.ocmanager.persistence.model.Tenant;
import com.asiainfo.ocmanager.persistence.model.User;
import com.asiainfo.ocmanager.persistence.model.UserRoleView;
import com.asiainfo.ocmanager.rest.bean.AssignmentInfoBean;
import com.asiainfo.ocmanager.rest.bean.IsAdminBean;
import com.asiainfo.ocmanager.rest.bean.PasswordBean;
import com.asiainfo.ocmanager.rest.bean.ResourceResponseBean;
import com.asiainfo.ocmanager.rest.bean.UserRoleViewBean;
import com.asiainfo.ocmanager.rest.bean.UserWithTURBean;
import com.asiainfo.ocmanager.rest.constant.Constant;
import com.asiainfo.ocmanager.rest.constant.ResponseCodeConstant;
import com.asiainfo.ocmanager.rest.resource.persistence.ServiceInstancePersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.TenantPersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.UserPersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.UserRoleViewPersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.utils.TenantUtils;
import com.asiainfo.ocmanager.service.client.ClusterFactory;
import com.asiainfo.ocmanager.service.client.RangerClient;
import com.asiainfo.ocmanager.service.client.RangerClient.UserExistedException;
import com.asiainfo.ocmanager.utils.ClusterConfig;
import com.asiainfo.ocmanager.utils.ClustersIni;
import com.asiainfo.ocmanager.utils.TenantTree;
import com.asiainfo.ocmanager.utils.TenantTree.TenantTreeNode;
import com.asiainfo.ocmanager.utils.TenantTreeUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

/**
 *
 * @author zhaoyim
 *
 */

@Path("/user")
public class UserResource {

	private static Logger logger = Logger.getLogger(UserResource.class);

	/**
	 * Get All OCManager users with tenants
	 *
	 * @return user list
	 */
	@GET
	@Path("/with/tenants")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	public Response getUsersWithTenants() {
		try {
			List<User> users = UserPersistenceWrapper.getUsers();
			List<UserWithTURBean> usersWithTenants = new ArrayList<UserWithTURBean>();
			for (User u : users) {

				List<UserRoleViewBean> urvbs = new ArrayList<UserRoleViewBean>();
				List<UserRoleView> urvs = UserRoleViewPersistenceWrapper.getTenantAndRoleBasedOnUserId(u.getId());

				for (UserRoleView urv : urvs) {
					UserRoleViewBean urvb = new UserRoleViewBean(urv);
					String parentTenantName = UserResource.getParentTenantName(urv.getTenantId());
					urvb.setParentTenantName(parentTenantName);
					urvbs.add(urvb);
				}

				UserWithTURBean userBean = new UserWithTURBean(u);
				userBean.setUrv(urvbs);
				usersWithTenants.add(userBean);
			}

			return Response.ok().entity(usersWithTenants).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getUsersWithTenants hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).build();
		}
	}

	/**
	 * Get the specific user with tenants by id
	 *
	 * @param userId
	 *            user id
	 * @return user
	 */
	@GET
	@Path("{id}/with/tenants")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	public Response getUserWithTenantsById(@PathParam("id") String userId) {
		try {
			User user = UserPersistenceWrapper.getUserById(userId);

			if (user == null) {
				return Response.status(Status.NOT_FOUND).entity("The user " + userId + "can not find.").build();
			}
			List<UserRoleViewBean> urvbs = new ArrayList<UserRoleViewBean>();
			List<UserRoleView> urvs = UserRoleViewPersistenceWrapper.getTenantAndRoleBasedOnUserId(userId);

			for (UserRoleView urv : urvs) {
				UserRoleViewBean urvb = new UserRoleViewBean(urv);
				String parentTenantName = UserResource.getParentTenantName(urv.getTenantId());
				urvb.setParentTenantName(parentTenantName);
				urvbs.add(urvb);
			}

			UserWithTURBean userBean = new UserWithTURBean(user);
			userBean.setUrv(urvbs);
			return Response.ok().entity(userBean).build();

		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getUserWithTenantsById hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).build();
		}
	}

	/**
	 * Get All OCManager users
	 *
	 * @return user list
	 */
	@GET
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.USERS)
	public Response getUsers() {
		try {
			List<User> users = UserPersistenceWrapper.getUsers();
			return Response.ok().entity(users).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getUsers hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).build();
		}
	}

	/**
	 * Get All OCManager users
	 *
	 * @return user list
	 */
	@GET
	@Path("ldap")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.LDAP_USERS)
	public Response listLdapUsers() {
		try {
			List<String> users = LdapWrapper.allUsers();
			return Response.ok().entity(users).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getUsers hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).build();
		}
	}

	/**
	 * Get the specific user by id
	 *
	 * @param userId
	 *            user id
	 * @return user
	 */
	@GET
	@Path("id/{id}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.USER)
	public Response getUserById(@PathParam("id") String userId) {
		try {
			User user = UserPersistenceWrapper.getUserById(userId);
			return Response.ok().entity(user == null ? new User() : user).tag(userId).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getUserById hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(userId).build();
		}
	}

	@GET
	@Path("name/{userName}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.USER)
	public Response getUserByName(@PathParam("userName") String userName) {
		try {
			User user = UserPersistenceWrapper.getUserByName(userName);
			return Response.ok().entity(user == null ? new User() : user).tag(userName).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getUserById hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(userName).build();
		}
	}

	/**
	 * Create a new user
	 *
	 * @param user
	 *            user obj json
	 * @return new user info
	 */
	@POST
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.CREATE, targetType = TargetType.USER)
	public Response createUser(User user, @Context HttpServletRequest request) {
		try {
			String token = request.getHeader("token");
			if (token == null || token.isEmpty()) {
				return Response.status(Status.NOT_FOUND)
						.entity(new ResourceResponseBean("create user failed",
								"token is null or empty, please check the token in request header.",
								ResponseCodeConstant.EMPTY_TOKEN))
						.tag(user.getUsername()).build();
			}

			String loginUser = TokenPaserUtils.paserUserName(token);
			logger.debug("createUser -> create user with login user: " + loginUser);

			UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser,
					Constant.ROOTTENANTID);

			if (role == null || !(role.getRoleName().equals(Constant.SYSADMIN))) {
				return Response.status(Status.UNAUTHORIZED)
						.entity(new ResourceResponseBean("create user failed",
								"the user is not system admin role, does NOT have the create user permission.",
								ResponseCodeConstant.NO_CREATE_USER_PERMISSION))
						.tag(user.getUsername()).build();
			}

			if (!appendUser2Rangers(user)) {
				logger.error("Failed to append user to Ranger: " + user);
				return Response.status(Status.BAD_REQUEST).entity("Failed to append user to Ranger: " + user)
						.tag(user.getUsername()).build();
			}

			user = UserPersistenceWrapper.createUser(user);
			return Response.ok().entity(user).tag(user.getUsername()).build();

		} catch (Exception e) {
			if (e.getCause() instanceof MySQLIntegrityConstraintViolationException) {
				MySQLIntegrityConstraintViolationException newExp = (MySQLIntegrityConstraintViolationException) e
						.getCause();
				if (newExp.getErrorCode() == 1062) {
					return Response.status(Status.CONFLICT)
							.entity(new ResourceResponseBean("create user failed",
									"the user" + user.getUsername() + "is already existed",
									ResponseCodeConstant.USER_EXIST))
							.tag(user.getUsername()).build();
				}
			}

			// system out the exception into the console log
			logger.error("createUser hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(user.getUsername()).build();

		}
	}

	public static void main(String[] args) {
		User user = new User();
		user.setUsername("ethanwang1");
		try {
			new UserResource().appendUser2Rangers(user);
		} catch (Exception e) {
			logger.error("Exception while main(): ", e);
		}
		System.out.println(">>>end of main.");
	}
	
	private boolean appendUser2Rangers(User user) throws Exception {
		Map<String, ClusterConfig> clusters = ClustersIni.getInstance().getClusters();
		clusters.forEach((k,v) -> {
			RangerClient ranger = ClusterFactory.getRanger(k);
			/*
			 * 1. user not exist in ranger and append succeed 
			 * 2. user not exist and append exception 
			 * 3. user existed in ranger and append failed
			 */
			try {
				ranger.addUser(user);
			} catch (UserExistedException e) {
				logger.warn("User already existed: " + user);
			} catch (Exception e) {
				logger.error("Append user to ranger failed: ", e);
				throw new RuntimeException(e);
			}
		});
		return true;
	}

	/**
	 * Update the existing user info
	 *
	 * @param user
	 *            user obj json
	 * @return updated user info
	 */
	@PUT
	@Path("id/{userId}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.UPDATE, targetType = TargetType.USER)
	public Response updateUserById(@PathParam("userId") String userId, User user, @Context HttpServletRequest request) {
		try {
			String token = request.getHeader("token");
			if (token == null || token.isEmpty()) {
				return Response.status(Status.NOT_FOUND)
						.entity(new ResourceResponseBean("update user failed",
								"token is null or empty, please check the token in request header.",
								ResponseCodeConstant.EMPTY_TOKEN))
						.tag(user.getUsername()).build();
			}

			String loginUser = TokenPaserUtils.paserUserName(token);
			logger.debug("updateUserById -> update user with login user: " + loginUser);

			UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser,
					Constant.ROOTTENANTID);

			User currentUser = UserPersistenceWrapper.getUserById(userId);

			if (!(loginUser.equals(currentUser.getUsername()))) {
				if (role == null || !(role.getRoleName().equals(Constant.SYSADMIN))) {
					return Response.status(Status.UNAUTHORIZED)
							.entity(new ResourceResponseBean("update user failed",
									"the user is not system admin role, does NOT have the update user permission.",
									ResponseCodeConstant.NO_UPDATE_USER_PERMISSION))
							.tag(user.getUsername()).build();
				}
			}

			user.setId(userId);
			user = UserPersistenceWrapper.updateUser(user);
			return Response.ok().entity(user).tag(user.getUsername()).build();

		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("updateUserById hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(user.getUsername()).build();
		}
	}

	@PUT
	@Path("name/{userName}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.UPDATE, targetType = TargetType.USER)
	public Response updateUserByName(@PathParam("userName") String userName, User user,
			@Context HttpServletRequest request) {
		try {
			String token = request.getHeader("token");
			if (token == null || token.isEmpty()) {
				return Response.status(Status.NOT_FOUND)
						.entity(new ResourceResponseBean("update user failed",
								"token is null or empty, please check the token in request header.",
								ResponseCodeConstant.EMPTY_TOKEN))
						.tag(userName).build();
			}

			String loginUser = TokenPaserUtils.paserUserName(token);
			logger.debug("updateUserByName -> update user with login user: " + loginUser);

			UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser,
					Constant.ROOTTENANTID);

			if (!(userName.equals(loginUser))) {
				if (role == null || !(role.getRoleName().equals(Constant.SYSADMIN))) {
					return Response.status(Status.UNAUTHORIZED)
							.entity(new ResourceResponseBean("update user failed",
									"the user is not system admin role, does NOT have the update user permission.",
									ResponseCodeConstant.NO_UPDATE_USER_PERMISSION))
							.tag(userName).build();
				}
			}

			user.setUsername(userName);
			user = UserPersistenceWrapper.updateUserByName(user);
			return Response.ok().entity(user).tag(userName).build();

		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("updateUserByName hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(userName).build();
		}
	}

	@PUT
	@Path("{userName}/password")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.UPDATE, targetType = TargetType.USER)
	public Response updateUserPassword(@PathParam("userName") String userName, PasswordBean password,
			@Context HttpServletRequest request) {
		try {

			String token = request.getHeader("token");
			if (token == null || token.isEmpty()) {
				return Response.status(Status.NOT_FOUND)
						.entity(new ResourceResponseBean("update user password failed",
								"token is null or empty, please check the token in request header.",
								ResponseCodeConstant.EMPTY_TOKEN))
						.tag(userName).build();
			}

			String loginUser = TokenPaserUtils.paserUserName(token);
			logger.debug("updateUserPassword -> update user password with login user: " + loginUser);

			UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser,
					Constant.ROOTTENANTID);

			if (!(userName.equals(loginUser))) {
				if (role == null || !(role.getRoleName().equals(Constant.SYSADMIN))) {
					return Response.status(Status.UNAUTHORIZED).entity(new ResourceResponseBean(
							"update user password failed",
							"the user is not system admin role or it is NOT change itself password, does NOT have the update user password permission.",
							ResponseCodeConstant.NO_UPDATE_USER_PASSWORD_PERMISSION)).tag(userName).build();
				}
			}

			UserPersistenceWrapper.updateUserPasswordByName(userName, password.getPassword());
			Authenticator.logout(userName);
			return Response.ok().entity(
					new ResourceResponseBean("update user password success", userName, ResponseCodeConstant.SUCCESS))
					.tag(userName).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("updateUserPassword hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(userName).build();
		}
	}

	/**
	 * Delete a user
	 *
	 * @param userId
	 *            user id
	 */
	@DELETE
	@Path("{id}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.DELETE, targetType = TargetType.USER)
	public Response deleteUser(@PathParam("id") String userId, @Context HttpServletRequest request) {
		String userName = null;
		try {

			String token = request.getHeader("token");
			if (token == null || token.isEmpty()) {
				return Response.status(Status.NOT_FOUND)
						.entity(new ResourceResponseBean("delete user failed",
								"token is null or empty, please check the token in request header.",
								ResponseCodeConstant.EMPTY_TOKEN))
						.tag(userName).build();
			}

			String loginUser = TokenPaserUtils.paserUserName(token);
			logger.debug("deleteUser -> delete user with login user: " + loginUser);

			UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser,
					Constant.ROOTTENANTID);

			if (role == null || !(role.getRoleName().equals(Constant.SYSADMIN))) {
				return Response.status(Status.UNAUTHORIZED)
						.entity(new ResourceResponseBean("delete user failed",
								"the user is not system admin role, does NOT have the delete user permission.",
								ResponseCodeConstant.NO_DELETE_USER_PERMISSION))
						.tag(userName).build();
			}

			User user = UserPersistenceWrapper.getUserById(userId);

			if (user == null) {
				return Response
						.status(Status.NOT_FOUND).entity(new ResourceResponseBean("delete user failed",
								"The user " + userId + "can not find.", ResponseCodeConstant.USER_NOT_FOUND))
						.tag(userName).build();
			}

			userName = user.getUsername();

			UserPersistenceWrapper.deleteUser(userId);
			Authenticator.logout(userName);

			return Response.ok()
					.entity(new ResourceResponseBean("delete user success", userId, ResponseCodeConstant.SUCCESS))
					.tag(userName).build();

		} catch (Exception e) {
			if (e.getCause() instanceof MySQLIntegrityConstraintViolationException) {
				List<UserRoleView> urvs = UserRoleViewPersistenceWrapper.getTenantAndRoleBasedOnUserName(userName);

				StringBuilder tenants = new StringBuilder();
				for (UserRoleView t : urvs) {
					tenants.append(t.getTenantName()).append(",");
				}
				return Response.status(Status.UNAUTHORIZED)
						.entity(new ResourceResponseBean("delete user failed",
								"The user is assign with the tenants: [" + tenants.toString()
										+ "], please unassign the user, then try to delete it again.",
								ResponseCodeConstant.USER_CAN_NOT_DELETE))
						.tag(userName).build();
			} else {
				// system out the exception into the console log
				logger.error("deleteUser  hit exception -> ", e);
				return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(userName).build();
			}
		}
	}

	@GET
	@Path("is/admin/{userName}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	public Response isAdminByName(@PathParam("userName") String userName) {
		try {

			UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(userName,
					Constant.ROOTTENANTID);

			if (role != null && role.getRoleName().equals(Constant.SYSADMIN)) {
				return Response.ok().entity(new IsAdminBean(true, userName)).build();
			} else {
				return Response.ok().entity(new IsAdminBean(false, userName)).build();
			}
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("isAdminByName hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).build();
		}
	}

	// /**
	// *
	// * @param userId
	// * @return
	// */
	// @GET
	// @Path("id/{id}/tenants")
	// @Produces((MediaType.APPLICATION_JSON + ";charset=utf-8"))
	// public Response getTenantAndRoleById(@PathParam("id") String userId) {
	// try {
	// List<UserRoleView> turs =
	// UserRoleViewPersistenceWrapper.getTenantAndRoleBasedOnUserId(userId);
	// return Response.ok().entity(turs).build();
	// } catch (Exception e) {
	// // system out the exception into the console log
	// logger.info("getTenantAndRoleById -> " + e.getMessage());
	// return Response.status(Status.BAD_REQUEST).entity(e.toString()).build();
	// }
	// }
	//
	// @GET
	// @Path("name/{name}/tenants")
	// @Produces((MediaType.APPLICATION_JSON + ";charset=utf-8"))
	// public Response getTenantAndRoleByName(@PathParam("name") String
	// userName) {
	// try {
	// List<UserRoleView> turs =
	// UserRoleViewPersistenceWrapper.getTenantAndRoleBasedOnUserName(userName);
	// return Response.ok().entity(turs).build();
	// } catch (Exception e) {
	// // system out the exception into the console log
	// logger.info("getTenantAndRoleByName -> " + e.getMessage());
	// return Response.status(Status.BAD_REQUEST).entity(e.toString()).build();
	// }
	// }

	/**
	 *
	 * @param userId
	 * @return
	 */
	@GET
	@Path("id/{id}/all/tenants")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.TENANTS)
	public Response getTenantsById(@PathParam("id") String userId) {
		try {
			List<UserRoleView> turs = UserRoleViewPersistenceWrapper.getTenantAndRoleBasedOnUserId(userId);

			List<Tenant> tenantList = new ArrayList<Tenant>();
			for (UserRoleView tur : turs) {
				TenantTree tree = TenantTreeUtil.constructTree(tur.getTenantId());
				List<TenantTreeNode> nodes = tree.listAllNodes();
				tenantList.addAll(TenantTreeUtil.transform(nodes));
			}

			return Response.ok().entity(TenantUtils.removeListDup(tenantList)).tag(userId).build();
		} catch (Exception e) {
			logger.error("Error while getting Tenants by user id: " + userId, e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(userId).build();
		}
	}

	/**
	 *
	 * @param userName
	 * @return
	 */
	@GET
	@Path("name/{name}/all/tenants")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.TENANTS)
	public Response getTenantsByName(@PathParam("name") String userName) {
		try {
			List<Tenant> tenantList = new ArrayList<Tenant>();
			List<UserRoleView> turs = UserRoleViewPersistenceWrapper.getTenantAndRoleBasedOnUserName(userName);
			for (UserRoleView tur : turs) {
				TenantTree tree = TenantTreeUtil.constructTree(tur.getTenantId());
				List<TenantTreeNode> allNodes = tree.listAllNodes();
				tenantList.addAll(TenantTreeUtil.transform(allNodes));
			}
			logger.info("getTenantsByName -> " + userName);
			return Response.ok().entity(TenantUtils.removeListDup(tenantList)).tag(userName).build();
		} catch (Exception e) {
			logger.error("Error while getting Tenants by user name: " + userName, e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(userName).build();
		}
	}

	@GET
	@Path("id/{id}/tenant/{tenantId}/children/tenants")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.TENANTS)
	public Response getChildrenTenantsByUserIdTenantId(@PathParam("id") String userId,
			@PathParam("tenantId") String tenantId) {
		try {
			List<UserRoleView> turs = UserRoleViewPersistenceWrapper.getTenantAndRoleBasedOnUserId(userId);

			List<Tenant> tenantList = UserResource.getTenantsList(turs);

			ArrayList<String> tenantIdList = new ArrayList<String>();
			ArrayList<Tenant> tenants = new ArrayList<Tenant>();
			for (Tenant ten : tenantList) {
				if (!tenantIdList.contains(ten.getId())) {
					tenantIdList.add(ten.getId());
					tenants.add(ten);
				}
			}

			ArrayList<Tenant> children = new ArrayList<Tenant>();
			for (Tenant t : tenants) {
				if (t.getParentId() != null) {
					if (t.getParentId().equals(tenantId)) {
						children.add(t);
					}
				}
			}

			return Response.ok().entity(children).tag(String.join("->", tenantId, userId)).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getChildrenTenantsByUserIdTenantId hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", tenantId, userId))
					.build();
		}
	}

	@GET
	@Path("name/{name}/tenant/{tenantId}/children/tenants")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.TENANTS)
	public Response getChildrenTenantsByUserNameTenantId(@PathParam("name") String userName,
			@PathParam("tenantId") String tenantId) {
		try {
			List<UserRoleView> turs = UserRoleViewPersistenceWrapper.getTenantAndRoleBasedOnUserName(userName);

			List<Tenant> tenantList = UserResource.getTenantsList(turs);

			ArrayList<String> tenantIdList = new ArrayList<String>();
			ArrayList<Tenant> tenants = new ArrayList<Tenant>();
			for (Tenant ten : tenantList) {
				if (!tenantIdList.contains(ten.getId())) {
					tenantIdList.add(ten.getId());
					tenants.add(ten);
				}
			}

			ArrayList<Tenant> children = new ArrayList<Tenant>();
			for (Tenant t : tenants) {
				if (t.getParentId() != null) {
					if (t.getParentId().equals(tenantId)) {
						children.add(t);
					}
				}
			}

			return Response.ok().entity(children).tag(String.join("->", tenantId, userName)).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getChildrenTenantsByUserNameTenantId  hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", tenantId, userName))
					.build();
		}
	}

	private static String getParentTenantName(String tenantId) {
		Tenant ct = TenantPersistenceWrapper.getTenantById(tenantId);
		if (ct != null) {
			Tenant pt = TenantPersistenceWrapper.getTenantById(ct.getParentId());
			return pt == null ? null : pt.getName();
		}
		return null;
	}

	private static List<Tenant> getTenantsList(List<UserRoleView> turs) {
		List<Tenant> tenantList = new ArrayList<Tenant>();
		for (UserRoleView tur : turs) {
			TenantTree tree = TenantTreeUtil.constructTree(tur.getTenantId());
			List<TenantTreeNode> allNodes = tree.listAllNodes();
			tenantList.addAll(TenantTreeUtil.transform(allNodes));
		}
		return tenantList;
	}

	@GET
	@Path("name/{userName}/tenant/{tenantId}/assignments/info")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.ASSIGNMENT)
	public Response getAssignmentsInfoForUser(@PathParam("userName") String userName,
			@PathParam("tenantId") String tenantId) {
		try {
			List<AssignmentInfoBean> assignmentInfoBeans = new ArrayList<AssignmentInfoBean>();
			List<ServiceInstance> instaces = ServiceInstancePersistenceWrapper.getServiceInstancesInTenant(tenantId);

			for (ServiceInstance instace : instaces) {
				String instanceStr = TenantUtils.getTenantServiceInstancesFromDf(tenantId, instace.getInstanceName());
				JsonElement instanceJE = new JsonParser().parse(instanceStr);
				JsonObject instanceJson = instanceJE.getAsJsonObject();
				JsonObject spec = instanceJson.getAsJsonObject("spec");
				String phase = instanceJson.getAsJsonObject("status").get("phase").getAsString();
//				JsonElement binding = spec.get("binding");
				String serviceName = spec.getAsJsonObject("provisioning").get("backingservice_name").getAsString();
				JsonElement action = instanceJson.getAsJsonObject("status").get("action");
				JsonElement patch = instanceJson.getAsJsonObject("status").get("patch");
				String actionString = action == null ? null :action.getAsString();
				String patchString = patch == null ? null :patch.getAsString();
				
//				if (!phase.equals(Constant.FAILURE)) {
//					if (Constant.list.contains(serviceName.toLowerCase())) {
//						if (!binding.isJsonNull()) {
//							JsonArray bindingArray = spec.getAsJsonArray("binding");
//							List<String> bindingedUserNames = new ArrayList<String>();
//							for (JsonElement je : bindingArray) {
//								String bindingUserName = je.getAsJsonObject().get("bind_hadoop_user").getAsString();
//								bindingedUserNames.add(bindingUserName);
//							}
//							if (bindingedUserNames.contains(userName)) {
//								AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//										"Authorization Success", phase, actionString, patchString);
//								assignmentInfoBeans.add(AIB);
//							} else {
//								if (action == null && patch == null) {
//									AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//											"Authorization Failure OR Not Begin", phase, actionString, patchString);
//									assignmentInfoBeans.add(AIB);
//								} else {
//									if (patch != null) {
//										if (patch.getAsString().equals(Constant.FAILURE)) {
//											AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//													"Failure OR Not Begin", phase, actionString, patchString);
//											assignmentInfoBeans.add(AIB);
//										} else {
//											AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//													"Authorization Running", phase, actionString, patchString);
//											assignmentInfoBeans.add(AIB);
//										}
//									} else {
//										if (action.getAsString().equals(Constant._TOBIND)
//												|| action.getAsString().equals(Constant._TOUNBIND)) {
//											AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//													"Authorization Running", phase, actionString, patchString);
//											assignmentInfoBeans.add(AIB);
//										} else {
//											AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//													"Failure OR Not Begin", phase, actionString, patchString);
//											assignmentInfoBeans.add(AIB);
//										}
//									}
//								}
//							}
//						} else {
//							if (action == null && patch == null) {
//								AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//										"Failure OR Not Begin", phase, actionString, patchString);
//								assignmentInfoBeans.add(AIB);
//							} else {
//								if (patch != null) {
//									if (patch.getAsString().equals(Constant.FAILURE)) {
//										AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//												"Failure OR Not Begin", phase, actionString, patchString);
//										assignmentInfoBeans.add(AIB);
//									} else {
//										AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//												"Authorization Running", phase, actionString, patchString);
//										assignmentInfoBeans.add(AIB);
//									}
//								} else {
//									if (action.getAsString().equals(Constant._TOBIND)
//											|| action.getAsString().equals(Constant._TOUNBIND)) {
//										AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//												"Authorization Running", phase, actionString, patchString);
//										assignmentInfoBeans.add(AIB);
//									} else {
//										AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//												"Failure OR Not Begin", phase, actionString, patchString);
//										assignmentInfoBeans.add(AIB);
//									}
//								}
//							}
//						}
//					} else {
//						AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
//								"Authorization Not Supported for service: " + instace.getServiceName(), phase, actionString, patchString);
//						assignmentInfoBeans.add(AIB);
//					}
//				}
				
				if (!Constant.list.contains(serviceName.toLowerCase())) {
					AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
							"Unsupportive For Privilege Assignment", phase, actionString, patchString);
					assignmentInfoBeans.add(AIB);
					continue;
				}
				
				switch (phase) {
				case Constant.UNBOUND:
					if (patch == null && action != null && action.getAsString().equals(Constant._TOBIND)) {
						AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
								"Privilege Assignment Running", phase, actionString, patchString);
						assignmentInfoBeans.add(AIB);	
					}else if (patch == null && action != null && action.getAsString().equals(Constant._TOUNBIND)) {
						AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
								"Privilege Not Assigned", phase, actionString, patchString);
						assignmentInfoBeans.add(AIB);
					}else if (patch == null && action == null) {
						AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
								"Privilege Not Assigned", phase, actionString, patchString);
						assignmentInfoBeans.add(AIB);
					} else {
						AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(), "Unknown BSI Status",
								phase, actionString, patchString);
						assignmentInfoBeans.add(AIB);
					} 
					break;
				case Constant.BOUND:
					if (inList(spec, userName)) {
						AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(), "Privilege Assigned",
								phase, actionString, patchString);
						assignmentInfoBeans.add(AIB);
					} else if (patch == null && action != null && action.getAsString().equals(Constant._TOUNBIND)) {
						AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
								"Privilege Unassignment Running", phase, actionString, patchString);
						assignmentInfoBeans.add(AIB);
					} else {
						AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(), "Unknown BSI Status",
								phase, actionString, patchString);
						assignmentInfoBeans.add(AIB);
					}
					break;
				case Constant.FAILURE:
					if (action == null && patch == null) {
						AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(),
								"BSI Provision Failed", phase, actionString, patchString);
						assignmentInfoBeans.add(AIB);
					} else {
						AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(), "Unknown BSI Status",
								phase, actionString, patchString);
						assignmentInfoBeans.add(AIB);
					}
					break;
				default:
					logger.error("Unkonwn <phase> status: " + phase);
					AssignmentInfoBean AIB = new AssignmentInfoBean(instace.getInstanceName(), "Unknown BSI Status",
							phase, actionString, patchString);
					assignmentInfoBeans.add(AIB);
					break;
				}
			}
			logger.info("User: " + userName + ", BSI infos: " + assignmentInfoBeans);
			return Response.ok().entity(assignmentInfoBeans).tag(String.join("->", userName, tenantId)).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getAssignmentsInfoForUser hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", userName, tenantId))
					.build();
		}
	}

	private boolean inList(JsonObject spec, String userName) {
		List<String> bindingedUserNames = new ArrayList<String>();
		JsonArray bindingArray = spec.getAsJsonArray("binding");
		if (bindingArray != null) {
			for (JsonElement je : bindingArray) {
				String bindingUserName = je.getAsJsonObject().get("bind_hadoop_user").getAsString();
				bindingedUserNames.add(bindingUserName);
			}	
		}
		return bindingedUserNames.contains(userName);
	}

}
