package com.asiainfo.ocmanager.rest.resource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.asiainfo.ocmanager.audit.Audit;
import com.asiainfo.ocmanager.audit.Audit.Action;
import com.asiainfo.ocmanager.audit.Audit.TargetType;
import com.asiainfo.ocmanager.service.client.AmbariClient;
import com.asiainfo.ocmanager.service.client.ClusterFactory;

/**
 * 
 * @author zhaoyim
 *
 */

@Path("/ambari")
public class AmbariResource {

	private static Logger logger = Logger.getLogger(AmbariResource.class);

	private static String tarGz = ".tar.gz";

	/**
	 * get yarn client configuration files from ambari
	 * 
	 * @param filename
	 * @return
	 */
	@GET
	@Path("yarnclient")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Audit(action = Action.GET, targetType = TargetType.AMBARI_RESOURCES)
	public Response getYarnClientFiles(@QueryParam("filename") String filename, @Context HttpServletRequest request) {

		try {
			if (filename == null || filename.isEmpty()) {
				filename = "yarnclient";
			}
			String cluster = request.getParameter("cluster");
			AmbariClient ambari = ClusterFactory.getAmbari(cluster);
			byte[] file = ambari.getFile("/YARN/components/YARN_CLIENT?format=client_config_tar");
			return Response.ok(file)
					.header("Content-disposition", "attachment;filename=" + filename + tarGz)
					.header("Cache-Control", "no-cache").tag(filename).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getYarnClientFiles hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(filename).build();
		}
	}

	/**
	 * get hdfs client configuration files from ambari
	 * 
	 * @param filename
	 * @return
	 */
	@GET
	@Path("hdfsclient")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Audit(action = Action.GET, targetType = TargetType.AMBARI_RESOURCES)
	public Response getHdfsClientFiles(@QueryParam("filename") String filename, @Context HttpServletRequest request) {
		try {
			if (filename == null || filename.isEmpty()) {
				filename = "hdfsclient";
			}
			String cluster = request.getParameter("cluster");
			AmbariClient ambari = ClusterFactory.getAmbari(cluster);
			byte[] file = ambari.getFile("/HDFS/components/HDFS_CLIENT?format=client_config_tar");
			return Response.ok(file)
					.header("Content-disposition", "attachment;filename=" + filename + tarGz)
					.header("Cache-Control", "no-cache").tag(filename).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("getHdfsClientFiles hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(filename).build();
		}
	}

	/**
	 * get spark client configuration files from ambari
	 * 
	 * @param filename
	 * @return
	 */
	@GET
	@Path("sparkclient")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Audit(action = Action.GET, targetType = TargetType.AMBARI_RESOURCES)
	public Response getSparkClientFiles(@QueryParam("filename") String filename, @Context HttpServletRequest request) {
		try {
			if (filename == null || filename.isEmpty()) {
				filename = "sparkclient";
			}
			String cluster = request.getParameter("cluster");
			AmbariClient ambari = ClusterFactory.getAmbari(cluster);
			byte[] file = ambari.getFile("/SPARK/components/SPARK_CLIENT?format=client_config_tar");
			return Response.ok(file)
					.header("Content-disposition", "attachment;filename=" + filename + tarGz)
					.header("Cache-Control", "no-cache").tag(filename).build();
		} catch (Exception e) {
			logger.error("getHdfsClientFiles hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(filename).build();
		}
	}

}
