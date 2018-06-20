package com.asiainfo.ocmanager.rest.resource.v2;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.ini4j.InvalidFileFormatException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asiainfo.ocmanager.audit.Audit;
import com.asiainfo.ocmanager.audit.Audit.Action;
import com.asiainfo.ocmanager.audit.Audit.TargetType;
import com.asiainfo.ocmanager.auth.utils.TokenPaserUtils;
import com.asiainfo.ocmanager.persistence.model.Broker;
import com.asiainfo.ocmanager.persistence.model.Broker.BrokerStatus;
import com.asiainfo.ocmanager.persistence.model.Cluster;
import com.asiainfo.ocmanager.persistence.model.UserRoleView;
import com.asiainfo.ocmanager.rest.bean.CustomEvnBean;
import com.asiainfo.ocmanager.rest.bean.ResourceResponseBean;
import com.asiainfo.ocmanager.rest.constant.Constant;
import com.asiainfo.ocmanager.rest.constant.ResponseCodeConstant;
import com.asiainfo.ocmanager.rest.resource.exception.bean.ResponseExceptionBean;
import com.asiainfo.ocmanager.rest.resource.persistence.BrokerPersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.ClusterPersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.persistence.UserRoleViewPersistenceWrapper;
import com.asiainfo.ocmanager.rest.resource.utils.model.DFRestResponse;
import com.asiainfo.ocmanager.rest.utils.SSLSocketIgnoreCA;
import com.asiainfo.ocmanager.service.broker.BrokerAdapterInterface;
import com.asiainfo.ocmanager.service.broker.utils.BrokerAdaptorUtils;
import com.asiainfo.ocmanager.service.client.DFRestClient;
import com.asiainfo.ocmanager.utils.DFTemplate;
import com.asiainfo.ocmanager.utils.EtcdJson;
import com.asiainfo.ocmanager.service.client.CmEtcdClient;
import com.asiainfo.ocmanager.utils.OsClusterIni;
import com.asiainfo.ocmanager.utils.ServicesIni;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 *
 * @author zhaoyim
 *
 */
@Path("/v2/api/brokers")
public class BrokerResource {

	private static Logger logger = LoggerFactory.getLogger(BrokerResource.class);

	@POST
	@Path("/dc")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.CREATE, targetType = TargetType.BROKER_DC)
	public Response createBrokerDC(String requestBody, @Context HttpServletRequest request) {
		String clustername = null;
		try {
			List<CustomEvnBean> cusEnvs = customEnvs(requestBody);
			clustername = request.getParameter("clustername");
			Preconditions.checkArgument(!Strings.isNullOrEmpty(clustername));
			Cluster cluster = ClusterPersistenceWrapper.getClusterByName(clustername);
			if (cluster == null) {
				logger.error("Cluster not found by name: " + clustername);
			}
			BrokerAdapterInterface adapter = BrokerAdaptorUtils.getAdapter(cluster, cusEnvs);
			String dcreq = DFTemplate.Create_DC.assembleString(adapter);
			DFRestResponse rsp = createdc(dcreq);
			if (success(rsp)) {
				String clusterName = adapter.getCluster().getCluster_name().toLowerCase();
				BrokerPersistenceWrapper.insert(
						new Broker(clusterName, adapter.getImage(), clusterName, clusterName, BrokerStatus.DC_CREATED));
				return Response.ok().entity(rsp.getEntity()).tag(clustername).build();
			}
			logger.error("Response failed: " + rsp);
			return Response.status(Status.BAD_REQUEST).entity(rsp).tag(clustername).build();
		} catch (Exception e) {
			logger.error("createBrokerDC hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(new ResponseExceptionBean(e.toString())).tag(clustername)
					.build();
		}
	}

	private boolean success(DFRestResponse rsp) {
		return rsp.getStatus() == 201;
	}

	private List<CustomEvnBean> customEnvs(String requestBody) {
		JsonObject json = new JsonParser().parse(requestBody).getAsJsonObject();
		JsonArray kvs = json.getAsJsonArray("env");
		if (kvs == null) {
			logger.debug("Create dc request does not has any customized environments: " + requestBody);
			return Lists.newArrayList();
		}
		List<CustomEvnBean> list = Lists.newArrayList();
		kvs.forEach(kv -> {
			JsonObject obj = kv.getAsJsonObject();
			CustomEvnBean bean = new CustomEvnBean(obj.getAsJsonPrimitive("key").getAsString(),
					obj.getAsJsonPrimitive("value").getAsString(), obj.getAsJsonPrimitive("description").getAsString());
			list.add(bean);
		});
		return list;
	}

	@POST
	@Path("/svc")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.CREATE, targetType = TargetType.BROKER_SVC)
	public Response createBrokerSVC(@Context HttpServletRequest request) {
		String dcName = request.getParameter("dcname");
		try {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(dcName));
			String svcreq = DFTemplate.Create_SVC.assembleString(dcName);
			DFRestResponse rsp = createsvc(svcreq);
			if (!success(rsp)) {
				logger.error("Response return code not 201: " + rsp);
				return Response.status(Status.BAD_REQUEST).entity(rsp).tag(dcName).build();
			}
			BrokerPersistenceWrapper.updateStatus(brokerName(dcName), BrokerStatus.SVC_CREATED.name());
			return Response.ok().entity(rsp.getEntity()).tag(dcName).build();
		} catch (Exception e) {
			logger.error("createBrokerSVC hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(new ResponseExceptionBean(e.toString())).tag(dcName)
					.build();
		}
	}

	@POST
	@Path("/router")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.CREATE, targetType = TargetType.BROKER_ROUTER)
	public Response createBrokerRouter(@Context HttpServletRequest request) {
		String svcname = request.getParameter("svcname");
		try {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(svcname));
			String svcreq = DFTemplate.Create_Router.assembleString(svcname);
			DFRestResponse rsp = createrouter(svcreq);
			if (!success(rsp)) {
				logger.error("Response return code not 201: " + rsp);
				return Response.status(Status.BAD_REQUEST).entity(rsp).tag(svcreq).build();
			}
			BrokerPersistenceWrapper.updateURL(brokerName(svcname), svcname + DFTemplate.Create_Router.HOST_POSTFIX);
			BrokerPersistenceWrapper.updateStatus(brokerName(svcname), BrokerStatus.ROUTER_CREATED.name());
			return Response.ok().entity(rsp.getEntity()).tag(svcname).build();
		} catch (Exception e) {
			logger.error("createBrokerRouter hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(new ResponseExceptionBean(e.toString())).tag(svcname)
					.build();
		}
	}

	private String brokerName(String svcname) {
		return svcname;
	}

	private DFRestResponse createsvc(String svcreq) {
		try {
			DFRestResponse rsp = new DFRestClient().sendPost("/api/v1/namespaces/dp-brokers/services", svcreq);
			return rsp;
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			logger.error("Exception while create svc: ", e);
			throw new RuntimeException("Exception while create svc: ", e);
		}
	}

	private DFRestResponse createdc(String dcreq) {
		try {
			DFRestResponse rsp = new DFRestClient().sendPost("/oapi/v1/namespaces/dp-brokers/deploymentconfigs", dcreq);
			return rsp;
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			logger.error("Exception while create dc: ", e);
			throw new RuntimeException("Exception while create dc: ", e);
		}
	}

	private DFRestResponse createrouter(String reqBody) {
		try {
			DFRestResponse rsp = new DFRestClient().sendPost("/oapi/v1/namespaces/dp-brokers/routes", reqBody);
			return rsp;
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			logger.error("Exception while create router: ", e);
			throw new RuntimeException("Exception while create router: ", e);
		}
	}

	/**
	 * get a broker dc
	 * 
	 * @param request
	 * @return
	 */
	@GET
	@Path("/{name}/dc")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.BROKER_DC)
	public Response getBrokerDC(@PathParam("name") String brokerName) {
		try {
			Broker broker = BrokerPersistenceWrapper.getBrokerByName(brokerName);
			if (broker == null) {
				logger.error("Broker not found by name: " + brokerName);
				return Response.status(Status.BAD_REQUEST).entity("Broker not found by name: " + brokerName)
						.tag(brokerName).build();
			}
			String dcName = broker.getDc_name();
			DFRestResponse rsp = new DFRestClient().sendGet(dcURI(dcName), null);
			return Response.ok().entity(rsp).build();
		} catch (Exception e) {
			logger.error("getBrokerDC hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(new ResponseExceptionBean(e.toString())).tag(brokerName)
					.build();
		}
	}

	private String dcURI(String dcName) {
		return "/oapi/v1/namespaces/dp-brokers/deploymentconfigs/" + dcName;
	}

	/**
	 * update a broker
	 * 
	 * @param request
	 * @return
	 */
	@PUT
	@Path("/{name}/dc")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.UPDATE, targetType = TargetType.BROKER_DC)
	public Response updateBrokerDC(@PathParam("name") String brokerName, String requestBody) {
		try {
			DFRestResponse rsp = new DFRestClient().sendPut(dcURI(brokerName), requestBody);
			return Response.ok().entity(rsp).tag(brokerName).build();
		} catch (Exception e) {
			logger.error("updateBrokerDC hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(new ResponseExceptionBean(e.toString())).tag(brokerName)
					.build();
		}
	}

	@GET
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.BROKER)
	public Response getBrokers() {
		try {
			List<Broker> brokers = BrokerPersistenceWrapper.getBrokers();
			return Response.ok().entity(brokers).build();
		} catch (Exception e) {
			logger.error("getBrokers hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(new ResponseExceptionBean(e.toString())).build();
		}
	}

	@GET
	@Path("/{name}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.BROKER)
	public Response getBroker(@PathParam("name") String name) {
		try {
			Broker broker = BrokerPersistenceWrapper.getBrokerByName(name);
			return Response.ok().entity(broker).tag(name).build();
		} catch (Exception e) {
			logger.error("getBrokers hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(new ResponseExceptionBean(e.toString())).tag(name)
					.build();
		}
	}

	public static void main(String[] args) {
		BrokerPersistenceWrapper.updateStatus("ocdp", BrokerStatus.CATALOG_INITIALIZED.name());
		System.out.println(">>> end of main");
	}

	/**
	 * Register service broker
	 *
	 * @return service
	 */
	@PUT
	@Path("/register")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.REGISTER, targetType = TargetType.BROKER)
	public Response registerServiceBroker(String reqBodyStr, @Context HttpServletRequest request) {
		String brokerurl = extractURL(reqBodyStr);
		try {

			String adToken = request.getHeader("token");
			if (adToken == null || adToken.isEmpty()) {
				return Response.status(Status.NOT_FOUND)
						.entity(new ResourceResponseBean("add service broker failed",
								"token is null or empty, please check the token in request header.",
								ResponseCodeConstant.EMPTY_TOKEN))
						.tag(brokerurl).build();
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
						.tag(brokerurl).build();
			}

			String url = OsClusterIni.getConf().get(Constant.SERVICE_CLUSTER).getProperty(Constant.OS_URL);
			String token = OsClusterIni.getConf().get(Constant.SERVICE_CLUSTER).getProperty(Constant.OS_TOKEN);
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
					BrokerPersistenceWrapper.updateStatus(extractName(reqBodyJson), BrokerStatus.REGISTERED.name());
					return Response.ok().entity(bodyStr).tag(brokerurl).build();
				} finally {
					response2.close();
				}
			} finally {
				httpclient.close();
			}
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("addServiceBroker hit exception-> ", e);
			return Response.status(Status.BAD_REQUEST).entity(new ResponseExceptionBean(e.toString())).tag(brokerurl)
					.build();
		}
	}

	private String extractName(JsonElement reqBodyJson) {
		JsonObject meta = reqBodyJson.getAsJsonObject().getAsJsonObject("metadata");
		String name = meta.getAsJsonPrimitive("name").getAsString();
		return name;
	}

	private String extractURL(String reqBodyStr) {
		JsonElement reqBodyJson = new JsonParser().parse(reqBodyStr);
		JsonObject spec = reqBodyJson.getAsJsonObject().getAsJsonObject("spec");
		return spec.get("url").getAsString();
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
		logger.error("delete not support in current version");
		return Response.status(Status.BAD_REQUEST).entity("Delete not support in current version")
				.tag(serviceBrokerName).build();
		// try {
		//
		// String adToken = request.getHeader("token");
		// if (adToken == null || adToken.isEmpty()) {
		// return Response.status(Status.NOT_FOUND)
		// .entity(new ResourceResponseBean("delete service broker failed",
		// "token is null or empty, please check the token in request header.",
		// ResponseCodeConstant.EMPTY_TOKEN))
		// .tag(serviceBrokerName).build();
		// }
		//
		// String loginUser = TokenPaserUtils.paserUserName(adToken);
		// logger.debug("deleteServiceBroker -> delete service broker with login user: "
		// + loginUser);
		//
		// UserRoleView role =
		// UserRoleViewPersistenceWrapper.getRoleBasedOnUserAndTenant(loginUser,
		// Constant.ROOTTENANTID);
		//
		// if (role == null || !(role.getRoleName().equals(Constant.SYSADMIN))) {
		// return Response.status(Status.UNAUTHORIZED)
		// .entity(new ResourceResponseBean("delete service broker failed",
		// "the user is not system admin role, does NOT have the add service broker
		// permission.",
		// ResponseCodeConstant.NO_DELETE_SERVICE_BROKER_PERMISSION))
		// .tag(serviceBrokerName).build();
		// }
		//
		// String url =
		// DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_URL);
		// String token =
		// DataFoundryConfiguration.getDFProperties().get(Constant.DATAFOUNDRY_TOKEN);
		// String dfRestUrl = url + "/oapi/v1/servicebrokers/" + serviceBrokerName;
		//
		// SSLConnectionSocketFactory sslsf =
		// SSLSocketIgnoreCA.createSSLSocketFactory();
		//
		// CloseableHttpClient httpclient =
		// HttpClients.custom().setSSLSocketFactory(sslsf).build();
		// try {
		// HttpDelete httpDelete = new HttpDelete(dfRestUrl);
		// httpDelete.addHeader("Content-type", "application/json");
		// httpDelete.addHeader("Authorization", "bearer " + token);
		//
		// CloseableHttpResponse response1 = httpclient.execute(httpDelete);
		//
		// try {
		// // int statusCode =
		// // response1.getStatusLine().getStatusCode();
		//
		// String bodyStr = EntityUtils.toString(response1.getEntity());
		//
		// return Response.ok().entity(bodyStr).tag(serviceBrokerName).build();
		// } finally {
		// response1.close();
		// }
		// } finally {
		// httpclient.close();
		// }
		// } catch (Exception e) {
		// // system out the exception into the console log
		// logger.error("deleteServiceBroker hit exception -> ", e);
		// return
		// Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(serviceBrokerName).build();
		// }
	}
	@POST
	@Path("/broker/catalog/{name}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.CREATE, targetType = TargetType.CATALOG)
	public Response initEtcd (@PathParam("name") String serviceBrokerName) {
		//get jsonArray from file
		JsonArray serviceList = null;
		try {
			serviceList = EtcdJson.getJsonArray();
		} catch(Exception e) {
			logger.error("fail to get jsonArray from file", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(serviceBrokerName).build();
		}
		//init etcd client
		try {
			CmEtcdClient etcd_client = CmEtcdClient.getInstance();
			etcd_client.check(serviceBrokerName);
			if (serviceList != null) {
				for (int serviceNum = 0; serviceNum < serviceList.size(); serviceNum++) {
					JsonObject serviceAction = serviceList.get(serviceNum).getAsJsonObject();
					JsonArray actionList = serviceAction.get("action-list").getAsJsonArray();
					String uuid = UUID.randomUUID().toString();
					for (int actionNum = 0; actionNum < actionList.size(); actionNum++ ) {
						JsonObject action = actionList.get(actionNum).getAsJsonObject();
						action.addProperty("key", action.get("key").getAsString().replace("${broker-id}", serviceBrokerName));
						action.addProperty("key", action.get("key").getAsString().replace("${catalog-id}", uuid));
						execAction(action);
					}
				}
			}
		} catch (Exception e) {
			logger.error("fail to init etcd", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(serviceBrokerName).build();
		}
		BrokerPersistenceWrapper.insert(new Broker(serviceBrokerName));
		return Response.ok().entity(serviceList.getAsString()).tag(serviceBrokerName).build();
	}
	private void execAction(JsonObject action) {
		
		CmEtcdClient etcd_client = CmEtcdClient.getInstance();
		
		switch (action.get("action").getAsString()) {
		case "createDir" :
			etcd_client.createDir(action.get("key").getAsString());
			break;
		case "write" :
			etcd_client.write(action.get("key").getAsString(), action.get("value").getAsString());
			break;
		default :
			logger.error("error action when init etcd");
			throw new RuntimeException("unexpect action : " + action);
		}
	}
}
