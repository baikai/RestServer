package com.asiainfo.ocmanager.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class KerberosConfiguration {

	private static Properties conf;

	public static Properties getConf() {
		if (conf == null) {
			synchronized (KerberosConfiguration.class) {
				if (conf == null) {
					new KerberosConfiguration();
				}
			}
		}
		return conf;
	}

	private KerberosConfiguration() {
		loadConf();
	}

	private void loadConf() {
		InputStream inputStream = null;
		try {
			String base = this.getClass().getResource("/").getPath() + ".." + File.separator;
			String confpath = base + "conf" + File.separator + "kerberos.properties";
			inputStream = new FileInputStream(new File(confpath));
			conf = new Properties();
			conf.load(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
