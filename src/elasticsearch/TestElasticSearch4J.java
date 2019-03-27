package elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by LXX on 2019/3/26.
 */
public class TestElasticSearch4J {

    private static RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(new HttpHost("localhost",9200,"http"))
    );

    private static String indexName = "lxx";

    public static void main(String[] args) throws IOException{

        if (!checkExistIndex(indexName)){
            createIndex(indexName);
        }else {
//            deleteIndex(indexName);
        }

        Product product = new Product();
        product.setId(1);
        product.setName("product 1");
        //增加文档
        addDocument(product);
        //获取文档
        getDocument(1);
        product.setName("product 1 copy");
        //修改文档
        updateDocument(product);
        getDocument(1);
        //删除文档
        deleteDocument(1);
        getDocument(1);

        //批量操作
        //14万条数据准备
        List<Product> products = ProductUtil.file2list("resource"+ File.separator+"140k_products.txt");
        System.out.println("准备数据，总计："+products.size()+"条");
        batchInsert(products);

        String keyword = "时尚连衣裙";
        int start = 0;
        int count = 10;
        //查询
        search(keyword,start,count);
        client.close();
    }

    private static void search(String keyword, int start, int count) throws IOException{
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //关键字匹配
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("name",keyword);
        //模糊匹配
        matchQueryBuilder.fuzziness(Fuzziness.AUTO);
        sourceBuilder.query(matchQueryBuilder);
        //第几页
        sourceBuilder.from(start);
        //第几条
        sourceBuilder.size(count);
        //匹配度从高到低
        sourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
        searchRequest.source(sourceBuilder);
        SearchResponse response = client.search(searchRequest);
        SearchHits hits = response.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit:searchHits){
            System.out.println(hit.getSourceAsString());
        }
    }

    private static void batchInsert(List<Product> products) throws IOException{
        BulkRequest request = new BulkRequest();
        for (Product product:products){
            Map<String,Object> map = product.toMap();
            IndexRequest indexRequest = new IndexRequest(indexName,"product",String.valueOf(product.getId())).source(map);
            request.add(indexRequest);
        }
        client.bulk(request);
    }

    //////////////////////////////文档管理/////////////////////////////////
    private static void deleteDocument(int id) throws IOException{
        DeleteRequest deleteRequest = new DeleteRequest(indexName,"product",String.valueOf(id));
        client.delete(deleteRequest);
        System.out.println("已经从ElasticSearch服务器上删除id="+id+"的文档");
    }

    private static void updateDocument(Product product) throws IOException{
        UpdateRequest updateRequest = new UpdateRequest(indexName,"product",String.valueOf(product.getId())).doc("name",product.getName());
        client.update(updateRequest);
        System.out.println("已经在ElasticSearch服务器修改产品为："+product);
    }

    private static void getDocument(int id) throws IOException{
        GetRequest getRequest = new GetRequest(indexName,"product",String.valueOf(id));
        GetResponse response = client.get(getRequest);
        if (!response.isExists()){
            System.out.println("检查到服务器上 "+"id="+id+ "的文档不存在");
        }else {
            String source = response.getSourceAsString();
            System.out.println("获取到服务器上 "+"id="+id+ "的文档内容是：");
            System.out.println(source);
        }
    }

    private static void addDocument(Product product) throws IOException{
        Map<String,Object> jsonMap = new HashMap<>();
        jsonMap.put("name",product.getName());
        IndexRequest indexRequest = new IndexRequest(indexName,"product",String.valueOf(product.getId())).source(jsonMap);
        client.index(indexRequest);
        System.out.println("已经向ElasticSearch服务器增加产品："+product);
    }

    /////////////////////////////索引管理////////////////////////////////////////////
    private static void deleteIndex(String indexName) throws IOException{
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        client.indices().delete(request);
        System.out.println("删除了索引："+indexName);
    }

    private static void createIndex(String indexName) throws IOException{
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        client.indices().create(request);
        System.out.println("创建了索引："+indexName);

    }

    /**
     * 通过 RestHighLevelClient 查看索引是否存在在当前 java api版本(6.2) 里是不提供了，使用的是通过捕捉异常来判断是否存在。
     * @param indexName
     * @return
     * @throws IOException
     */
    private static boolean checkExistIndex(String indexName) throws IOException{
        boolean result = true;
        try {
            OpenIndexRequest openIndexRequest = new OpenIndexRequest(indexName);
            client.indices().open(openIndexRequest).isAcknowledged();
        }catch (ElasticsearchStatusException e){
            String m = "Elasticsearch exception [type=index_not_found_exception, reason=no such index]";
            if (m.equals(e.getMessage())){
                result = false;
            }
        }
        if (result){
            System.out.println("索引："+indexName+" 存在");
        }else {
            System.out.println("索引："+indexName+" 不存在");
        }
        return result;
    }

}
