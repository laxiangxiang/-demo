package elasticsearchstudyofficially;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * Created by LXX on 2019/3/28.
 */
public class Test {

    private static TransportClient client;

    public static void main(String[] args) throws Exception{
        getTransportClient();
//        store2Index();
//        loadFromIndex();
//        deleteFromIndex();
        updateFromIndex();
    }

    public static void getTransportClient() throws UnknownHostException{
        Settings settings = Settings.builder()//连接elasticsearch集群
                .put("cluster.name","myClusterName")
                .put("client.transport.sniff",true)//开启客户端嗅探功能
                .build();//更多集群配置看官网：https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/transport-client.html

//        client = new PreBuiltTransportClient(Settings.EMPTY)
//                .addTransportAddress(new TransportAddress(InetAddress.getByName("host1"),9300))
//                .addTransportAddress(new TransportAddress(InetAddress.getByName("host2"),9300));
        client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new TransportAddress(InetAddress.getByName("127.0.0.1"),9300));
    }

    public static String getDocument(){
        //直接使用json格式字符串
        String json1 = "{" +
                "\"user\":\"kimchy\"," +
                "\"postDate\":\"2013-01-30\"," +
                "\"message\":\"trying out Elasticsearch\"" +
                "}";
        //使用map
        Map<String,Object> json2 = new HashMap<String, Object>();
        json2.put("user","yang");
        json2.put("postDate",new Date());
        json2.put("message","trying out Elasticsearch");
        //对象序列化，使用jackson
        ObjectMapper mapper = new ObjectMapper();
//        byte[] json3 = mapper.writeValueAsBytes(yourbeaninstance);

        //使用elasticsearch helpers
        XContentBuilder builder = null;
        try {
             builder = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("user","yang")
                    .field("postDate",new Date())
                    .field("message","trying out elasticsearch")
                    .endObject();
        }catch (IOException e){
            e.printStackTrace();
        }
        return json1;
    }

    public static void store2Index() throws IOException{
        //把document对象存入index（可以认为是数据库）：，type（可以认为是表）： 中
        IndexResponse response ;
//        response = client.prepareIndex("lxx","user","1")
//                .setSource(XContentFactory.jsonBuilder()
//                .startObject()
//                .field("user","yang")
//                .field("posrDate",new Date())
//                .field("message","trying out Elasticsearch")
//                .endObject()).get();

        response = client.prepareIndex("official-study","user")
                .setSource(getDocument(), XContentType.JSON)
                .get();
        client.prepareIndex("official-study","user")
                .setSource(new HashMap<String, Object>(){
                    {
                        put("user","yang");
                        put("postDate",new Date());
                        put("message","trying out Elasticsearch");
                    }
                }).get();
        String indexName = response.getIndex();
        String typeName = response.getType();
        String _id = response.getId();
        long _version = response.getVersion();
        RestStatus status = response.status();
        System.out.println("_index:"+indexName+",_type:"+typeName+",_id:"+_id+",_version:"+_version+",status:"+status);
    }

    public static void loadFromIndex(){
        GetResponse response = client.prepareGet("official-study","user","MY8y3WkBq8hiTjQZYThy").get();
        Map<String,Object> source = response.getSource();
        Iterator iSource = source.keySet().iterator();
        while (iSource.hasNext()){
            Object key = iSource.next();
            Object object= source.get(key);
            System.out.println(key+":"+object);
        }
    }

    public static void deleteFromIndex(){
        DeleteResponse response = client.prepareDelete("official-study","user","Mo8y3WkBq8hiTjQZYTid").get();
        System.out.println(response.getId());
        System.out.println(response.getResult().getOp());
        System.out.println("-----------------------------------");
        //基于查询API删除查询出的结果集
        BulkByScrollResponse response1 = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
                .filter(QueryBuilders.matchQuery("name","yang"))
                .source("official-study")
                .get();
        System.out.println(response1.getDeleted());
        System.out.println("-----------------------------------");
            //异步查询并删除
        final CountDownLatch latch = new CountDownLatch(1);
        DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
                .filter(QueryBuilders.matchQuery("_id","MI8x3WkBq8hiTjQZeDjB"))
                .source("official-study")
                .execute(new ActionListener<BulkByScrollResponse>() {
                    public void onResponse(BulkByScrollResponse bulkByScrollResponse) {
                        System.out.println(bulkByScrollResponse.getDeleted());
                        latch.countDown();
                    }

                    public void onFailure(Exception e) {
                        e.printStackTrace();
                    }
                });
        try {
            latch.await();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public static void updateFromIndex() throws IOException,ExecutionException,InterruptedException{
        //use UpdateRequest to update
        //1.use doc
        UpdateRequest updateRequest = new UpdateRequest("official-study","user","NI8_3WkBq8hiTjQZ3Tjq");
        updateRequest.doc(XContentFactory.jsonBuilder()
        .startObject()
        .field("user","yang-1")
        .endObject());
        client.update(updateRequest).get();
        //2.use script
        UpdateRequest updateRequest1 = new UpdateRequest("official-study","user","NI8_3WkBq8hiTjQZ3Tjq");
        updateRequest1.script(new Script("ctx._source.user=\"yang-2\""));
        client.update(updateRequest1).get();

        //use prepareUpdate()
        //1.use script
        client.prepareUpdate("official-study","user","NI8_3WkBq8hiTjQZ3Tjq")
                .setScript(new Script("ctx._source.user=\"yang-3\""))
                .get();
        //2.use doc
        client.prepareUpdate("official-study","user","NI8_3WkBq8hiTjQZ3Tjq")
                .setDoc(XContentFactory.jsonBuilder()
                .startObject()
                .field("user","yang-4")
                .endObject())
                .get();

        //upsert 如果存在_id为NI8_3WkBq8hiTjQZ3Tjq的对象，则更新user字段为yang-6，否则新增indexRequest中的doc
        IndexRequest indexRequest = new IndexRequest("official-study","user","NI8_4WkBq8hiTjQZ3Tjq")
                .source(XContentFactory.jsonBuilder()
                .startObject()
                .field("user","yang-5")
                .field("message","trying insert if id not exist else update")
                .field("postDate",new Date())
                .endObject());
        UpdateRequest updateRequest2 = new UpdateRequest("official-study","user","NI8_4WkBq8hiTjQZ3Tjq")
                .doc(XContentFactory.jsonBuilder()
                .startObject()
                .field("user","yang-6")
                .endObject())
                .upsert(indexRequest);
        client.update(updateRequest2).get();
    }
}
