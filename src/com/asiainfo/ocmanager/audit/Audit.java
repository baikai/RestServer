package com.asiainfo.ocmanager.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for auditting
 * @author Ethan
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.METHOD })
public @interface Audit {
	/**
	 * target type
	 * @return
	 */
	TargetType targetType();
	/**
	 * action on entity
	 * @return
	 */
	Action action();
	
	/**
	 * User-end action type
	 * @author Ethan
	 *
	 */
	public static enum Action {
		GET,
		CREATE,
		UPDATE,
		DELETE,
		LOGIN,
		LOGOUT,
		ASSIGN,
		UNASSIGN
	}
	
	/**
	 * Entity type to audit of user-end operation
	 * @author Ethan
	 *
	 */
	public static enum TargetType {
		/**
		 * tenant
		 */
		TENANT,
		/**
		 * tenants
		 */
		TENANTS,
		/**
		 * instance
		 */
		INSTANCE,
		/**
		 * instances
		 */
		INSTANCES,
		/**
		 * user
		 */
		USER,
		/**
		 * users
		 */
		USERS,
		/**
		 * Ldap users
		 */
		LDAP_USERS,
		/**
		 * role
		 */
		ROLE,
		/**
		 * service
		 */
		SERVICE,
		/**
		 * service plan
		 */
		SERVICE_PLAN,
		/**
		 * services
		 */
		SERVICES,
		/**
		 * assignment
		 */
		ASSIGNMENT,
		/**
		 * quotas
		 */
		QUOTAS,
		/**
		 * access info
		 */
		ACCESSINFO,
		/**
		 * tenant lifetime
		 */
		TENANT_LIFETIME,
		/**
		 * service broker
		 */
		BROKER,
		/**
		 * keytab
		 */
		KEYTAB,
		/**
		 * krb5.conf
		 */
		KRB5_FILE,
		/**
		 * external file
		 */
		EXTERNAL_FILE,
		/**
		 * ambari resouce
		 */
		AMBARI_RESOURCES,
		/**
		 * http request
		 */
		HTTP_REQUEST
	}
}
