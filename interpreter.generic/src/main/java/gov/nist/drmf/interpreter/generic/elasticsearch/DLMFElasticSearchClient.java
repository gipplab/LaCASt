package gov.nist.drmf.interpreter.generic.elasticsearch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gov.nist.drmf.interpreter.common.config.ElasticSearchConfig;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.generic.macro.MacroBean;
import gov.nist.drmf.interpreter.generic.macro.MacroDefinitionStyleFileParser;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.Map;

import static gov.nist.drmf.interpreter.common.constants.GlobalPaths.PATH_ELASTICSEARCH_INDEX_CONFIG;

/**
 * To avoid complications in the first place, the current interface is entirely implemented
 * synchronously!
 * @author Andre Greiner-Petter
 */
public class DLMFElasticSearchClient {
    private static final Logger LOG = LogManager.getLogger(DLMFElasticSearchClient.class.getName());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    private final RestHighLevelClient client;

    private final String index;

    public DLMFElasticSearchClient() {
        this(new ElasticSearchConfig());
    }

    public DLMFElasticSearchClient(ElasticSearchConfig config) {
        HttpHost httpHost = new HttpHost(config.getHost(), config.getPort(), "http");
        RestClientBuilder builder = RestClient.builder(httpHost);
        client = new RestHighLevelClient(builder);
        index = config.getIndex();
    }

    /**
     * Stops the client.
     * After calling, you must create another client connection if needed.
     */
    public void stop() {
        try {
            this.client.close();
        } catch (IOException e) {
            LOG.error("Cannot close elasticsearch connection", e);
        }
    }

    public LinkedList<MacroResult> searchMacroDescription(String description, int max) throws IOException {
        SearchRequest searchRequest = buildSearchRequest(description, max);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        LinkedList<MacroResult> results = new LinkedList<>();
        for ( SearchHit hit : searchResponse.getHits() ) {
            MacroBean res = OBJECT_MAPPER.readValue(hit.getSourceAsString(), MacroBean.class);
            results.addLast(new MacroResult(hit.getScore(), res));
        }
        return results;
    }

    private SearchRequest buildSearchRequest(String description, int size) {
        MatchQueryBuilder matchQB = QueryBuilders.matchQuery("meta.description", description);

        // score mode avg is default, but it should not make any difference because every doc only
        // has one meta.description object
        NestedQueryBuilder nestedQB = QueryBuilders.nestedQuery("meta", matchQB, ScoreMode.Avg);

        SearchSourceBuilder sb = new SearchSourceBuilder();
        sb.query(nestedQB);
        if ( size > 0 ) sb.size(size);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(index);
        searchRequest.source(sb);
        return searchRequest;
    }

    /**
     * Resets (or creates if not exist) the index
     */
    public void resetOrCreateIndex() throws IOException {
        GetIndexRequest getRequest = new GetIndexRequest(index);
        boolean exists = client.indices().exists(getRequest, RequestOptions.DEFAULT);

        if ( exists ) { // delete first
            LOG.debug("Index exists, delete it first.");
            deleteIndex();
            LOG.debug("Successfully deleted the old index, create it new now.");
        }

        createIndex();
    }

    /**
     * @return true if a new index was generated, otherwise false
     * @throws IOException if elasticsearch is unable to read database
     */
    private boolean createIfNotExist() throws IOException {
        GetIndexRequest getRequest = new GetIndexRequest(index);
        boolean exists = client.indices().exists(getRequest, RequestOptions.DEFAULT);

        if ( !exists ) {
            LOG.info(index + " does not exist in elasticsearch database. Generate it now!");
            createIndex();
            return true;
        } return false;
    }

    /**
     * It only triggers the actual indexing process if the database does not exist already.
     * @throws IOException if something went wrong
     */
    public void indexDLMFDatabaseIfNotExist() throws IOException {
        if ( createIfNotExist() ) {
            reIndexDLMFDatabase();
        }
    }

    private void reIndexDLMFDatabase() throws IOException {
        MacroDefinitionStyleFileParser macroParser = new MacroDefinitionStyleFileParser();
        String macroDefinitions = Files.readString(GlobalPaths.PATH_SEMANTIC_MACROS_DEFINITIONS);
        macroParser.load(macroDefinitions);
        Map<String, MacroBean> macros = macroParser.getExtractedMacros();
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String ser = mapper.writeValueAsString(macros);
        Files.writeString(GlobalPaths.PATH_MACROS_REPLACEMENT_PATTERNS, ser);
        indexElements(macros);
    }

    private void deleteIndex() throws IOException, ElasticsearchException {
        try {
            DeleteIndexRequest delRequest = new DeleteIndexRequest(index);
            AcknowledgedResponse response = client.indices().delete(delRequest, RequestOptions.DEFAULT);
            if ( !response.isAcknowledged() ) {
                throw new IOException("Elasticsearch did not acknowledged the deletion request for index " + index);
            }
        } catch (ElasticsearchException ee) {
            if ( ee.status() == RestStatus.NOT_FOUND ) {
                LOG.warn("Tried to delete a non-existing index from elasticsearch: " + ee.getDetailedMessage());
            } else throw ee;
        }
    }

    private void createIndex() throws IOException {
        String indexConfig = Files.readString(PATH_ELASTICSEARCH_INDEX_CONFIG, StandardCharsets.UTF_8);
        CreateIndexRequest createRequest = new CreateIndexRequest(index);
        createRequest.source(indexConfig, XContentType.JSON);
        CreateIndexResponse response = client.indices().create(createRequest, RequestOptions.DEFAULT);
        if ( !response.isAcknowledged() ) {
            throw new IOException("Elasticsearch did not acknowledge the creation of the new index: "
                    + response.toString());
        }
    }

    private void indexElements(Map<String, MacroBean> macros) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BulkRequest bulkRequest = new BulkRequest(index);
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);

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

    public static boolean isEsAvailable() {
        try {
            DLMFElasticSearchClient connector = new DLMFElasticSearchClient(new ElasticSearchConfig());
            boolean pingResult = connector.client.ping(RequestOptions.DEFAULT);
            if ( pingResult ) {
                LOG.info("Elasticsearch is available. Update DB if necessary.");
                connector.indexDLMFDatabaseIfNotExist();
            }
            connector.stop();
            return pingResult;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        DLMFElasticSearchClient es = new DLMFElasticSearchClient();
        es.resetOrCreateIndex();
        es.reIndexDLMFDatabase();
        es.stop();
    }
}
