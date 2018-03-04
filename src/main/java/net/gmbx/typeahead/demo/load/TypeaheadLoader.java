package net.gmbx.typeahead.demo.load;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.node.Node;
import org.elasticsearch.threadpool.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ComponentScan(value = "net.gmbx.typeahead.demo.config")
@EnableAutoConfiguration
public class TypeaheadLoader implements CommandLineRunner {

    private static final int BULK_LOAD_PAGE_SIZE = 1000;

    private static final Logger logger = LoggerFactory.getLogger(TypeaheadLoader.class);

    @Autowired
    private RestClient restClient;

    @Autowired
    private RestHighLevelClient client;

    public static void main(String[] args) {
        SpringApplication.run(TypeaheadLoader.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        if (args.length != 4) {
            System.err.println("Usage: indexName indexType path/to/index/config path/to/data");
            System.err.println("Note: [indexType] needs to match the config mapping (unchecked)");
            System.exit(1);
        }

        final String indexName = args[0];
        final String indexType = args[1];
        final String configName = args[2];
        final String jsonDocuments = args[3];

        if (!createIndex(indexName, configName)) {
            System.err.println(String.format("Error: Unable to create %s index", indexName));
            System.exit(1);
        }

        loadDocuments(indexName, indexType, BULK_LOAD_PAGE_SIZE, jsonDocuments);

        System.exit(0);
    }

    private boolean createIndex(String indexName, String configName) throws IOException, URISyntaxException {
        URL url = this.getClass().getClassLoader().getResource(configName);
        if (url == null) {
            throw new RuntimeException("Unable to locate: " + configName);
        }
        Path pathToConfig = Paths.get(url.toURI());
        String config = new String(Files.readAllBytes(pathToConfig), StandardCharsets.UTF_8);
        logger.info("Creating index [{}]", indexName);
        try {
            HttpEntity entity = new NStringEntity(config, ContentType.APPLICATION_JSON);
            Response response = restClient.performRequest("PUT", "/" + indexName, Collections.emptyMap(), entity);
            logger.trace("create index response: {}", response.toString());
            return true;
        } catch (ResponseException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    private void loadDocuments(String indexName, String indexType, int pageSize, String filename) throws IOException, InterruptedException, URISyntaxException {
        BulkProcessor bulkProcessor = buildBulkProcessor(pageSize);
        URL url = this.getClass().getClassLoader().getResource(filename);
        if (url == null) {
            throw new RuntimeException("Unable to locate: " + filename);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(url.toURI())))) {
            while (reader.ready()) {
                String json = reader.readLine();
                bulkProcessor.add(Requests.indexRequest(indexName).type(indexType).source(json, XContentType.JSON));
            }
        }
        bulkProcessor.awaitClose(3, TimeUnit.MINUTES);
        waitForClusterStatusYellow();
    }

    private void waitForClusterStatusYellow() throws IOException {
        Map<String, String> parameters = Collections.singletonMap("wait_for_status", "yellow");
        Response response = restClient.performRequest("GET", "/_cluster/health", parameters);

        ClusterHealthStatus healthStatus;
        try (InputStream is = response.getEntity().getContent()) {
            Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
            healthStatus = ClusterHealthStatus.fromString((String) map.get("status"));
        }
        logger.info(response.toString());
        if (healthStatus == ClusterHealthStatus.YELLOW) {
            logger.error("ClusterHealthStatus: {}", healthStatus);
        }
    }

    private BulkProcessor buildBulkProcessor(int pageSize) {
        ThreadPool threadPool = new ThreadPool(Settings.builder().put(Node.NODE_NAME_SETTING.getKey(), "high-level-client").build());
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                logger.info(String.format("Bulk execution [%d] completed %d actions in %dms, hasFailures=%b.",
                        executionId, response.getItems().length, response.getTookInMillis(), response.hasFailures()));
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                logger.info(String.format("Bulk execution [%d] failed.\n%s", executionId, failure.toString()));
            }
        };
        return new BulkProcessor.Builder(client::bulkAsync, listener, threadPool)
                .setBulkActions(pageSize)
                .setBulkSize(new ByteSizeValue(5, ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(5))
                .setConcurrentRequests(1)
                .setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 3))
                .build();
    }
}