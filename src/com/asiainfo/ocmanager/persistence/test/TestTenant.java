package com.asiainfo.ocmanager.persistence.test;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.asiainfo.ocmanager.persistence.mapper.TenantMapper;
import com.asiainfo.ocmanager.persistence.model.Tenant;

/**
 * 
 * @author zhaoyim
 *
 */
public class TestTenant {

	public static void main(String[] args) {
		SqlSession session = TestDBConnectorFactory.getSession();
		try {
			TenantMapper mapper = session.getMapper(TenantMapper.class);

			System.out.println("=== Insert Tenant ===");
			mapper.insertTenant(new Tenant("id8", "name", "description", null, 1));

			List<Tenant> tenants = mapper.selectAllTenants();

			System.out.println("=== All tenants ===");
			for (Tenant t : tenants) {
				System.out.println(t.getId());
				System.out.println(t.getName());
				System.out.println(t.getDescription());
				System.out.println(t.getParentId());
			}

			List<Tenant> children = mapper.selectChildrenTenants("2");

			System.out.println("=== Children tenants ===");
			for (Tenant c : children) {
				System.out.println(c.getId());
				System.out.println(c.getName());
				System.out.println(c.getDescription());
				System.out.println(c.getParentId());
			}

			Tenant tenant = mapper.selectTenantById("id7");
			System.out.println(tenant);
			System.out.println(tenant.getId());
			System.out.println(tenant.getName());

			System.out.println("=== delete tenant ===");
			mapper.deleteTenant("id8");

			System.out.println("=== get root tenants ===");
			List<Tenant> roots = mapper.selectAllRootTenants();
			for (Tenant r : roots) {
				System.out.println(r.getId());
				System.out.println(r.getName());
				System.out.println(r.getDescription());
				System.out.println(r.getParentId());
				System.out.println(r.getLevel());
			}

			System.out.println("=== update tenant name ===");
			mapper.updateTenantName("t1", "aaaaaaaa");
			
			session.commit();
		} catch (Exception e) {
			session.rollback();
		} finally {
			session.close();
		}

	}

}
