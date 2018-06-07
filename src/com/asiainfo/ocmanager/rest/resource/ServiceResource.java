package com.asiainfo.ocmanager.rest.resource;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.asiainfo.ocmanager.audit.Audit;
import com.asiainfo.ocmanager.audit.Audit.Action;
import com.asiainfo.ocmanager.audit.Audit.TargetType;
import com.asiainfo.ocmanager.auth.utils.TokenPaserUtils;
import com.asiainfo.ocmanager.persistence.model.Service;
import com.asiainfo.ocmanager.persistence.model.ServiceInstance;
import com.asiainfo.ocmanager.persistence.model.UserRoleView;
import com.asiainfo.ocmanager.rest.bean.ResourceResponseBean;
import com.asiainfo.ocmanager.rest.constant.Constant;
import com.asiainfo.ocmanager.rest.constant.ResponseCodeConstant;
import com.asiainfo.ocmanager.rest.resource.persistence.ServiceInstancePersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.ServicePersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.UserRoleViewPersistenceWrapper;
import com.asiainfo.ocmanager.rest.utils.DataFoundryConfiguration;
import com.asiainfo.ocmanager.rest.utils.SSLSocketIgnoreCA;
import com.asiainfo.ocmanager.utils.Catalog;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 *
 * @author zhaoyim
 *
 */
@Path("/service")
public class ServiceResource {

	private static Logger logger = Logger.getLogger(ServiceResource.class);

	/**
	 * Get All OCManager services
	 *
	 * @return service list
	 */
	@GET
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.SERVICES)
	public Response getServices() {

		try {
			// TODO should call df service api and compare with adapter db
			// service data, insert the data which is not in the adapter db
			// every time when call the get all services api it will symnc the
			// adapter db with df services data
			// this is not a good solution should be enhance in future
			List<Service> servicesInDB = ServicePersistenceWrapper.getAllServices();

			// get all the services in the adapter db
			List<String> dbServiceNameList = new ArrayList<String>();
			for (Service s : servicesInDB) {
				dbServiceNameList.add(s.getServicename().toLowerCase());
			}

			String servicesFromDf = ServiceResource.callDFToGetAllServices();
			JsonObject servicesFromDfJson = new JsonParser().parse(servicesFromDf).getAsJsonObject();
			JsonArray items = servicesFromDfJson.getAsJsonArray("items");

			if (items != null) {
				for (int i = 0; i < items.size(); i++) {
					String name = items.get(i).getAsJsonObject().getAsJsonObject("spec").get("name").getAsString();
					String id = items.get(i).getAsJsonObject().getAsJsonObject("spec").get("id").getAsString();
					String description = items.get(i).getAsJsonObject().getAsJsonObject("spec").get("description")
							.getAsString();
					String origin = items.get(i).getAsJsonObject().getAsJsonObject("metadata").getAsJsonObject("labels")
							.get("asiainfo.io/servicebroker").getAsString();

					if (servicesInDB.size() == 0) {
						ServicePersistenceWrapper.addService(new Service(id, name, description, origin,
								Catalog.getInstance().getServiceType(name).toLowerCase()));
					} else {
						if (!dbServiceNameList.contains(name.toLowerCase())) {
							ServicePersistenceWrapper.addService(new Service(id, name, description, origin,
									Catalog.getInstance().getServiceType(name).toLowerCase()));
						}
					}

				}
			}

			List<Service> services = ServicePersistenceWrapper.getAllServices();

			return Response.ok().entity(services).tag("all-services").build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getServices  hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag("all-services").build();
		}
	}

	/**
	 * Get service by id
	 *
	 * @return service
	 */
	@GET
	@Path("{id}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.SERVICE)
	public Response getServiceById(@PathParam("id") String serviceId) {
		try {
			Service service = ServicePersistenceWrapper.getServiceById(serviceId);

			return Response.ok().entity(service == null ? new Service() : service).tag(serviceId).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getServiceById hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(serviceId).build();
		}
	}

	/**
	 * Add service broker
	 *
	 * @return service
	 */
	@POST
	@Path("/broker")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.CREATE, targetType = TargetType.BROKER)
	public Response addServiceBroker(String reqBodyStr, @Context HttpServletRequest request) {
		String brokerIP = extractIP(reqBodyStr);
		try {

			String adToken = request.getHeader("token");
			if (adToken == null || adToken.isEmpty()) {
				return Response.status(Status.NOT_FOUND)
						.entity(new ResourceResponseBean("add service broker failed",
								"token is null or empty, please check the token in request header.",
								ResponseCodeConstant.EMPTY_TOKEN))
						.tag(brokerIP).build();
			}

			String loginUser = TokenPaserUtils.paserUserName(adToken);
			logger.debug("addServiceBroker -> add service broker with login user: " + loginUser);

			UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser,
					Constant.ROOTTENANTID);

			if (role == null || !(role.getRoleName().equals(Constant.SYSADMIN))) {
				return Response.status(Status.UNAUTHORIZED)
						.entity(new ResourceResponseBean("add service broker failed",
								"the user is not system admin role, does NOT have the add service broker permission.",
								ResponseCodeConstant.NO_ADD_SERVICE_BROKER_PERMISSION))
						.tag(brokerIP).build();
			}

			String url = DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_URL);
			String token = DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_TOKEN);
			String dfRestUrl = url + "/oapi/v1/servicebrokers";

			// parse the req body make sure it is json
			JsonElement reqBodyJson = new JsonParser().parse(reqBodyStr);
			SSLConnectionSocketFactory sslsf = SSLSocketIgnoreCA.createSSLSocketFactory();

			CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
			try {
				HttpPost httpPost = new HttpPost(dfRestUrl);
				httpPost.addHeader("Content-type", "application/json");
				httpPost.addHeader("Authorization", "bearer " + token);
				StringEntity se = new StringEntity(reqBodyJson.toString());
				se.setContentType("application/json");
				se.setContentEncoding("utf-8");
				httpPost.setEntity(se);

				CloseableHttpResponse response2 = httpclient.execute(httpPost);

				try {
					// int statusCode =
					// response2.getStatusLine().getStatusCode();
					String bodyStr = EntityUtils.toString(response2.getEntity());

					return Response.ok().entity(bodyStr).tag(brokerIP).build();
				} finally {
					response2.close();
				}
			} finally {
				httpclient.close();
			}
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("addServiceBroker hit exception-> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(brokerIP).build();
		}
	}

	private String extractIP(String reqBodyStr) {
		JsonElement reqBodyJson = new JsonParser().parse(reqBodyStr);
		JsonObject spec = reqBodyJson.getAsJsonObject().getAsJsonObject("spec");
		JsonObject url = spec.getAsJsonObject("url");
		return url.getAsString();
	}

	/**
	 * Get all service from df
	 *
	 * @return services
	 */
	@GET
	@Path("/df")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.SERVICES)
	public Response getServiceFromDf() {
		try {
			return Response.ok().entity(ServiceResource.callDFToGetAllServices()).tag("all-services").build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getServiceFromDf hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag("all-services").build();
		}
	}

	/**
	 * delete service broker
	 *
	 * @return service
	 */
	@DELETE
	@Path("/broker/{name}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.DELETE, targetType = TargetType.BROKER)
	public Response deleteServiceBroker(@PathParam("name") String serviceBrokerName,
			@Context HttpServletRequest request) {
		try {

			String adToken = request.getHeader("token");
			if (adToken == null || adToken.isEmpty()) {
				return Response.status(Status.NOT_FOUND)
						.entity(new ResourceResponseBean("delete service broker failed",
								"token is null or empty, please check the token in request header.",
								ResponseCodeConstant.EMPTY_TOKEN))
						.tag(serviceBrokerName).build();
			}

			String loginUser = TokenPaserUtils.paserUserName(adToken);
			logger.debug("deleteServiceBroker -> delete service broker with login user: " + loginUser);

			UserRoleView role = UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser,
					Constant.ROOTTENANTID);

			if (role == null || !(role.getRoleName().equals(Constant.SYSADMIN))) {
				return Response.status(Status.UNAUTHORIZED)
						.entity(new ResourceResponseBean("delete service broker failed",
								"the user is not system admin role, does NOT have the add service broker permission.",
								ResponseCodeConstant.NO_DELETE_SERVICE_BROKER_PERMISSION))
						.tag(serviceBrokerName).build();
			}

			String url = DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_URL);
			String token = DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_TOKEN);
			String dfRestUrl = url + "/oapi/v1/servicebrokers/" + serviceBrokerName;

			SSLConnectionSocketFactory sslsf = SSLSocketIgnoreCA.createSSLSocketFactory();

			CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
			try {
				HttpDelete httpDelete = new HttpDelete(dfRestUrl);
				httpDelete.addHeader("Content-type", "application/json");
				httpDelete.addHeader("Authorization", "bearer " + token);

				CloseableHttpResponse response1 = httpclient.execute(httpDelete);

				try {
					// int statusCode =
					// response1.getStatusLine().getStatusCode();

					String bodyStr = EntityUtils.toString(response1.getEntity());

					return Response.ok().entity(bodyStr).tag(serviceBrokerName).build();
				} finally {
					response1.close();
				}
			} finally {
				httpclient.close();
			}
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("deleteServiceBroker hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(serviceBrokerName).build();
		}
	}

	/**
	 * call data foundry rest api
	 *
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String callDFToGetAllServices() throws KeyManagementException, NoSuchAlgorithmException,
			KeyStoreException, ClientProtocolException, IOException {

		String url = DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_URL);
		String token = DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_TOKEN);
		String dfRestUrl = url + "/oapi/v1/namespaces/openshift/backingservices";

		SSLConnectionSocketFactory sslsf = SSLSocketIgnoreCA.createSSLSocketFactory();

		CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
		try {
			HttpGet httpGet = new HttpGet(dfRestUrl);
			httpGet.addHeader("Content-type", "application/json;charset=utf-8");
			httpGet.addHeader("Authorization", "bearer " + token);

			CloseableHttpResponse response1 = httpclient.execute(httpGet);

			try {
				// int statusCode =
				// response1.getStatusLine().getStatusCode();

				String bodyStr = EntityUtils.toString(response1.getEntity(), "UTF-8");

				return bodyStr;
			} finally {
				response1.close();
			}
		} finally {
			httpclient.close();
		}
	}

	/**
	 * Get all service instances
	 *
	 * @return service instance list
	 */
	@GET
	@Path("all/instances")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.INSTANCES)
	public Response getAllServiceInstances() {
		try {
			List<ServiceInstance> serviceInstances = ServiceInstancePersistenceWrapper.getAllServiceInstances();
			return Response.ok().entity(serviceInstances).tag("all-instances").build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getAllServiceInstances hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag("all-instances").build();
		}
	}

	/**
	 * 
	 * @param serviceName
	 * @return
	 */
	@GET
	@Path("{serviceName}/plan")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.SERVICE_PLAN)
	public Response getServicePlanInfo(@PathParam("serviceName") String serviceName) {
		try {
			String servicesStr = ServiceResource.callDFToGetAllServices();
			JsonObject servicesJson = new JsonParser().parse(servicesStr).getAsJsonObject();
			JsonArray items = servicesJson.getAsJsonArray("items");
			if (items != null) {
				for (int i = 0; i < items.size(); i++) {
					JsonObject spec = items.get(i).getAsJsonObject().getAsJsonObject("spec");
					String name = spec.get("name").getAsString();
					String plan = spec.getAsJsonArray("plans").toString();
					if (serviceName.toLowerCase().equals(name.toLowerCase())) {
						return Response.ok().entity(plan).tag(serviceName).build();
					}
				}
			}

			return Response.status(Status.NOT_FOUND)
					.entity(new ResourceResponseBean("get service plan failed",
							"can NOT find the service plan, please make sure you input the correct service name"
									+ " or the service is added successfully in the OCManager.",
							ResponseCodeConstant.SERVICE_PLAN_NOT_FOUND))
					.tag(serviceName).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getServicePlanInfo hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(serviceName).build();
		}
	}

}
