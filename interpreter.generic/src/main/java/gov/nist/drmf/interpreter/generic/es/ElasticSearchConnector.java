package gov.nist.drmf.interpreter.generic.es;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.generic.macro.MacroBean;
import gov.nist.drmf.interpreter.generic.macro.MacroDefinitionStyleFileParser;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static gov.nist.drmf.interpreter.common.constants.GlobalPaths.PATH_ELASTICSEARCH_INDEX_CONFIG;

/**
 * To avoid complications in the first place, the current interface is entirely implemented
 * synchronously!
 * @author Andre Greiner-Petter
 */
public class ElasticSearchConnector {
    private static final Logger LOG = LogManager.getLogger(ElasticSearchConnector.class.getName());

    private static final String ES_INDEX = "dlmf-macros";

    private RestHighLevelClient client;

    public ElasticSearchConnector(ElasticSearchConfig config) {
        HttpHost httpHost = new HttpHost(config.getHost(), config.getPort(), "http");
        RestClientBuilder builder = RestClient.builder(httpHost);
        client = new RestHighLevelClient(builder);
    }

    public void stop() {
        try {
            this.client.close();
        } catch (IOException e) {
            LOG.error("Cannot close elasticsearch connection", e);
        }
    }

    /**
     * Resets (or creates if not exist) the index
     */
    public void resetOrCreateIndex() throws IOException {
        GetIndexRequest getRequest = new GetIndexRequest(ES_INDEX);
        boolean exists = client.indices().exists(getRequest, RequestOptions.DEFAULT);

        if ( exists ) { // delete first
            LOG.debug("Index exists, delete it first.");
            deleteIndex();
            LOG.debug("Successfully deleted the old index, create it new now.");
        }

        createIndex();
    }

    private void deleteIndex() throws IOException, ElasticsearchException {
        try {
            DeleteIndexRequest delRequest = new DeleteIndexRequest(ES_INDEX);
            AcknowledgedResponse response = client.indices().delete(delRequest, RequestOptions.DEFAULT);
            if ( !response.isAcknowledged() ) {
                throw new IOException("Elasticsearch did not acknowledged the deletion request for index " + ES_INDEX);
            }
        } catch (ElasticsearchException ee) {
            if ( ee.status() == RestStatus.NOT_FOUND ) {
                LOG.warn("Tried to delete a non-existing index from elasticsearch: " + ee.getDetailedMessage());
            } else throw ee;
        }
    }

    private void createIndex() throws IOException {
        String indexConfig = Files.readString(PATH_ELASTICSEARCH_INDEX_CONFIG, StandardCharsets.UTF_8);
        CreateIndexRequest createRequest = new CreateIndexRequest(ES_INDEX);
        createRequest.source(indexConfig, XContentType.JSON);
        CreateIndexResponse response = client.indices().create(createRequest, RequestOptions.DEFAULT);
        if ( !response.isAcknowledged() ) {
            throw new IOException("Elasticsearch did not acknowledge the creation of the new index: "
                    + response.toString());
        }
    }

    public void indexElements(Map<String, MacroBean> macros) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BulkRequest bulkRequest = new BulkRequest(ES_INDEX);

        int counter = 0;
        for ( String macroName : macros.keySet() ) {
            MacroBean macro = macros.get(macroName);
            String macroJson = mapper.writeValueAsString(macro);
            IndexRequest indexRequest = new IndexRequest(); // index defined by bulk request
            indexRequest.id(Integer.toString(counter));
            indexRequest.source(macroJson, XContentType.JSON);
            bulkRequest.add(indexRequest);
            counter++;
        }

        LOG.info("Start indexing " + counter + " elements.");
        BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        if ( response.hasFailures() ) {
            LOG.warn("Not every operation finished successfully. Analyzing errors...");
            int numOfFailures = analyzeBulkErrors(response);
            LOG.warn("Successfully indexed " + (counter-numOfFailures) + " elements.");
        } else {
            LOG.info("Successfully indexed " + counter + " elements.");
        }
    }

    /**
     * Logs all failures and returns the total number of failures.
     * @param response the bulk response that contains failures
     * @return number of failures
     */
    private int analyzeBulkErrors(BulkResponse response) {
        int counter = 0;
        for (BulkItemResponse bulkItemResponse : response) {
            if ( bulkItemResponse.isFailed() ) {
                counter++;
                BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                LOG.warn("Operation " + failure.getId() + " failed: " + failure.getMessage());
            }
        }
        return counter;
    }

    public static void main(String[] args) throws IOException {
        ElasticSearchConnector es = new ElasticSearchConnector(new ElasticSearchConfig());
        es.resetOrCreateIndex();

        MacroDefinitionStyleFileParser macroParser = new MacroDefinitionStyleFileParser();
        String macroDefinitions = Files.readString(GlobalPaths.PATH_SEMANTIC_MACROS_DEFINITIONS);
        macroParser.load(macroDefinitions);
        Map<String, MacroBean> macros = macroParser.getExtractedMacros();
        es.indexElements(macros);

        es.stop();
    }
}
