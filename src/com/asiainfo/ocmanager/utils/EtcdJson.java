package com.asiainfo.ocmanager.utils;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * RestServer configuration.
 * 
 * @author EthanWang
 *
 */

public class EtcdJson {
	private static final Logger LOG = LoggerFactory.getLogger(EtcdJson.class);
	private static JsonArray actionList;

	public static JsonArray getJsonArray() {
		if (actionList == null) {
			synchronized (EtcdJson.class) {
				if (actionList == null) {
					new EtcdJson();
				}
			}
		}
		return actionList;
}
	private EtcdJson() {
		loadFile();
	}

	private void loadFile() {
		//get file path
		File inFile = null;
		String path = null;
		try {
			String base = ServicesIni.class.getResource("/").getPath() + ".." + File.separator;
			path = base + "conf" + File.separator + "initEtcd.json";
			inFile = new File(path);
			if (!inFile.exists()) {
				LOG.error("File not found: " + path);
				throw new RuntimeException("File not found: " + path);
			}
		} catch(Exception e) {
			LOG.error("fail to get file path", e);
			throw new RuntimeException("fail to get file path: " + path);
		}
		//get jsonArray from file
		 actionList = null;
		try {
			String input = FileUtils.readFileToString(inFile, "UTF-8");
			JsonElement jsoninfile = new JsonParser().parse(input);
			actionList = jsoninfile.getAsJsonArray();
		} catch (Exception e) {
			LOG.error("fail to get jsonArray from file: " + path, e);
			throw new RuntimeException("fail to get jsonArray from file: " + path);
		}
	}
}
