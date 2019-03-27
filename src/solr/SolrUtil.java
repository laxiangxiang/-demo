package solr;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;


public class SolrUtil {
    public static SolrClient client;
    private static String url;
    static {
        url = "http://localhost:8983/solr/lxx";
        client = new HttpSolrClient.Builder(url).build();
    }


    public static <T> boolean batchSaveOrUpdate(List<T> entities) throws SolrServerException, IOException {
        DocumentObjectBinder binder = new DocumentObjectBinder();
		int total = entities.size();
		int count=0,per=0,oldPer=0;
        for (T t : entities) {
            SolrInputDocument doc = binder.toSolrInputDocument(t);
            client.add(doc);
            count++;
            per = count*100/total;
            if (per != oldPer){
                oldPer = per;
                System.out.printf("索引中，总共要添加%d条记录，当前添加进度是：%d%% %n",total,per);
            }
		}
        client.commit();
        return true;
    }

    public static <T>boolean saveOrUpdate(T entity)throws SolrServerException,IOException{
        DocumentObjectBinder binder = new DocumentObjectBinder();
        SolrInputDocument document = binder.toSolrInputDocument(entity);
        client.add(document);
        client.commit();
        return true;
    }

    public static boolean deleteById(String id){
        try {
            client.deleteById(id);
            client.commit();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

   public static void query(String keywords,int startOfPage,int numberOfPage) throws SolrServerException,IOException{
       SolrQuery query = new SolrQuery();
       query.setStart(startOfPage);
       query.setRows(numberOfPage);
       query.setQuery(keywords);
       QueryResponse queryResponse = client.query(query);
       SolrDocumentList documents = queryResponse.getResults();
       System.out.println("累计找到的条数："+documents.getNumFound());
       if (!documents.isEmpty()){
           Collection<String> fieldNames = documents.get(0).getFieldNames();
           for (String fieldName : fieldNames){
               System.out.print(fieldName+"\t");
           }
           System.out.println();
       }
       for (SolrDocument solrDocument : documents){
           Collection<String> fieldNames = solrDocument.getFieldNames();
           for (String fieldName : fieldNames){
               System.out.print(solrDocument.get(fieldName)+"\t");
           }
           System.out.println();
       }
   }

    public static void queryHighLight(String keywords) throws SolrServerException,IOException{
        SolrQuery query = new SolrQuery();
        //开始页数
        query.setStart(0);
        //每页显示条数
        query.setRows(10);
        //设置查询关键字
        query.setQuery(keywords);
        //开启高亮
        query.setHighlight(true);
        //高亮字段
        query.addHighlightField("name");
        //高亮单词的前缀
        query.setHighlightSimplePre("<span style='color:red'>");
        //高亮单词的后缀
        query.setHighlightSimplePost("</span>");
        //摘要最长100个字符
        query.setHighlightFragsize(100);
        //查询
        QueryResponse queryResponse = client.query(query);
        //获取高亮字段name相应结果
        NamedList<Object> response = queryResponse.getResponse();
        NamedList<?> highlighting = (NamedList<?>) response.get("highlighting");
        for (int i = 0;i<highlighting.size();i++){
            System.out.println(highlighting.getName(i)+":"+highlighting.getVal(i));
        }
        //获取查询结果
        SolrDocumentList results = queryResponse.getResults();
        for (SolrDocument document : results){
            System.out.println(document.toString());
        }
    }



}