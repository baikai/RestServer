package com.asiainfo.ocmanager.rest.resource.utils;

import java.util.ArrayList;
import java.util.List;

import com.asiainfo.ocmanager.persistence.mapper.TenantUserRoleAssignmentMapper;
import com.asiainfo.ocmanager.persistence.model.TenantUserRoleAssignment;
import org.apache.ibatis.session.SqlSession;

import com.asiainfo.ocmanager.persistence.mapper.TenantMapper;
import com.asiainfo.ocmanager.persistence.model.Tenant;
import com.asiainfo.ocmanager.persistence.DBConnectorFactory;

/**
 *
 * @author zhaoyim
 *
 */
public class TenantPersistenceWrapper {

	private static final String ADMINID = "2ef26018-003d-4b2b-b786-0481d4ee9fa8";
	private static final String ADMINROLEID = "a10170cb-524a-11e7-9dbb-fa163ed7d0ae";

	/**
	 *
	 * @param tenant
	 */
	public static void createTenant(Tenant tenant) {
		SqlSession session = DBConnectorFactory.getSession();
		try {
			TenantMapper mapper = session.getMapper(TenantMapper.class);
			TenantUserRoleAssignmentMapper tenantUserRoleAssignmentMapper = session
					.getMapper(TenantUserRoleAssignmentMapper.class);
			TenantUserRoleAssignment tenantUserRoleAssignment = new TenantUserRoleAssignment(tenant.getId(), ADMINID,
					ADMINROLEID);

			mapper.insertTenant(tenant);
			tenantUserRoleAssignmentMapper.insertTenantUserRoleAssignment(tenantUserRoleAssignment);

			session.commit();
		} catch (Exception e) {
			session.rollback();
			throw e;
		} finally {
			session.close();
		}
	}

	/**
	 *
	 * @return
	 */
	public static List<Tenant> getAllTenants() {
		SqlSession session = DBConnectorFactory.getSession();
		List<Tenant> tenants = new ArrayList<Tenant>();
		try {
			TenantMapper mapper = session.getMapper(TenantMapper.class);

			tenants = mapper.selectAllTenants();

			session.commit();
		} catch (Exception e) {
			session.rollback();
			throw e;
		} finally {
			session.close();
		}

		return tenants;
	}

	/**
	 *
	 * @param tenantId
	 * @return
	 */
	public static Tenant getTenantById(String tenantId) {
		SqlSession session = DBConnectorFactory.getSession();
		Tenant tenant = null;
		try {
			TenantMapper mapper = session.getMapper(TenantMapper.class);
			tenant = mapper.selectTenantById(tenantId);
			session.commit();
		} catch (Exception e) {
			session.rollback();
			throw e;
		} finally {
			session.close();
		}
		return tenant;
	}

	/**
	 *
	 * @param parentTenantId
	 * @return
	 */
	public static List<Tenant> getChildrenTenants(String parentTenantId) {
		SqlSession session = DBConnectorFactory.getSession();
		List<Tenant> childrenTenants = new ArrayList<Tenant>();
		try {
			TenantMapper mapper = session.getMapper(TenantMapper.class);
			childrenTenants = mapper.selectChildrenTenants(parentTenantId);
			session.commit();
		} catch (Exception e) {
			session.rollback();
			throw e;
		} finally {
			session.close();
		}
		return childrenTenants;
	}

	/**
	 *
	 * @param tenantId
	 */
	public static void deleteTenant(String tenantId) {
		SqlSession session = DBConnectorFactory.getSession();

		try {
			TenantMapper mapper = session.getMapper(TenantMapper.class);
			TenantUserRoleAssignmentMapper tenantUserRoleAssignmentMapper = session
					.getMapper(TenantUserRoleAssignmentMapper.class);
			tenantUserRoleAssignmentMapper.deleteTenantUserRoleAssignment(tenantId, ADMINID);
			mapper.deleteTenant(tenantId);

			session.commit();
		} catch (Exception e) {
			session.rollback();
			throw e;
		} finally {
			session.close();
		}
	}

	/**
	 * 
	 * @return
	 */
	public static List<Tenant> getAllRootTenants() {
		SqlSession session = DBConnectorFactory.getSession();
		List<Tenant> tenants = new ArrayList<Tenant>();
		try {
			TenantMapper mapper = session.getMapper(TenantMapper.class);
			tenants = mapper.selectAllRootTenants();
			session.commit();
		} catch (Exception e) {
			session.rollback();
			throw e;
		} finally {
			session.close();
		}

		return tenants;
	}

	/**
	 * 
	 * @param tenantId
	 * @param newName
	 */
	public static void updateTenantName(String tenantId, String newName) {
		SqlSession session = DBConnectorFactory.getSession();
		try {
			TenantMapper mapper = session.getMapper(TenantMapper.class);
			mapper.updateTenantName(tenantId, newName);
			session.commit();
		} catch (Exception e) {
			session.rollback();
			throw e;
		} finally {
			session.close();
		}
	}

	/**
	 * 
	 * @param level
	 * @return
	 */
	public static List<Tenant> getTenantsByLevel(int level) {
		SqlSession session = DBConnectorFactory.getSession();
		List<Tenant> tenants = new ArrayList<Tenant>();
		try {
			TenantMapper mapper = session.getMapper(TenantMapper.class);
			tenants = mapper.selectTenantsByLevel(level);
			session.commit();
		} catch (Exception e) {
			session.rollback();
			throw e;
		} finally {
			session.close();
		}

		return tenants;
	}

}
