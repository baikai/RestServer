package com.asiainfo.ocmanager.rest.resource;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.asiainfo.ocmanager.audit.Audit;
import com.asiainfo.ocmanager.audit.Audit.Action;
import com.asiainfo.ocmanager.audit.Audit.TargetType;
import com.asiainfo.ocmanager.rest.bean.ResourceResponseBean;
import com.asiainfo.ocmanager.rest.constant.ResponseCodeConstant;

@Path("/file")
public class ExternalFileResource {
	private static Logger logger = Logger.getLogger(ExternalFileResource.class);
	private static final String BASE = ExternalFileResource.class.getResource("/").getPath() + ".." + File.separator
			+ "external" + File.separator;

	@GET
	@Path("/clusterHosts")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Audit(action = Action.GET, targetType = TargetType.EXTERNAL_FILE)
	public Response getHostsFile() {
		try {
			String path = BASE + "hosts";
			File file = new File(path);

			if (!file.exists()) {
				logger.error("File not found: " + file.getPath());
				return Response.status(Response.Status.NOT_FOUND)
						.entity(new ResourceResponseBean("download hosts file failed!",
								"hosts file NOT exist, please consult admin,",
								ResponseCodeConstant.CAN_NOT_FIND_EXTERNAL_FILE))
						.tag(path).build();
			}

			return Response.ok(file).header("Content-disposition", "attachment;filename=" + "hosts")
					.header("Cache-Control", "no-cache").tag(path).build();

		} catch (Exception e) {
			logger.error("Get external file exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(BASE + "hosts").build();
		}
	}

}
