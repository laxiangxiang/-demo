package Lucene;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by LXX on 2019/3/26.
 */
public class TestLuceneForProduct {

    public static void main(String[] args) throws ParseException,IOException,InvalidTokenOffsetsException{
        //1.准备中文分词器
        IKAnalyzer ikAnalyzer = new IKAnalyzer();
        //2.索引
        Directory index = createIndex(ikAnalyzer);
        //3.查询器
        Scanner s = new Scanner(System.in);
        while (true){
            System.out.println("请输入查询关键字：");
            String keyword = s.nextLine();
            System.out.println("当前关键字是："+keyword);
            Query query = new QueryParser("name",ikAnalyzer).parse(keyword);
            //4.搜索
            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            int pageNow = 1;
            int pageSize = 10;
//            ScoreDoc[] hits = searcher.search(query,pageSize).scoreDocs;
            //////////分页查询///////////////////
//            ScoreDoc[] hits = pageSearch1(query,searcher,pageNow,pageSize);
            ScoreDoc[] hits = pageSearch2(query,searcher,pageNow,pageSize);
            //5.显示查询结果
            showSearchResult(searcher,hits,query,ikAnalyzer);
            //6.关闭查询
            reader.close();
        }
    }

    private static void showSearchResult(IndexSearcher searcher, ScoreDoc[] hits, Query query, IKAnalyzer ikAnalyzer) throws IOException,InvalidTokenOffsetsException{
        System.out.println("找到："+hits.length+"个命中。");
        SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<span style='color:red'>", "</span>");
        Highlighter highlighter = new Highlighter(simpleHTMLFormatter,new QueryScorer(query));
        System.out.println("序号\t匹配度得分\t结果");
        for (int i = 0; i < hits.length ; i++){
            ScoreDoc scoreDoc = hits[i];
            int docId = scoreDoc.doc;
            Document document = searcher.doc(docId);
            List<IndexableField> fields = document.getFields();
            System.out.print(i+1);
            System.out.print("\t"+scoreDoc.score);
            for (IndexableField field : fields){
                if ("name".equals(field)){
                    TokenStream tokenStream = ikAnalyzer.tokenStream(field.name(),new StringReader(document.get(field.name())));
                    String fieldContent = highlighter.getBestFragment(tokenStream,document.get(field.name()));
                }else {
                    System.out.print("\t"+document.get(field.name()));
                }
            }
            System.out.println("<br>");
        }
    }

    private static Directory createIndex(IKAnalyzer ikAnalyzer) throws IOException{
        Directory index = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(ikAnalyzer);
        IndexWriter writer = new IndexWriter(index,config);
        String fileName = "resource"+ File.separator+"140k_products.txt";
        List<Product> products = ProductUtil.file2list(fileName);
        int total = products.size();
        int count = 0,per = 0,oldPer = 0;
        for (Product product : products){
            addDoc(writer,product);
            count++;
            per = count*100/total;
            if (per != oldPer){
                oldPer = per;
                System.out.printf("索引中，总共要添加%d条记录，当前添加进度是：%d%% %n",total,per);
            }
        }
        writer.close();
        return index;

    }

    private static void addDoc(IndexWriter writer, Product product) throws IOException{
        Document document = new Document();
        document.add(new TextField("id",String.valueOf(product.getId()), Field.Store.YES));
        document.add(new TextField("name",product.getName(), Field.Store.YES));
        document.add(new TextField("category", product.getCategory(), Field.Store.YES));
        document.add(new TextField("price",String.valueOf(product.getPrice()), Field.Store.YES));
        document.add(new TextField("place", product.getPlace(), Field.Store.YES));
        document.add(new TextField("code", product.getCode(), Field.Store.YES));
        writer.addDocument(document);
    }

    /**
     * lucene分页查询：方法1
     * 比如要查询第10页，每页10条数据。
     * 把100条数据查出来，然后取最后10条。 优点是快，缺点是对内存消耗大。
     * @return
     */
    private static ScoreDoc[] pageSearch1(Query query,IndexSearcher searcher,int pageNow,int pageSize) throws IOException{
        TopDocs topDocs = searcher.search(query,pageNow*pageSize);
        System.out.println("查询到的总条数\t"+topDocs.totalHits);
        ScoreDoc[] allScores = topDocs.scoreDocs;
        List<ScoreDoc> hitScores = new ArrayList();
        int start = (pageNow -1)*pageSize;
        int end = pageSize*pageNow;
        for (int i = start;i < end;i++){
            hitScores.add(allScores[i]);
        }
        ScoreDoc[] hits = hitScores.toArray(new ScoreDoc[]{});
        return hits;
    }

    /**
     * luncene分页查询：方法2：
     * 比如要查询第10页，每页10条数据。
     * 是把第90条查询出来，然后基于这一条，通过searchAfter方法查询10条数据。
     * 优点是内存消耗小，缺点是比第一种更慢
     * @param query
     * @param searcher
     * @param pageNow
     * @param pageSize
     * @return
     * @throws IOException
     */
    private static ScoreDoc[] pageSearch2(Query query,IndexSearcher searcher,int pageNow,int pageSize) throws IOException{
        int start = (pageNow -1)*pageSize;
        if (0 ==start){
            TopDocs topDocs = searcher.search(query,pageNow+pageSize);
            return topDocs.scoreDocs;
        }
        // 查询数据， 结束页面自前的数据都会查询到，但是只取本页的数据
        TopDocs topDocs = searcher.search(query,start);
        //获取到上一页最后一条
        ScoreDoc scoreDoc = topDocs.scoreDocs[start-1];
        //查询最后一条后的数据的一页数据
        topDocs = searcher.searchAfter(scoreDoc,query,pageSize);
        return topDocs.scoreDocs;
    }

    private static void deletedDocument(Directory directory,IKAnalyzer analyzer) throws IOException{
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(directory,config);
        writer.deleteDocuments(new Term("id","51173"));
        writer.commit();
        writer.close();
    }

    private static void updateDocument(Directory directory,IKAnalyzer analyzer) throws IOException{
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(directory,config);
        Document document = new Document();
        document.add(new TextField("id","51173", Field.Store.YES));
        document.add(new TextField("name", "神鞭，鞭没了，神还在", Field.Store.YES));
        document.add(new TextField("category", "道具", Field.Store.YES));
        document.add(new TextField("price", "998", Field.Store.YES));
        document.add(new TextField("place", "南海群岛", Field.Store.YES));
        document.add(new TextField("code", "888888", Field.Store.YES));
        writer.updateDocument(new Term("id", "51173"), document );
        writer.commit();
        writer.close();
    }
}
