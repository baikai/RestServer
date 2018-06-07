package com.asiainfo.ocmanager.rest.resource;

import java.io.File;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.log4j.Logger;

import com.asiainfo.ocmanager.audit.Audit;
import com.asiainfo.ocmanager.audit.Audit.Action;
import com.asiainfo.ocmanager.audit.Audit.TargetType;
import com.asiainfo.ocmanager.rest.bean.ResourceResponseBean;
import com.asiainfo.ocmanager.rest.constant.Constant;
import com.asiainfo.ocmanager.rest.constant.ResponseCodeConstant;
import com.asiainfo.ocmanager.rest.utils.UUIDFactory;
import com.asiainfo.ocmanager.service.client.KrbClient;
import com.asiainfo.ocmanager.service.exception.KerberosOperationException;
import com.asiainfo.ocmanager.utils.KerberosConfiguration;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Path("/kerberos")
public class KerberosResource {

	private static Logger logger = Logger.getLogger(KerberosResource.class);

	private static final String PATH = KerberosResource.class.getResource("/").getPath() + ".." + File.separator
			+ "keytabs" + File.separator + "oc.{$username}.keytab";
	private static final String KRB5PATH = KerberosConfiguration.getConf().getProperty(Constant.KERBEROS_KRB5_LOCATION);

	@GET
	@Path("krb5")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Audit(action = Action.GET, targetType = TargetType.KRB5_FILE)
	public Response getKrb5File() {
		try {

			File file = new File(KRB5PATH);

			if (!file.exists()) {
				logger.error("File not found: " + file.getPath());
				return Response.status(Response.Status.NOT_FOUND)
						.entity(new ResourceResponseBean("download keytab failed!",
								"krb5.conf is NOT exist, please check with admin,"
										+ " and make sure kerberos client is installed on the OCM server.",
								ResponseCodeConstant.CAN_NOT_FIND_KRB5_CONF_FILE))
						.tag(KRB5PATH).build();
			}

			return Response.ok(file).header("Content-disposition", "attachment;filename=" + "krb5.conf")
					.header("Cache-Control", "no-cache").tag(KRB5PATH).build();

		} catch (Exception e) {
			logger.error("getKrb5File hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(KRB5PATH).build();
		}
	}

	@GET
	@Path("keytab/{userName}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Audit(action = Action.GET, targetType = TargetType.KEYTAB)
	public Response getKeyTabFile(@PathParam("userName") String userName) {
		try {

			File file = new File(PATH.replace("{$username}", userName.trim()));

			if (!file.exists()) {
				logger.error("File not found: " + file.getPath());
				return Response.status(Response.Status.NOT_FOUND)
						.entity(new ResourceResponseBean("download keytab failed!",
								userName + ".keytab is NOT exist, please generate the keytab by call"
										+ " REST API: /ocmanager/v1/api/kerberos/keytab ",
								ResponseCodeConstant.CAN_NOT_FIND_KRB_KEYTAB_FILE))
						.tag(file.getPath()).build();
			}

			return Response.ok(file).header("Content-disposition", "attachment;filename=" + userName + ".keytab")
					.header("Cache-Control", "no-cache").tag(file.getPath()).build();

		} catch (Exception e) {
			logger.error("getKeyTabFile hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString())
					.tag(PATH.replace("{$username}", userName.trim())).build();
		}
	}

	@POST
	@Path("create/keytab")
	@Produces((MediaType.APPLICATION_JSON + Constant.SEMICOLON + Constant.CHARSET_EQUAL_UTF_8))
	@Consumes(MediaType.APPLICATION_JSON)
	@Audit(action = Action.CREATE, targetType = TargetType.KEYTAB)
	public Response createKeyTabFile(String requestBody) {
		String krbusername = null;
		try {
			JsonElement req = new JsonParser().parse(requestBody);
			JsonObject obj = req.getAsJsonObject();

			JsonElement krbusernameJE = obj.get("krbusername");
			// JsonElement krbpasswordJE = obj.get("krbpassword");

			if (krbusernameJE == null) {
				return Response.status(Status.BAD_REQUEST)
						.entity(new ResourceResponseBean("generate keytab failed",
								"can NOT find krbusername in the request boday, please make sure you "
										+ "input the krbusername and krbpassword in the request body correctly.",
								ResponseCodeConstant.CAN_NOT_FIND_KRB_USERNAME_OR_PASSWORD))
						.tag("UserNameNull").build();
			}

			logger.info("Creating keytab for user " + krbusernameJE + " with password random password.");

			krbusername = krbusernameJE.getAsString();
			// String krbpassword = krbpasswordJE.getAsString();

			File file = new File(PATH.replace("{$username}", krbusername.trim()));

			if (!file.exists()) {
				// use the uuid as the random password
				this.createKeyTabFile(krbusername, UUIDFactory.getUUID());
				logger.info("Creating keytab successful, user: " + krbusernameJE);
			} else {
				logger.warn("The keytab already existed for user: " + krbusername + "NOT need to generate again.");
			}

			return Response.ok().entity(new ResourceResponseBean("generate keytab successfully!",
					krbusername + ".keytab created", ResponseCodeConstant.SUCCESS)).tag(file.getPath()).build();
		} catch (Exception e) {
			// system out the exception into the console log
			logger.error("createKeyTabFile hit exception -> ", e);
			return Response.status(Status.BAD_REQUEST).entity(e.toString()).tag(PATH.replace("{$username}", krbusername.trim())).build();
		}
	}

	private void createKeyTabFile(String krbusername, String krbpassword) throws KerberosOperationException {
		KrbClient client = KrbClient.getInstance();
		String principalName = krbusername + "@" + KerberosConfiguration.getConf().getProperty(Constant.KERBEROS_REALM);

		// if the pricipal exists remove first
		// then user the random password create a new one
		if (client.principalExists(principalName)) {
			client.removePrincipal(principalName);
			client.createPrincipal(principalName, krbpassword);
		} else {
			client.createPrincipal(principalName, krbpassword);
		}

		Keytab keyTab = client.createKeyTab(principalName, krbpassword, null);

		client.createKeyTabFile(keyTab, PATH.replace("{$username}", krbusername.trim()));
	}

}
