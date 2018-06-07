package com.asiainfo.ocmanager.rest.resource;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asiainfo.ocmanager.audit.Audit;
import com.asiainfo.ocmanager.audit.Audit.Action;
import com.asiainfo.ocmanager.audit.Audit.TargetType;
import com.asiainfo.ocmanager.auth.utils.TokenPaserUtils;
import com.asiainfo.ocmanager.concurrent.TenantLockerPool;
import com.asiainfo.ocmanager.persistence.model.ServiceInstance;
import com.asiainfo.ocmanager.persistence.model.Tenant;
import com.asiainfo.ocmanager.persistence.model.TenantUserRoleAssignment;
import com.asiainfo.ocmanager.persistence.model.UserRoleView;
import com.asiainfo.ocmanager.rest.bean.ResourceResponseBean;
import com.asiainfo.ocmanager.rest.bean.TenantBean;
import com.asiainfo.ocmanager.rest.bean.TenantQuotaResponse;
import com.asiainfo.ocmanager.rest.bean.TenantQuotaResponse.ServiceInstanceQuotaResponse;
import com.asiainfo.ocmanager.rest.bean.service.instance.ServiceInstanceQuotaConst;
import com.asiainfo.ocmanager.rest.constant.Constant;
import com.asiainfo.ocmanager.rest.constant.ResponseCodeConstant;
import com.asiainfo.ocmanager.rest.resource.executor.TenantResourceAssignRoleExecutor;
import com.asiainfo.ocmanager.rest.resource.executor.TenantResourceUnAssignRoleExecutor;
import com.asiainfo.ocmanager.rest.resource.executor.TenantResourceUpdateRoleExecutor;
import com.asiainfo.ocmanager.rest.resource.persistence.ServiceInstancePersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.TURAssignmentPersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.TenantPersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.UserRoleViewPersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.utils.ServiceInstanceUtils;
import com.asiainfo.ocmanager.rest.resource.utils.TenantJsonParserUtils;
import com.asiainfo.ocmanager.rest.resource.utils.TenantUtils;
import com.asiainfo.ocmanager.rest.resource.utils.model.ServiceInstanceQuotaCheckerResponse;
import com.asiainfo.ocmanager.rest.resource.utils.model.ServiceInstanceResponse;
import com.asiainfo.ocmanager.rest.resource.utils.model.TenantResponse;
import com.asiainfo.ocmanager.rest.utils.DataFoundryConfiguration;
import com.asiainfo.ocmanager.rest.utils.PeekerUtils;
import com.asiainfo.ocmanager.rest.utils.SSLSocketIgnoreCA;
import com.asiainfo.ocmanager.service.broker.ResourcePeeker;
import com.asiainfo.ocmanager.service.broker.utils.ResourcePeekerFactory;
import com.asiainfo.ocmanager.utils.Catalog;
import com.asiainfo.ocmanager.utils.TenantTree.TenantTreeNode;
import com.asiainfo.ocmanager.utils.TenantTreeUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 *
 * @author zhaoyim
 *
 */

@Path("/tenant")
public class TenantResource {

	private static Logger logger = LoggerFactory.getLogger(TenantResource.class);

	/**
	 * Get All OCManager tenants
	 *
	 * @return tenant list
	 */
	@GET
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.TENANTS)
	public Response getAllTenants() {
		try {
			List<Tenant> tenants = TenantPersistenceWrapper.getAllTenants();
			return Response.ok().entity(tenants).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getAllTenants hit exception-> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).build();
		}
	}

	/**
	 * Get the specific tenant by id
	 *
	 * @param tenantId
	 *            tenant id
	 * @return tenant
	 */
	@GET
	@Path("{id}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.TENANT)
	public Response getTenantById(@PathParam("id") String tenantId) {
		try {
			Tenant tenant = TenantPersistenceWrapper.getTenantById(tenantId);
			return Response.ok().entity(tenant).tag(tenantId).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getTenantById hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(tenantId).build();
		}
	}

	/**
	 * Get the child tenants
	 *
	 * @param tenantId
	 *            tenant id
	 * @return tenant list
	 */
	@GET
	@Path("{id}/children")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.TENANTS)
	public Response getChildrenTenants(@PathParam("id") String parentTenantId) {
		try {
			List<Tenant> tenants = TenantPersistenceWrapper.getChildrenTenants(parentTenantId);
			return Response.ok().entity(tenants).tag(parentTenantId).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getChildrenTenants hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(parentTenantId).build();
		}
	}

	/**
	 * Get the users list in the specific tenant
	 *
	 * @param tenantId
	 *            tenant id
	 * @return user list
	 */
	@GET
	@Path("{id}/users")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.USERS)
	public Response getTenantUsers(@PathParam("id") String tenantId) {
		try {
			List<UserRoleView> usersRoles = UserRoleViewPersistenceWrapper.getUsersInTenant(tenantId);
			return Response.ok().entity(usersRoles).tag(tenantId).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getTenantUsers hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(tenantId).build();
		}
	}

	/**
	 * Get user role on the tenant, based on recursive route upper to root.
	 * 
	 * @param tenantId
	 * @param userName
	 * @return
	 */
	private UserRoleView getRecursiveRole(String tenantId, String userName) {

		UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(userName, tenantId);
		if (role == null) {
			logger.debug("getRole -> start get tenant");
			Tenant tenant = TenantPersistenceWrapper.getTenantById(tenantId);
			logger.debug("getRole -> finish get tenant");
			if (tenant == null) {
				return null;
			}
			if (tenant.getParentId() == null) {
				return null;
			} else {
				role = getRecursiveRole(tenant.getParentId(), userName);
			}
		}
		return role;
	}

	/**
	 * Get the role based on the tenant and user
	 *
	 * @param tenantId
	 * @param userName
	 * @return
	 */
	@GET
	@Path("{id}/user/{userName}/role")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.ROLE)
	public Response getRoleByTenantUserName(@PathParam("id") String tenantId, @PathParam("userName") String userName) {
		try {
			UserRoleView role = getRecursiveRole(tenantId, userName);
			if (role != null) {
				// set the tenant id to the passed tenant id
				role.setTenantId(tenantId);
			}
			return Response.ok().entity(role).tag(String.join("->", tenantId, userName)).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getRoleByTenantUserName hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", tenantId, userName))
					.build();
		}
	}

	/**
	 * Get the service instance list in the specific tenant
	 *
	 * @param tenantId
	 *            tenant id
	 * @return service instance list
	 */
	@GET
	@Path("{id}/service/instances")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.INSTANCES)
	public Response getTenantServiceInstances(@PathParam("id") String tenantId) {
		try {
			List<ServiceInstance> serviceInstances = ServiceInstancePersistenceWrapper
					.getServiceInstancesInTenant(tenantId);
			return Response.ok().entity(serviceInstances).tag(tenantId).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getTenantServiceInstances hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(tenantId).build();
		}
	}

	/**
	 * get the tenant service instance info which include the access info
	 *
	 * @param tenantId
	 * @param InstanceName
	 * @return
	 */
	@GET
	@Path("{tenantId}/service/instance/{InstanceName}/access/info")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.ACCESSINFO)
	public Response getTenantServiceInstanceAccessInfo(@PathParam("tenantId") String tenantId,
			@PathParam("InstanceName") String InstanceName) {
		try {
			return Response.ok().entity(TenantUtils.getTenantServiceInstancesFromDf(tenantId, InstanceName))
					.tag(InstanceName).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getTenantServiceInstanceAccessInfo hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(InstanceName).build();
		}

	}

	@PUT
	@Path("{tenantId}/status")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.UPDATE, targetType = TargetType.TENANT_LIFETIME)
	public Response renewTenantLease(Tenant tenant, @Context HttpServletRequest request) {
		try {
			String loginUser = TokenPaserUtils.paserUserName(getToken(request));
			if (!isSysadmin(loginUser)) {
				logger.error("Only system-admin allowed to renew tenant lease. Current user " + loginUser
						+ " has no privilege.");
				return Response.status(Status.UNAUTHORIZED)
						.entity(new ResourceResponseBean("operation failed",
								"Current user has no privilege to do the operations.",
								ResponseCodeConstant.NO_PERMISSION_ON_TENANT))
						.tag(tenant.getName()).build();
			}
			synchronized (TenantLockerPool.getInstance().getLocker(tenant.getId())) {
				TenantPersistenceWrapper.updateStatus(tenant);
				return Response.ok().entity("Renew tenant lease successful!").tag(tenant.getName()).build();
			}
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("createTenant hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(tenant.getName()).build();
		}
	}

	/**
	 * Create a new tenant
	 *
	 * @param tenant
	 *            tenant obj json
	 * @return new tenant info
	 */
	@POST
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.CREATE, targetType = TargetType.TENANT)
	public Response createTenant(Tenant tenant, @Context HttpServletRequest request) {
		try {
			if (tenant.getName() == null || tenant.getId() == null) {
				return Response.status(Status.BAD_REQUEST).entity("input format is not correct").tag(tenant.getName())
						.build();
			}

			if (!TenantJsonParserUtils.isValidJsonString(tenant.getQuota())) {
				return Response.status(Status.BAD_REQUEST)
						.entity(new ResourceResponseBean("operation failed",
								"tenant quota is invalid json format, please correct.",
								ResponseCodeConstant.BAD_REQUEST))
						.tag(tenant.getName()).build();
			}

			if (tenant.getParentId() == null || tenant.getParentId().isEmpty()) {
				return Response.status(Status.NOT_ACCEPTABLE)
						.entity("Parent tenant must be specified and parentID must not be null.").tag(tenant.getName())
						.build();
			}

			// if the tenant have the instances
			// can NOT create chlid tenant
			if (tenant.getParentId() != null && hasInstances(tenant.getParentId())) {
				return Response.status(Status.NOT_ACCEPTABLE)
						.entity("The parent tenant have service instances, can NOT create child tenant.")
						.tag(tenant.getName()).build();
			}

			String loginUser = TokenPaserUtils.paserUserName(getToken(request));
			if (!isSysadmin(loginUser)) {
				if (!isAdminOnRecursive(loginUser, tenant.getId())) {
					logger.error("User not privileged to update tenants. Current user " + loginUser
							+ " has no permission to update tenant " + tenant.getId());
					return Response.status(Status.UNAUTHORIZED)
							.entity(new ResourceResponseBean("operation failed",
									"Current user has no privilege to do the operations.",
									ResponseCodeConstant.NO_PERMISSION_ON_TENANT))
							.tag(tenant.getName()).build();
				}
			}

			// lock parent tenant
			synchronized (TenantLockerPool.getInstance().getLocker(tenant.getParentId())) {
				TenantResponse tenantRes = TenantUtils.createTenant(tenant);
				if (!tenantRes.getCheckerRes().isCanChange()) {
					logger.error("Failed to create tenant due to exceeded parent tenant quota: "
							+ tenantRes.getCheckerRes().getMessages());
					return Response.status(Status.NOT_ACCEPTABLE)
							.entity(new ResourceResponseBean("operation failed",
									tenantRes.getCheckerRes().getMessages(),
									ResponseCodeConstant.EXCEED_PARENT_TENANT_QUOTA))
							.tag(tenant.getName()).build();
				}
				TenantLockerPool.getInstance().register(tenant);
				return Response.ok().entity(tenantRes.getTenantBean()).tag(tenant.getName()).build();
			}
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("createTenant hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(tenant.getName()).build();
		}
	}

	/**
	 * Whether the role has privileges on the specified tenant
	 *
	 * @param role
	 * @return true if does, or false either user not tenant-admin or tenant is
	 *         deactive
	 */
	private boolean privilegedOnTenant(UserRoleView role, String tenantID) {
		if (role == null || role.getRoleName().isEmpty()) {
			return false;
		} else if (tenantID == null || tenantID.isEmpty()) {
			return false;
		} else {
			return role.getRoleName().equals(Constant.TENANTADMIN) && isActive(tenantID);
		}
	}

	private boolean isActive(String tenantID) {
		Tenant t = TenantPersistenceWrapper.getTenantById(tenantID);
		return (t.getStatus() != null && t.getStatus().equalsIgnoreCase("active"));
	}

	private String getToken(HttpServletRequest request) {
		String token = request.getHeader("token");
		if (token == null || token.isEmpty()) {
			logger.error("Token is null in request: " + request);
			throw new RuntimeException("Token is null in request");
		}
		return token;
	}

	private boolean isSysadmin(String user) {
		UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(user, Constant.ROOTTENANTID);
		if (role == null || role.getRoleName().isEmpty()) {
			return false;
		}
		return role.getRoleName().equals(Constant.SYSADMIN);
	}

	/**
	 * Create a service instance in specific tenant
	 * 
	 * @param
	 * @return
	 */
	@POST
	@Path("{id}/service/instance")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.CREATE, targetType = TargetType.INSTANCE)
	public Response createServiceInstanceInTenant(@PathParam("id") String tenantId, String reqBodyStr,
			@Context HttpServletRequest request) {
		String cuzbisNameString = null;
		try {
			JsonElement reqBodyJson = new JsonParser().parse(reqBodyStr);

			// when custom the bsi name shoud check
			// wethere it existing
			String metadataName = reqBodyJson.getAsJsonObject().getAsJsonObject("metadata").get("name").getAsString();
			String backingServiceName = reqBodyJson.getAsJsonObject().getAsJsonObject("spec")
					.getAsJsonObject("provisioning").get("backingservice_name").getAsString();
			JsonObject parameters = reqBodyJson.getAsJsonObject().getAsJsonObject("spec")
					.getAsJsonObject("provisioning").getAsJsonObject("parameters");
			JsonElement cuzBsiNameJE = parameters.get("cuzBsiName");
			cuzbisNameString = cuzBsiNameJE.getAsString();

			String loginUser = TokenPaserUtils.paserUserName(getToken(request));
			if (!isSysadmin(loginUser)) {
				UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser, tenantId);
				if (!privilegedOnTenant(role, tenantId)) {
					logger.error("Current user " + loginUser + " has no privilege on tenant " + tenantId
							+ ". User may not be tenant.admin of current tenant, or the tenant is out of lifecycle");
					return Response.status(Status.UNAUTHORIZED)
							.entity(new ResourceResponseBean("operation failed",
									"Current user has no privilege to do the operations.",
									ResponseCodeConstant.NO_PERMISSION_ON_TENANT))
							.tag(cuzbisNameString).build();
				}
			}

			if (metadataName.indexOf("-") != -1) {
				logger.error("The service instance name can NOT include dash (-), " + "please try antoher name.");
				return Response.status(Status.BAD_REQUEST)
						.entity(new ResourceResponseBean("operation failed",
								"The service instance name can NOT include dash (-), " + "please try antoher name.",
								ResponseCodeConstant.BAD_REQUEST))
						.tag(cuzbisNameString).build();
			}

			// check exist custom bsiName
			if (cuzBsiNameJE != null && Constant.list.contains(backingServiceName.toLowerCase())) {
				String cuzBsiName = cuzBsiNameJE.getAsString();
				ServiceInstance serInst = ServiceInstancePersistenceWrapper.getServiceInstanceByCuzBsiName(cuzBsiName,
						backingServiceName);
				if (serInst != null) {
					logger.error("Resource name [{}] of service [{}] already exist in OCDP cluster. Try another name.",
							cuzBsiName, backingServiceName);
					return Response.status(Status.CONFLICT)
							.entity(new ResourceResponseBean("operation failed",
									"Resource name already exist in OCDP cluster.", ResponseCodeConstant.CONFLICT))
							.tag(cuzbisNameString).build();
				}
			}

			ServiceInstanceResponse serviceInstRes = new ServiceInstanceResponse();
			synchronized (TenantLockerPool.getInstance().getLocker(tenantId)) {
				if (ServiceInstanceQuotaConst.quotaCheckServices
						.contains(Catalog.getInstance().getServiceType(backingServiceName).toLowerCase())) {

					ServiceInstanceQuotaCheckerResponse checkRes = ServiceInstanceUtils.canCreateBsi(backingServiceName,
							tenantId, parameters);
					serviceInstRes.setCheckerRes(checkRes);

					if (!serviceInstRes.getCheckerRes().isCanChange()) {
						logger.error("Failed to create bsi due to exceeded tenant quota: "
								+ serviceInstRes.getCheckerRes().getMessages());
						return Response.status(Status.NOT_ACCEPTABLE)
								.entity(new ResourceResponseBean("operation failed",
										serviceInstRes.getCheckerRes().getMessages(),
										ResponseCodeConstant.EXCEED_PARENT_TENANT_QUOTA))
								.tag(cuzbisNameString).build();
					}
				}
				String resBody = ServiceInstanceUtils.createBsi(tenantId, reqBodyJson);
				serviceInstRes.setResBody(resBody);
			}

			return Response.ok().entity(serviceInstRes.getResBody()).tag(cuzbisNameString).build();

		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("createServiceInstanceInTenant hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(cuzbisNameString).build();
		}

	}

	/**
	 * Update a service instance in specific tenant
	 *
	 * @param tenantId
	 * @param instanceName
	 * @param reqBodyStr
	 * @return
	 */
	@PUT
	@Path("{id}/service/instance/{instanceName}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.UPDATE, targetType = TargetType.INSTANCE)
	public Response updateServiceInstanceInTenant(@PathParam("id") String tenantId,
			@PathParam("instanceName") String instanceName, String parametersStr, @Context HttpServletRequest request) {

		try {
			String loginUser = TokenPaserUtils.paserUserName(getToken(request));
			if (!isSysadmin(loginUser)) {
				UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser, tenantId);
				if (!privilegedOnTenant(role, tenantId)) {
					logger.error("Current user " + loginUser + " has no privilege on tenant " + tenantId
							+ ". User may not be tenant.admin of current tenant, or the tenant is out of lifecycle");
					return Response.status(Status.UNAUTHORIZED)
							.entity(new ResourceResponseBean("operation failed",
									"Current user has no privilege to do the operations.",
									ResponseCodeConstant.NO_PERMISSION_ON_TENANT))
							.tag(instanceName).build();
				}
			}

			// get the just now created instance info
			String getInstanceResBody = TenantUtils.getTenantServiceInstancesFromDf(tenantId, instanceName);

			JsonElement serviceInstanceJson = new JsonParser().parse(getInstanceResBody);
			// get status phase
			String phase = serviceInstanceJson.getAsJsonObject().getAsJsonObject("status").get("phase").getAsString();

			if (phase.equals(Constant.PROVISIONING)) {
				logger.info(
						"updateServiceInstanceInTenant -> The instance can not be updated when it is Provisioning!");
				return Response.status(Status.BAD_REQUEST)
						.entity("The instance can not be updated when it is Provisioning!").tag(instanceName).build();
			}

			if (phase.equals(Constant.FAILURE)) {
				logger.info("updateServiceInstanceInTenant -> The instance can not be updated when it is Failure!");
				return Response.status(Status.BAD_REQUEST).entity("The instance can not be updated when it is Failure!")
						.tag(instanceName).build();
			}

			// get the provisioning json
			JsonObject provisioning = serviceInstanceJson.getAsJsonObject().getAsJsonObject("spec")
					.getAsJsonObject("provisioning");

			// parse the input parameters json
			JsonElement parameterJon = new JsonParser().parse(parametersStr);
			JsonObject parameterObj = parameterJon.getAsJsonObject().getAsJsonObject("parameters");
			//check REQUEST_NUMBER and REQUEST_SIZE
			String requestNumStr = null;
			String requestSizeStr = null;
			if (parameterObj.has("REQUEST_NUMBER")) {
				requestNumStr = parameterObj.get("REQUEST_NUMBER").getAsString();
			}
			if (parameterObj.has("REQUEST_SIZE")) {
				requestSizeStr = parameterObj.get("REQUEST_SIZE").getAsString();
			}
			if (requestNumStr != null) {
				parameterObj.remove("REQUEST_NUMBER");
			}
			if (requestSizeStr !=null) {
				parameterObj.remove("REQUEST_SIZE");
			}

			synchronized (TenantLockerPool.getInstance().getLocker(tenantId)) {
				// check whether parameters format is legal
				try {
					JsonObject incrementalParameters = getIncremental(tenantId, instanceName, parameterObj);
					// Pair<String, ServiceType> bsi = getInstanceIDandType(tenantId, instanceName);
					// validateUpdateParameter(tenantId, bsi, toMap(parameterObj.entrySet()));
					ServiceInstanceResponse serviceInstRes = new ServiceInstanceResponse();
					ServiceInstanceQuotaCheckerResponse checkRes = ServiceInstanceUtils.canCreateBsi(
							provisioning.get("backingservice_name").getAsString(), tenantId, incrementalParameters);
					serviceInstRes.setCheckerRes(checkRes);

					if (!serviceInstRes.getCheckerRes().isCanChange()) {
						logger.error("Failed to create bsi due to exceeded tenant quota: "
								+ serviceInstRes.getCheckerRes().getMessages());
						return Response.status(Status.NOT_ACCEPTABLE)
								.entity(new ResourceResponseBean("operation failed",
										serviceInstRes.getCheckerRes().getMessages(),
										ResponseCodeConstant.EXCEED_PARENT_TENANT_QUOTA))
								.tag(instanceName).build();
					}
				} catch (Exception e) {
					logger.error("Failed to update bsi due to exceeded tenant quota: " + e.getMessage());
					return Response
							.status(Status.NOT_ACCEPTABLE).entity(new ResourceResponseBean("operation failed",
									e.getMessage(), ResponseCodeConstant.EXCEED_PARENT_TENANT_QUOTA))
							.tag(instanceName).build();
				}

				//add REQUEST_NUMBER and REQUEST_SIZE to parameter
				if (requestNumStr != null) {
					parameterObj.addProperty("REQUEST_NUMBER", requestNumStr);
				}
				if (requestSizeStr !=null) {
					parameterObj.addProperty("REQUEST_SIZE", requestSizeStr);
				}
				// add into the update json
				provisioning.add("parameters", parameterObj);

				// add the patch Updating into the request body
				JsonObject status = serviceInstanceJson.getAsJsonObject().getAsJsonObject("status");
				status.addProperty("patch", Constant.UPDATE);

				logger.info("updateServiceInstanceInTenant -> update start");
				ResourceResponseBean responseBean = TenantUtils.updateTenantServiceInstanceInDf(tenantId, instanceName,
						serviceInstanceJson.toString());

				if (responseBean.getResCodel() == 200) {
					logger.info("updateServiceInstanceInTenant -> update successfully");
					JsonElement resBodyJson = new JsonParser().parse(responseBean.getMessage());
					JsonObject resBodyJsonObj = resBodyJson.getAsJsonObject();
					if ((resBodyJsonObj.getAsJsonObject("spec").getAsJsonObject("provisioning").get("parameters")
							.isJsonNull())) {
						logger.error(
								"Abnormal response from DF, parameters returned by DF is null! UpdateRquest: instanceName "
										+ instanceName + ", parameters " + parametersStr);
						throw new RuntimeException("parameters returned by DF is null!");
					}

					this.updateOCMDatabase(parametersStr, tenantId, instanceName);
				}

				return Response.ok().entity(responseBean.getMessage()).tag(instanceName).build();
			}
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("updateServiceInstanceInTenant hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(instanceName).build();
		}
	}

	/**
	 * Get incremental value of parameters by the result of requested parameter
	 * mimus current parameter value. Additionally, request parameters will be
	 * filtered in some circumstances(eg. Kafka topicQuota parameter might be
	 * removed coz of kafka restriction).
	 * 
	 * @param instanceName
	 * @param instanceName2
	 * @param parameterObj
	 * @return
	 */
	private JsonObject getIncremental(String tenantID, String instanceName, JsonObject parameterObj) {
		JsonObject incrementalObj = new JsonObject();
		Iterator<Entry<String, JsonElement>> iterator = parameterObj.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, JsonElement> entry = iterator.next();
			if (isKafkaTopicQuota(entry)) {
				long quotaInDB = getKafkaTopicQuotaInDB(tenantID, instanceName);
				long newQuota = entry.getValue().getAsLong();
				if (newQuota < quotaInDB) {
					logger.error("Kafka topicQuota parameter [" + newQuota
							+ "] must NOT be smaller than current value: " + quotaInDB);
					throw new RuntimeException("Kafka topicQuota must not be smaller than current value: " + quotaInDB);
				} else if (newQuota == quotaInDB) {
					logger.warn("Removing topicQuota parameter, coz it's equivalent to current value: " + quotaInDB);
					iterator.remove();
					continue;
				}
			}
			double current = getCurrentQuota(tenantID, instanceName, entry);
			double incremental = Double.parseDouble(entry.getValue().getAsString()) - current;
			incrementalObj.addProperty(entry.getKey(), incremental);
		}
		logger.info("Updating bsi {} by incremental quotas: {}", instanceName, incrementalObj);
		return incrementalObj;
	}

	private double getCurrentQuota(String tenantID, String instanceName, Entry<String, JsonElement> entry) {
		String json = ServiceInstancePersistenceWrapper.getServiceInstance(tenantID, instanceName).getQuota();
		JsonObject jobj = new JsonParser().parse(json).getAsJsonObject();
		return Double.parseDouble(jobj.get(entry.getKey()).getAsString());
	}

	private long getKafkaTopicQuotaInDB(String tenantID, String instanceName) {
		String json = ServiceInstancePersistenceWrapper.getServiceInstance(tenantID, instanceName).getQuota();
		JsonObject jobj = new JsonParser().parse(json).getAsJsonObject();
		long topicSize = jobj.get("topicQuota").getAsLong();
		return topicSize;
	}

	private void updateOCMDatabase(String parametersStr, String tenantId, String instanceName) {

		JsonElement parameterJon = new JsonParser().parse(parametersStr);
		JsonObject parameterObj = parameterJon.getAsJsonObject().getAsJsonObject("parameters");

		Set<Entry<String, JsonElement>> entrySet = parameterObj.getAsJsonObject().entrySet();

		JsonObject attributes = new JsonObject();
		JsonObject quota = new JsonObject();

		for (Entry<String, JsonElement> type : entrySet) {

			if (type.getKey().startsWith(Constant.ATTRIBUTES)) {
				attributes.add(type.getKey(), type.getValue());
			} else {
				quota.add(type.getKey(), type.getValue());
			}
		}

		ServiceInstancePersistenceWrapper.updateServiceInstanceQuota(tenantId, instanceName, quota.toString());

		ServiceInstancePersistenceWrapper.updateServiceInstanceAttributes(tenantId, instanceName,
				attributes.toString());
	}

	private boolean isKafkaTopicQuota(Entry<String, JsonElement> entry) {
		return "topicQuota".equals(entry.getKey().trim());
	}

	/**
	 * Delete a service instance in specific tenant
	 *
	 * @param tenantId
	 * @param instanceName
	 * @return
	 */
	@DELETE
	@Path("{id}/service/instance/{instanceName}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.DELETE, targetType = TargetType.INSTANCE)
	public Response deleteServiceInstanceInTenant(@PathParam("id") String tenantId,
			@PathParam("instanceName") String instanceName, @Context HttpServletRequest request) {

		try {
			String loginUser = TokenPaserUtils.paserUserName(getToken(request));
			if (!isSysadmin(loginUser)) {
				UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser, tenantId);
				if (!privilegedOnTenant(role, tenantId)) {
					logger.error("Current user " + loginUser + " has no privilege on tenant " + tenantId
							+ ". User may not be tenant.admin of current tenant, or the tenant is out of lifecycle");
					return Response.status(Status.UNAUTHORIZED)
							.entity(new ResourceResponseBean("operation failed",
									"Current user has no privilege to do the operations.",
									ResponseCodeConstant.NO_PERMISSION_ON_TENANT))
							.tag(instanceName).build();
				}
			}

			synchronized (TenantLockerPool.getInstance().getLocker(tenantId)) {
				String getInstanceResBody = TenantUtils.getTenantServiceInstancesFromDf(tenantId, instanceName);
				JsonElement resBodyJson = new JsonParser().parse(getInstanceResBody);
				JsonObject instance = resBodyJson.getAsJsonObject();
				String serviceName = instance.getAsJsonObject("spec").getAsJsonObject("provisioning")
						.get("backingservice_name").getAsString();
				// get status phase
				String phase = instance.getAsJsonObject("status").get("phase").getAsString();

				if (phase.equals(Constant.PROVISIONING)) {
					logger.info("Instance [{}] can not be deleted when it is Provisioning!", instanceName);
					return Response.status(Status.BAD_REQUEST)
							.entity("Tnstance can not be deleted when it is Provisioning!").tag(instanceName).build();
				}

				// get binding info
				JsonObject spec = instance.getAsJsonObject("spec");
				JsonElement binding = spec.get("binding");

				// if the instance is Failure do not need to unbound
				if (!phase.equals(Constant.FAILURE)) {
					if (Constant.list.contains(serviceName.toLowerCase())) {
						if (!binding.isJsonNull()) {
							JsonArray bindingArray = spec.getAsJsonArray("binding");
							for (JsonElement je : bindingArray) {
								String userName = je.getAsJsonObject().get("bind_hadoop_user").getAsString();
								logger.info("Unbinding user [{}] to instance [{}] starting", userName, instanceName);
								ResourceResponseBean unBindingRes = TenantUtils.removeOCDPServiceCredentials(tenantId,
										instanceName, userName);
								if (unBindingRes.getResCodel() == 201) {
									TenantUtils.watiInstanceUnBindingComplete(unBindingRes, tenantId, instanceName);
									logger.info("Unbinding user [{}] to instance [{}] finished", userName,
											instanceName);
								}
							}
						}
					}
				}

				// begin to delete the instance
				String url = DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_URL);
				String token = DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_TOKEN);
				String dfRestUrl = url + "/oapi/v1/namespaces/" + tenantId + "/backingserviceinstances/" + instanceName;

				SSLConnectionSocketFactory sslsf = SSLSocketIgnoreCA.createSSLSocketFactory();

				CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
				try {
					HttpDelete httpDelete = new HttpDelete(dfRestUrl);
					httpDelete.addHeader("Content-type", "application/json");
					httpDelete.addHeader("Authorization", "bearer " + token);

					logger.info("Beginning to delete instance [{}]", instanceName);
					CloseableHttpResponse response1 = httpclient.execute(httpDelete);

					try {
						int statusCode = response1.getStatusLine().getStatusCode();
						if (statusCode == 200) {
							ServiceInstancePersistenceWrapper.deleteServiceInstance(tenantId, instanceName);
							logger.info("Successfully deleted instance [{}]", instanceName);
						}
						String bodyStr = EntityUtils.toString(response1.getEntity());

						return Response.ok().entity(bodyStr).tag(instanceName).build();
					} finally {
						response1.close();
					}
				} finally {
					httpclient.close();
				}
			}
		} catch (Exception e) {
			logger.error("Exception while deleting instance : " + instanceName, e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(instanceName).build();
		}

	}

	/**
	 * Update the existing tenant info only in OCM rest
	 *
	 * @param tenant
	 *            tenant obj json
	 * @return updated tenant info
	 */
	@PUT
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.UPDATE, targetType = TargetType.TENANT)
	public Response updateTenant(Tenant tenant, @Context HttpServletRequest request) {

		try {
			if (!TenantJsonParserUtils.isValidJsonString(tenant.getQuota())) {
				return Response.status(Status.BAD_REQUEST)
						.entity(new ResourceResponseBean("operation failed",
								"tenant quota is invalid json format, please correct.",
								ResponseCodeConstant.BAD_REQUEST))
						.tag(tenant.getName()).build();
			}

			logger.info("updateTenant -> start update");

			if (tenant.getId() == null) {
				return Response.status(Status.BAD_REQUEST).entity("tenant id is null").tag(tenant.getName()).build();
			}
			String loginUser = TokenPaserUtils.paserUserName(getToken(request));
			if (!isSysadmin(loginUser)) {
				if (!isAdminOnParents(loginUser, tenant.getId())) {
					logger.error("User not privileged to update tenants. Current user " + loginUser
							+ " has no permission to update tenant " + tenant.getId());
					return Response.status(Status.UNAUTHORIZED)
							.entity(new ResourceResponseBean("operation failed",
									"Current user has no privilege to do the operations.",
									ResponseCodeConstant.NO_PERMISSION_ON_TENANT))
							.tag(tenant.getName()).build();
				}
			}

			Tenant origin = TenantPersistenceWrapper.getTenantById(tenant.getId());
			if (origin == null) {
				return Response.status(Status.BAD_REQUEST)
						.entity(new ResourceResponseBean("operation failed",
								"the tenant is NOT existed in the OCManager, please input a correct one.",
								ResponseCodeConstant.BAD_REQUEST))
						.tag(tenant.getName()).build();
			}

			synchronized (TenantLockerPool.getInstance()
					.getLocker(origin.getParentId() != null ? origin.getParentId() : origin.getId())) {
				TenantResponse tenantRes = TenantUtils.updateTenant(tenant);
				if (!tenantRes.getCheckerRes().isCanChange()) {
					logger.error("Failed to update tenant due to exceeded parent tenant quota: "
							+ tenantRes.getCheckerRes().getMessages());
					return Response.status(Status.NOT_ACCEPTABLE)
							.entity(new ResourceResponseBean("operation failed",
									tenantRes.getCheckerRes().getMessages(),
									ResponseCodeConstant.EXCEED_PARENT_TENANT_QUOTA))
							.tag(tenant.getName()).build();
				}
			}

			return Response.ok().entity(new TenantBean(tenant, "no dataFoundryInfo")).tag(tenant.getName()).build();

		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("updateTenant hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(tenant.getName()).build();
		}
	}

	/**
	 * Is user admin on parent tenants(excluding specified tenant itself).
	 * 
	 * @param loginUser
	 * @param tenantId
	 * @return
	 */
	private boolean isAdminOnParents(String loginUser, String tenantId) {
		for (TenantTreeNode node : TenantTreeUtil.constructTree(tenantId).listOriginParents()) {
			UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser, node.getId());
			if (role != null && (role.getRoleName().equals(Constant.TENANTADMIN)
					|| role.getRoleName().equals(Constant.SYSADMIN))) {
				return true;
			}
		}
		logger.debug("User [{}] is neither system nor tenant admin on all Parents of tenant: " + tenantId);
		return false;
	}

	/**
	 * Is user admin on specified tenant or parent tenants.
	 * 
	 * @param loginUser
	 * @param tenantId
	 * @param include
	 *            whether to include the specified tenant itself
	 * @return
	 */
	private boolean isAdminOnRecursive(String loginUser, String tenantId) {
		UserRoleView role = getRecursiveRole(tenantId, loginUser);
		if (role != null
				&& (role.getRoleName().equals(Constant.TENANTADMIN) || role.getRoleName().equals(Constant.SYSADMIN))) {
			return true;
		}
		logger.debug("User [{}] has no system/tenant admin privilege on parents of tenant: {}", loginUser, tenantId);
		return false;
	}

	/**
	 * Delete a tenant
	 *
	 * @param tenantId
	 *            tenant id
	 */
	@DELETE
	@Path("{id}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.DELETE, targetType = TargetType.TENANT)
	public Response deleteTenant(@PathParam("id") String tenantId, @Context HttpServletRequest request) {
		try {
			String loginUser = TokenPaserUtils.paserUserName(getToken(request));
			if (!isSysadmin(loginUser)) {
				logger.error("Only sysadmin can do this operation. User: " + loginUser);
				return Response.status(Status.UNAUTHORIZED)
						.entity(new ResourceResponseBean("operation failed",
								"Current user has no privilege to do the operations.",
								ResponseCodeConstant.NOT_SYSTEM_ADMIN))
						.tag(tenantId).build();
			}

			if (!isLeafTenant(tenantId)) {
				return Response.status(Status.NOT_ACCEPTABLE).tag(tenantId)
						.entity("The tenant can not be deleted, because it have sub-tenants.").build();
			}

			// if have instances can not be deleted
			if (hasInstances(tenantId)) {
				return Response.status(Status.NOT_ACCEPTABLE).tag(tenantId)
						.entity("The tenant can not be deleted, because it have service instances on it.").build();
			}
			// if have users can not be deleted
			if (hasUsers(tenantId)) {
				return Response.status(Status.NOT_ACCEPTABLE).tag(tenantId)
						.entity("The tenant can not be deleted, because it have users binding with it.").build();
			}
			int statusCode = -1;
			try {
				synchronized (TenantLockerPool.getInstance().getLocker(tenantId)) {
					String url = DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_URL);
					String token = DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_TOKEN);
					String dfRestUrl = url + "/oapi/v1/projects/" + tenantId;

					SSLConnectionSocketFactory sslsf = SSLSocketIgnoreCA.createSSLSocketFactory();

					CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
					try {
						HttpDelete httpDelete = new HttpDelete(dfRestUrl);
						httpDelete.addHeader("Content-type", "application/json");
						httpDelete.addHeader("Authorization", "bearer " + token);

						logger.info("deleteTenant -> delete start");
						CloseableHttpResponse response1 = httpclient.execute(httpDelete);

						try {
							Tenant tenant = TenantPersistenceWrapper.getTenantById(tenantId);

							statusCode = response1.getStatusLine().getStatusCode();
							if (statusCode == 200) {
								TenantPersistenceWrapper.deleteTenant(tenantId);
								logger.info("deleteTenant -> delete successfully");
							}
							String bodyStr = EntityUtils.toString(response1.getEntity());

							return Response.ok().entity(new TenantBean(tenant, bodyStr)).tag(tenantId).build();
						} finally {
							response1.close();
						}
					} finally {
						httpclient.close();
					}
				}
			} finally {
				if (statusCode == 200) {
					TenantLockerPool.getInstance().unregister(getTenant(tenantId));
				}
			}

		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("deleteTenant hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(tenantId).build();
		}
	}

	private Tenant getTenant(String tenantId) {
		Tenant t = new Tenant();
		t.setId(tenantId);
		return t;
	}

	private boolean isLeafTenant(String tenantId) {
		List<Tenant> children = TenantPersistenceWrapper.getChildrenTenants(tenantId);
		if (children == null || children.isEmpty()) {
			return true;
		}
		return false;
	}

	private boolean hasInstances(String tenantId) {
		List<ServiceInstance> instances = ServiceInstancePersistenceWrapper.getServiceInstancesInTenant(tenantId);
		if (instances.size() > 0) {
			return true;
		}
		return false;
	}

	private boolean hasUsers(String tenantId) {
		List<UserRoleView> users = UserRoleViewPersistenceWrapper.getUsersInTenant(tenantId);
		// if the user list == 1 and it is admin
		// the tenant can be deleted
		if (users.size() == 1 && users.get(0).getUserName().equals(Constant.ADMIN)) {
			return false;
		} else {
			// if the size >= 1 the tenant have user
			if (users.size() >= 1) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Assign role to user in tenant
	 *
	 * @param tenantId
	 * @param assignment
	 * @return
	 */
	@POST
	@Path("{id}/user/role/assignment")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.ASSIGN, targetType = TargetType.ASSIGNMENT)
	public Response assignRoleToUserInTenant(@PathParam("id") String tenantId, TenantUserRoleAssignment assignment,
			@Context HttpServletRequest request) {
		// assgin to the input tenant
		assignment.setTenantId(tenantId);

		try {
			String loginUser = TokenPaserUtils.paserUserName(getToken(request));
			if (!isSysadmin(loginUser)) {
				UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser, tenantId);
				if (!privilegedOnTenant(role, tenantId)) {
					logger.error("Current user " + loginUser + " has no privilege on tenant " + tenantId
							+ ". User may not be tenant.admin of current tenant, or the tenant is out of lifecycle");
					return Response.status(Status.UNAUTHORIZED)
							.entity(new ResourceResponseBean("operation failed",
									"Current user has no privilege to do the operations.",
									ResponseCodeConstant.NO_PERMISSION_ON_TENANT))
							.tag(assignment.toString()).build();
				}
			}

			// get all service instances from df
			String allServiceInstances = TenantUtils.getTenantAllServiceInstancesFromDf(tenantId);
			JsonElement allServiceInstancesJson = new JsonParser().parse(allServiceInstances);

			JsonArray allServiceInstancesArray = allServiceInstancesJson.getAsJsonObject().getAsJsonArray("items");
			for (int i = 0; i < allServiceInstancesArray.size(); i++) {
				TenantResourceAssignRoleExecutor runnable = new TenantResourceAssignRoleExecutor(tenantId,
						allServiceInstancesArray, assignment, i);
				Thread thread = new Thread(runnable);
				thread.start();

			}

			assignment = TURAssignmentPersistenceWrapper.assignRoleToUserInTenant(assignment);

			return Response.ok().entity(assignment).tag(assignment.toString()).build();

		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("assignRoleToUserInTenant hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(assignment.toString()).build();
		}

	}

	/**
	 * Update user role in tenant
	 *
	 * @param tenantId
	 * @param assignment
	 * @return
	 */
	@PUT
	@Path("{id}/user/role/assignment")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.UPDATE, targetType = TargetType.ASSIGNMENT)
	public Response updateRoleToUserInTenant(@PathParam("id") String tenantId, TenantUserRoleAssignment assignment,
			@Context HttpServletRequest request) {

		try {
			// assgin to the input tenant
			assignment.setTenantId(tenantId);

			String loginUser = TokenPaserUtils.paserUserName(getToken(request));
			if (!isSysadmin(loginUser)) {
				UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser, tenantId);
				if (!privilegedOnTenant(role, tenantId)) {
					logger.error("Current user " + loginUser + " has no privilege on tenant " + tenantId
							+ ". User may not be tenant.admin of current tenant, or the tenant is out of lifecycle");
					return Response.status(Status.UNAUTHORIZED)
							.entity(new ResourceResponseBean("operation failed",
									"Current user has no privilege to do the operations.",
									ResponseCodeConstant.NO_PERMISSION_ON_TENANT))
							.tag(assignment.toString()).build();
				}
			}

			// get all service instances from df
			String allServiceInstances = TenantUtils.getTenantAllServiceInstancesFromDf(tenantId);
			JsonElement allServiceInstancesJson = new JsonParser().parse(allServiceInstances);

			JsonArray allServiceInstancesArray = allServiceInstancesJson.getAsJsonObject().getAsJsonArray("items");
			for (int i = 0; i < allServiceInstancesArray.size(); i++) {
				TenantResourceUpdateRoleExecutor runnable = new TenantResourceUpdateRoleExecutor(tenantId,
						allServiceInstancesArray, assignment, i);
				Thread thread = new Thread(runnable);
				thread.start();
			}

			assignment = TURAssignmentPersistenceWrapper.updateRoleToUserInTenant(assignment);

			return Response.ok().entity(assignment).tag(assignment.toString()).build();

		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("updateRoleToUserInTenant hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(assignment.toString()).build();
		}

	}

	/**
	 * Unassign role to user in tenant
	 *
	 * @param tenantId
	 * @param userId
	 * @return
	 */
	@DELETE
	@Path("{id}/user/{userId}/role/assignment")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.UNASSIGN, targetType = TargetType.ASSIGNMENT)
	public Response unassignRoleFromUserInTenant(@PathParam("id") String tenantId, @PathParam("userId") String userId,
			@Context HttpServletRequest request) {

		try {
			String loginUser = TokenPaserUtils.paserUserName(getToken(request));
			if (!isSysadmin(loginUser)) {
				UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser, tenantId);
				if (!privilegedOnTenant(role, tenantId)) {
					logger.error("Current user " + loginUser + " has no privilege on tenant " + tenantId
							+ ". User may not be tenant.admin of current tenant, or the tenant is out of lifecycle");
					return Response.status(Status.UNAUTHORIZED)
							.entity(new ResourceResponseBean("operation failed",
									"Current user has no privilege to do the operations.",
									ResponseCodeConstant.NO_PERMISSION_ON_TENANT))
							.tag(String.join("->", tenantId, userId)).build();
				}
			}

			// get all service instances from df
			String allServiceInstances = TenantUtils.getTenantAllServiceInstancesFromDf(tenantId);
			JsonElement allServiceInstancesJson = new JsonParser().parse(allServiceInstances);

			JsonArray allServiceInstancesArray = allServiceInstancesJson.getAsJsonObject().getAsJsonArray("items");
			for (int i = 0; i < allServiceInstancesArray.size(); i++) {
				JsonObject instance = allServiceInstancesArray.get(i).getAsJsonObject();
				TenantResourceUnAssignRoleExecutor runnable = new TenantResourceUnAssignRoleExecutor(tenantId, instance,
						userId);
				Thread thread = new Thread(runnable);
				thread.start();
			}

			TURAssignmentPersistenceWrapper.unassignRoleFromUserInTenant(tenantId, userId);

			return Response.ok().entity(new ResourceResponseBean("delete success", userId, 200))
					.tag(String.join("->", tenantId, userId)).build();

		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("unassignRoleFromUserInTenant hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", tenantId, userId))
					.build();
		}

	}

	/**
	 * Get quota usage of the specified tenant
	 * 
	 * @param tenantId
	 * @return
	 */
	@GET
	@Path("{tenantId}/quotas")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.QUOTAS)
	public Response getTenantQuotas(@PathParam("tenantId") String tenantId) {
		try {
			TenantQuotaResponse rsp = new TenantQuotaResponse(tenantId);
			List<ServiceInstance> instances = getInstances(tenantId);
			instances.forEach(e -> {
				try {
					ResourcePeeker peeker = ResourcePeekerFactory.getPeeker(e.getServiceName());
					ResourcePeeker quota = peeker.peekOn(Arrays.asList(e.getCuzBsiName()));
					ServiceInstanceQuotaResponse insQuota = toInstanceQuotaBean(e.getId(), quota);
					rsp.addInstance(insQuota);
				} catch (Exception e2) {
					logger.error("Exception while getting tenant quota: ", e2);
					throw new RuntimeException("Exception while getting tenant quota: ", e2);
				}
			});
			return Response.ok().entity(rsp).tag(tenantId).build();
		} catch (Exception e) {
			logger.error("getTenantQuotas hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(tenantId).build();
		}

	}

	private ServiceInstanceQuotaResponse toInstanceQuotaBean(String instanceId, ResourcePeeker quota) {
		ServiceInstanceQuotaResponse insQuota = new ServiceInstanceQuotaResponse(instanceId);
		insQuota.setItems(PeekerUtils.transformToBeans(quota));
		return insQuota;
	}

	private List<ServiceInstance> getInstances(String tenantId) {
		List<ServiceInstance> instances = ServiceInstancePersistenceWrapper.getServiceInstancesInTenant(tenantId);
		return instances;
	}

}
