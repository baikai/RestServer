package com.asiainfo.ocmanager.rest.resource;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asiainfo.ocmanager.audit.Audit;
import com.asiainfo.ocmanager.audit.Audit.Action;
import com.asiainfo.ocmanager.audit.Audit.TargetType;
import com.asiainfo.ocmanager.rest.bean.QuotaResponse;
import com.asiainfo.ocmanager.rest.constant.Constant;
import com.asiainfo.ocmanager.rest.utils.PeekerUtils;
import com.asiainfo.ocmanager.service.broker.ResourcePeeker;
import com.asiainfo.ocmanager.service.broker.utils.ResourcePeekerFactory;

@Path("/quota")
public class QuotaResource {
	private static final Logger LOG = LoggerFactory.getLogger(QuotaResource.class);

	@GET
	@Path("hdfs")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.QUOTAS)
	public Response getHdfsQuota(@Context HttpServletRequest request) {
		String service = null;
		String path = null;
		try {
			service = request.getParameter("service");
			path = request.getParameter("path");
			LOG.info("Received quota request of service [{}], resource path: [{}]", service, path);
			ResourcePeeker peeker = ResourcePeekerFactory.getPeeker(service);
			long begin = System.currentTimeMillis();
			QuotaResponse response = PeekerUtils.transform(peeker.peekOn(Arrays.asList(path)));
			LOG.info("Response been returned within {}ms.", System.currentTimeMillis() - begin);
			return Response.ok().entity(response).tag(String.join("->", service, path)).build();
		} catch (Exception e) {
			LOG.error("Error while fetching resource usage: ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", service, path))
					.build();
		}
	}

	@GET
	@Path("mapreduce/{queuename}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.QUOTAS)
	public Response getYarnQuota(@PathParam("queuename") String queuename, @Context HttpServletRequest request) {
		String service = null;
		try {
			service = request.getParameter("service");
			LOG.info("Received quota request of service [{}], resource path: [{}]", service, queuename);
			ResourcePeeker peeker = ResourcePeekerFactory.getPeeker(service);
			long begin = System.currentTimeMillis();
			QuotaResponse response = PeekerUtils.transform(peeker.peekOn(Arrays.asList(queuename)));
			LOG.info("Response been returned within {}ms.", System.currentTimeMillis() - begin);
			return Response.ok().entity(response).tag(String.join("->", service, queuename)).build();
		} catch (Exception e) {
			LOG.error("Error while fetching resource usage: ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", service, queuename))
					.build();
		}
	}

	@GET
	@Path("hbase/{namespace}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.QUOTAS)
	public Response getHbaseQuota(@PathParam("namespace") String namespace, @Context HttpServletRequest request) {
		String service = null;
		try {
			service = request.getParameter("service");
			LOG.info("Received quota request of service [{}], resource path: [{}]", service, namespace);
			ResourcePeeker peeker = ResourcePeekerFactory.getPeeker(service);
			long begin = System.currentTimeMillis();
			QuotaResponse response = PeekerUtils.transform(peeker.peekOn(Arrays.asList(namespace)));
			LOG.info("Response been returned within {}ms.", System.currentTimeMillis() - begin);
			return Response.ok().entity(response).tag(String.join("->", service, namespace)).build();
		} catch (Exception e) {
			LOG.error("Error while fetching resource usage: ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", service, namespace))
					.build();
		}
	}

	@GET
	@Path("kafka/{topic}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.QUOTAS)
	public Response getKafkaQuota(@PathParam("topic") String topic, @Context HttpServletRequest request) {
		String service = null;
		try {
			service = request.getParameter("service");
			LOG.info("Received quota request of service [{}], resource path: [{}]", service, topic);
			ResourcePeeker peeker = ResourcePeekerFactory.getPeeker(service);
			long begin = System.currentTimeMillis();
			QuotaResponse response = PeekerUtils.transform(peeker.peekOn(Arrays.asList(topic)));
			LOG.info("Response been returned within {}ms.", System.currentTimeMillis() - begin);
			return Response.ok().entity(response).tag(String.join("->", service, topic)).build();
		} catch (Exception e) {
			LOG.error("Error while fetching resource usage: ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", service, topic))
					.build();
		}
	}

	@GET
	@Path("greenplum/{serviceInstanceId}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.QUOTAS)
	public Response getGpQuota(@PathParam("serviceInstanceId") String instanceId, @Context HttpServletRequest request) {
		String service = null;
		try {
			service = request.getParameter("service");
			LOG.info("Received quota request of service [{}], resource path: [{}]", service, instanceId);
			ResourcePeeker peeker = ResourcePeekerFactory.getPeeker(service);
			long begin = System.currentTimeMillis();
			QuotaResponse response = PeekerUtils.transform(peeker.peekOn(Arrays.asList(instanceId)));
			LOG.info("Response been returned within {}ms.", System.currentTimeMillis() - begin);
			return Response.ok().entity(response).tag(String.join("->", service, instanceId)).build();
		} catch (Exception e) {
			LOG.error("Error while fetching resource usage: ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", service, instanceId))
					.build();
		}
	}

	@GET
	@Path("mongodb/{serviceInstanceId}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.QUOTAS)
	public Response getMongoQuota(@PathParam("serviceInstanceId") String instanceId,
			@Context HttpServletRequest request) {
		String service = null;
		try {
			service = request.getParameter("service");
			LOG.info("Received quota request of service [{}], resource path: [{}]", service, instanceId);
			ResourcePeeker peeker = ResourcePeekerFactory.getPeeker(service);
			long begin = System.currentTimeMillis();
			QuotaResponse response = PeekerUtils.transform(peeker.peekOn(Arrays.asList(instanceId)));
			LOG.info("Response been returned within {}ms.", System.currentTimeMillis() - begin);
			return Response.ok().entity(response).tag(String.join("->", service, instanceId)).build();
		} catch (Exception e) {
			LOG.error("Error while fetching resource usage: ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", service, instanceId))
					.build();
		}
	}

	@GET
	@Path("spark/{queueName}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.QUOTAS)
	public Response getSparkQuota(@PathParam("queueName") String queueName, @Context HttpServletRequest request) {
		String service = null;
		try {
			service = request.getParameter("service");
			LOG.info("Received quota request of service [{}], resource path: [{}]", service, queueName);
			ResourcePeeker peeker = ResourcePeekerFactory.getPeeker(service);
			long begin = System.currentTimeMillis();
			QuotaResponse response = PeekerUtils.transform(peeker.peekOn(Arrays.asList(queueName)));
			LOG.info("Response been returned within {}ms.", System.currentTimeMillis() - begin);
			return Response.ok().entity(response).tag(String.join("->", service, queueName)).build();
		} catch (Exception e) {
			LOG.error("Error while fetching spark resource usage: ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", service, queueName))
					.build();
		}
	}

	@GET
	@Path("hive/{dbname}")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Audit(action = Action.GET, targetType = TargetType.QUOTAS)
	public Response getHiveQuota(@PathParam("dbname") String dbname, @Context HttpServletRequest request) {
		String service = null;
		try {
			service = request.getParameter("service");
			LOG.info("Received quota request of service [{}], resource path: [{}]", service, dbname);
			ResourcePeeker peeker = ResourcePeekerFactory.getPeeker(service);
			String path = "/apps/hive/warehouse/" + dbname + ".db";
			long begin = System.currentTimeMillis();
			QuotaResponse response = PeekerUtils.transform(peeker.peekOn(Arrays.asList(path)));
			LOG.info("Response been returned within {}ms.", System.currentTimeMillis() - begin);
			return Response.ok().entity(response).tag(String.join("->", service, dbname)).build();
		} catch (Exception e) {
			LOG.error("Error while fetching resource usage: ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(String.join("->", service, dbname))
					.build();
		}
	}

}
