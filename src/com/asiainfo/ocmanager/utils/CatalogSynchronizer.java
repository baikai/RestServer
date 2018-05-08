package com.asiainfo.ocmanager.utils;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asiainfo.ocmanager.rest.resource.persistence.TenantPersistenceWrapper;
import com.google.common.collect.Table;

/**
 * Sync up catalog with resources
 * 
 * @author Ethan
 *
 */
public class CatalogSynchronizer {
	private static final Logger LOG = LoggerFactory.getLogger(CatalogSynchronizer.class);
	private static Catalog catalog;
	static {
		try {
			catalog = Catalog.getInstance();
		} catch (Throwable e) {
			LOG.error("Exception while init class: ", e);
			throw new RuntimeException("Exception while init class: ", e);
		}
	}

	/**
	 * Synchronize catalog quotas with tenants in DB
	 */
	public static void syncWithTenants() {
		catalog.listAllServices().values().forEach(s -> {
			List<String> quotas = catalog.getServiceQuotaKeys(s);
			updateTenants(s, quotas);
		});
	}

	private static void updateTenants(String s, List<String> quotas) {
		TenantPersistenceWrapper.getAllTenants().forEach(t -> {
			Table<String, String, String> tenantQuotas = QuotaParser.parse(t.getQuota());
			if (!tenantQuotas.containsRow(s)) {
				append(tenantQuotas, s, quotas);
			}
			t.setQuota(QuotaParser.toString(tenantQuotas));
			TenantPersistenceWrapper.updateTenant(t);
		});
	}

	private static void append(Table<String, String, String> tenantQuotas, String s, List<String> quotas) {
		quotas.forEach(q -> {
			tenantQuotas.put(s, q, String.valueOf(0));
		});
	}
}
