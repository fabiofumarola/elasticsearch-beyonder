/*
 * Licensed to David Pilato (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.pilato.elasticsearch.tools.template;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.client.Client;

/**
 * By default, indexes are created with their default Elasticsearch settings. You can specify
 * your own settings for your index by putting a /es/indexname/_settings.json in your classpath.
 * <br>
 * So if you create a file named /es/twitter/_settings.json in your src/main/resources folder (for maven lovers),
 * it will be used by the factory to create the twitter index.
 * <pre>
 * {@code
 * {
 *   "index" : {
 *     "number_of_shards" : 3,
 *     "number_of_replicas" : 2
 *   }
 * }
 * </pre>
 * By default, types are not created and wait for the first document you send to Elasticsearch (auto mapping).
 * But, if you define a file named /es/indexname/type.json in your classpath, the type will be created at startup using
 * the type definition you give.
 * <br>
 * So if you create a file named /es/twitter/tweet.json in your src/main/resources folder (for maven lovers),
 * it will be used by the factory to create the tweet type in twitter index.
 * <pre>
 * {@code
 * {
 *   "tweet" : {
 *     "properties" : {
 *       "message" : {"type" : "string", "store" : "yes"}
 *     }
 *   }
 * }
 * </pre>
 *
 * By convention, the factory will create all settings and mappings found under the /es classpath.<br>
 * You can disable convention and use configuration by setting autoscan to false.
 * @author David Pilato
 */
public class TemplateElasticsearchUpdater {

	private static final Logger logger = LogManager.getLogger(TemplateElasticsearchUpdater.class);

	/**
	 * Create a template in Elasticsearch.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param template Template name
	 * @throws Exception
	 */
	public static void createTemplate(Client client, String root, String template, boolean force) throws Exception {
		String json = TemplateSettingsReader.readTemplate(root, template);
		createTemplateWithJson(client, template, json, force);
	}

	/**
	 * Create a template in Elasticsearch. Read read content from default classpath dir.
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @throws Exception
	 */
	public static void createTemplate(Client client, String template, boolean force) throws Exception {
		String json = TemplateSettingsReader.readTemplate(template);
		createTemplateWithJson(client, template, json, force);
	}

	/**
	 * Create a new template in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @param force set it to true if you want to force cleaning template before adding it
	 * @throws Exception
	 */
	public static void createTemplateWithJson(Client client, String template, String json, boolean force) throws Exception {
		if (isTemplateExist(client, template)) {
			if (force) {
				logger.debug("Template [{}] already exists. Force is set. Removing it.", template);
				removeTemplate(client, template);
			} else {
				logger.debug("Template [{}] already exists.", template);
			}
		}

		if (!isTemplateExist(client, template)) {
			logger.debug("Template [{}] doesn't exist. Creating it.", template);
			createTemplateWithJsonInElasticsearch(client, template, json);
		}
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param template Template name
	 * @param json JSon content for the template
	 * @throws Exception
	 */
	private static void createTemplateWithJsonInElasticsearch(Client client, String template, String json) throws Exception {
		logger.trace("createTemplate([{}])", template);

		assert client != null;
		assert template != null;

		PutIndexTemplateResponse response = client.admin().indices()
				.preparePutTemplate(template)
				.setSource(json)
				.get();

		if (!response.isAcknowledged()) {
			logger.warn("Could not create template [{}]", template);
			throw new Exception("Could not create template ["+template+"].");
		}

		logger.trace("/createTemplate([{}])", template);
	}

	/**
	 * Check if a template exists
	 * @param template template name
	 */
	public static boolean isTemplateExist(Client client, String template) {
		return !client.admin().indices().prepareGetTemplates(template).get().getIndexTemplates().isEmpty();
	}

	/**
	 * Remove a template
	 * @param template
	 * @throws Exception
	 */
	public static void removeTemplate(Client client, String template) throws Exception {
		logger.trace("removeTemplate({})", template);
		client.admin().indices().prepareDeleteTemplate(template).get();
		logger.trace("/removeTemplate({})", template);
	}

}
