package com.asiainfo.ocmanager.rest.resource.executor;

import org.apache.log4j.Logger;

import com.asiainfo.ocmanager.persistence.model.ServiceRolePermission;
import com.asiainfo.ocmanager.persistence.model.TenantUserRoleAssignment;
import com.asiainfo.ocmanager.rest.bean.AdapterResponseBean;
import com.asiainfo.ocmanager.rest.constant.Constant;
import com.asiainfo.ocmanager.rest.resource.TenantResource;
import com.asiainfo.ocmanager.rest.resource.utils.ServiceRolePermissionWrapper;
import com.asiainfo.ocmanager.rest.resource.utils.UserPersistenceWrapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.asiainfo.ocmanager.dacp.DacpAllResult;

/**
 *
 * @author zhaoyim
 *
 */
public class TenantResourceAssignRoleExecutor implements Runnable {

	private static Logger logger = Logger.getLogger(TenantResource.class);

	private String tenantId;
	private int instnaceNum;
	private JsonArray allServiceInstancesArray;
	private TenantUserRoleAssignment assignment;

	public TenantResourceAssignRoleExecutor(String tenantId, JsonArray allServiceInstancesArray,
			TenantUserRoleAssignment assignment, int instnaceNum) {
		this.tenantId = tenantId;
		this.allServiceInstancesArray = allServiceInstancesArray;
		this.assignment = assignment;
		this.instnaceNum = instnaceNum;
	}

	public int getInstnaceNum() {
		return instnaceNum;
	}

	public void setInstnaceNum(int instnaceNum) {
		this.instnaceNum = instnaceNum;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public JsonArray getAllServiceInstancesArray() {
		return allServiceInstancesArray;
	}

	public void setAllServiceInstancesArray(JsonArray allServiceInstancesArray) {
		this.allServiceInstancesArray = allServiceInstancesArray;
	}

	public TenantUserRoleAssignment getAssignment() {
		return assignment;
	}

	public void setAssignment(TenantUserRoleAssignment assignment) {
		this.assignment = assignment;
	}

	@Override
	public void run() {
		try {
			for (int i = 0; i < allServiceInstancesArray.size(); i++) {
				// TODO should consider the resource version changed need to
				// call get instance by id
				JsonObject instance = allServiceInstancesArray.get(i).getAsJsonObject();
				// get service name
				String serviceName = instance.getAsJsonObject("spec").getAsJsonObject("provisioning")
						.get("backingservice_name").getAsString();

				String phase = instance.getAsJsonObject("status").get("phase").getAsString();
				if (!phase.equals(Constant.PROVISIONING) && !phase.equals(Constant.FAILURE)) {
					// Because the Provisioning will make the update failed
					if (Constant.list.contains(serviceName.toLowerCase())) {
						// get service instance name
						String instanceName = instance.getAsJsonObject("metadata").get("name").getAsString();
						String OCDPServiceInstanceStr = TenantResource.getTenantServiceInstancesFromDf(tenantId,
								instanceName);

						// get the service permission based on the service name
						// and role
						ServiceRolePermission permission = ServiceRolePermissionWrapper
								.getServicePermissionByRoleId(serviceName, assignment.getRoleId());

						// only the has service permission users
						// can be assign
						if (permission != null) {
							// parse the update request body
							JsonElement OCDPServiceInstanceJson = new JsonParser().parse(OCDPServiceInstanceStr);
							// get the provisioning json
							JsonObject provisioning = OCDPServiceInstanceJson.getAsJsonObject().getAsJsonObject("spec")
									.getAsJsonObject("provisioning");
							// add the user name to the parameters for update
							String userName = UserPersistenceWrapper.getUserById(assignment.getUserId()).getUsername();
							provisioning.getAsJsonObject("parameters").addProperty("user_name", userName);

							// add the accesses fields into the request body
							provisioning.getAsJsonObject("parameters").addProperty("accesses",
									permission.getServicePermission());

							// add the patch Updating into the request body
							JsonObject status = OCDPServiceInstanceJson.getAsJsonObject().getAsJsonObject("status");
							status.addProperty("patch", Constant.UPDATE);

							logger.info("assignRoleToUserInTenant -> begin to update");
							AdapterResponseBean updateRes = TenantResource.updateTenantServiceInstanceInDf(tenantId,
									instanceName, OCDPServiceInstanceJson.toString());

							if (updateRes.getResCodel() == 200) {
								logger.info(instanceName + "assignRoleToUserInTenant -> wait update complete");
								TenantResource.watiInstanceUpdateComplete(updateRes, tenantId, instanceName);
								logger.info(instanceName + "assignRoleToUserInTenant -> update complete");

								logger.info(instanceName + "assignRoleToUserInTenant -> begin to binding");
								AdapterResponseBean bindingRes = TenantResource.generateOCDPServiceCredentials(tenantId,
										instanceName, userName);
								if (bindingRes.getResCodel() == 201) {
									logger.info(instanceName + "assignRoleToUserInTenant -> binding successfully");
									TenantResource.watiInstanceBindingComplete(bindingRes, tenantId, instanceName);
								}
							}
						}
					}
				}
			}
			DacpAllResult.getAllResult(tenantId);
		} catch (Exception e) {
			// system out the exception into the console log
			logger.info("assignRoleToUserInTenant -> " + e.getMessage());

		}
	}

}
