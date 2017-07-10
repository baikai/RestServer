package com.asiainfo.ocmanager.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.asiainfo.ocmanager.persistence.model.Tenant;

/**
 * 
 * @author zhaoyim
 *
 */

public interface TenantMapper {

	/**
	 * 
	 * @param id
	 * @return
	 */
	public Tenant selectTenantById(String id);

	/**
	 * 
	 * @param tenant
	 */
	public void insertTenant(Tenant tenant);

	/**
	 * 
	 * @return
	 */
	public List<Tenant> selectAllTenants();

	/**
	 * 
	 * @return
	 */
	public List<Tenant> selectChildrenTenants(String parentTenantId);

	/**
	 * 
	 * @param id
	 */
	public void deleteTenant(@Param("id") String id);

	/**
	 * 
	 * @return
	 */
	public List<Tenant> selectAllRootTenants();
	
	/**
	 * 
	 * @param tenant
	 */
	public void updateTenantName(@Param("id") String id, @Param("name") String name);
	

}
