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

package fr.pilato.elasticsearch.tools.index;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
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
public class IndexElasticsearchUpdater {

	private static final Logger logger = LogManager.getLogger(IndexElasticsearchUpdater.class);

	/**
	 * Create a new index in Elasticsearch. Read also _settings.json if exists.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param index Index name
	 * @throws Exception
	 */
	public static void createIndex(Client client, String root, String index) throws Exception {
		String settings = IndexSettingsReader.readSettings(root, index);
		createIndexWithSettings(client, index, settings);
	}

	/**
	 * Create a new index in Elasticsearch. Read also _settings.json if exists in default classpath dir.
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @throws Exception
	 */
	public static void createIndex(Client client, String index) throws Exception {
		String settings = IndexSettingsReader.readSettings(index);
		createIndexWithSettings(client, index, settings);
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no specific settings
	 * @throws Exception
	 */
	public static void createIndexWithSettings(Client client, String index, String settings) throws Exception {
		if (!isIndexExist(client, index)) {
			logger.debug("Index [{}] doesn't exist. Creating it.", index);
			createIndexWithSettingsInElasticsearch(client, index, settings);
		} else {
			logger.debug("Index [{}] already exists.", index);
		}
	}

	/**
	 * Create a new index in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no specific settings
	 * @throws Exception
	 */
	private static void createIndexWithSettingsInElasticsearch(Client client, String index, String settings) throws Exception {
		logger.trace("createIndex([{}])", index);

		assert client != null;
		assert index != null;

		CreateIndexRequestBuilder cirb = client.admin().indices().prepareCreate(index);

		// If there are settings for this index, we use it. If not, using Elasticsearch defaults.
		if (settings != null) {
			logger.trace("Found settings for index [{}]: [{}]", index, settings);
			cirb.setSettings(settings);
		}

		CreateIndexResponse createIndexResponse = cirb.execute().actionGet();
		if (!createIndexResponse.isAcknowledged()) {
			logger.warn("Could not create index [{}]", index);
			throw new Exception("Could not create index ["+index+"].");
		}

		logger.trace("/createIndex([{}])", index);
	}

	/**
	 * Update settings in Elasticsearch
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @param settings Settings if any, null if no update settings
	 * @throws Exception
	 */
	private static void updateIndexWithSettingsInElasticsearch(Client client, String index, String settings) throws Exception {
		logger.trace("updateIndex([{}])", index);

		assert client != null;
		assert index != null;

		if (settings != null) {
			logger.trace("Found update settings for index [{}]: [{}]", index, settings);
			logger.debug("updating settings for index [{}]", index);
			client.admin().indices().prepareUpdateSettings(index).setSettings(settings).get();
		}

		logger.trace("/updateIndex([{}])", index);
	}

	/**
	 * Check if an index already exists
	 * @param index Index name
	 * @return true if index already exists
	 * @throws Exception
	 */
	public static boolean isIndexExist(Client client, String index) throws Exception {
		return client.admin().indices().prepareExists(index).get().isExists();
	}

	/**
	 * Update index settings in Elasticsearch. Read also _update_settings.json if exists.
	 * @param client Elasticsearch client
	 * @param root dir within the classpath
	 * @param index Index name
	 * @throws Exception
	 */
	public static void updateSettings(Client client, String root, String index) throws Exception {
		String settings = IndexSettingsReader.readUpdateSettings(root, index);
		updateIndexWithSettingsInElasticsearch(client, index, settings);
	}

	/**
	 * Update index settings in Elasticsearch. Read also _update_settings.json if exists in default classpath dir.
	 * @param client Elasticsearch client
	 * @param index Index name
	 * @throws Exception
	 */
	public static void updateSettings(Client client, String index) throws Exception {
		String settings = IndexSettingsReader.readUpdateSettings(index);
		updateIndexWithSettingsInElasticsearch(client, index, settings);
	}
}
