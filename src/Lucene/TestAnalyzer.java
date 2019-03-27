package Lucene;

import org.apache.lucene.analysis.TokenStream;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.IOException;

/**
 * Created by LXX on 2019/3/26.
 * 分词器
 * 如果使用like,那么%护眼带光源%，匹配出来的结果就是要么全匹配，要不都不匹配。
 * 而使用分词器，就会把这个关键字分为 护眼，带，光源 3个关键字，这样就可以找到不同相关程度的结果了。
 */
public class TestAnalyzer {

    public static void main(String[] args) throws IOException{
        IKAnalyzer analyzer = new IKAnalyzer();
        TokenStream ts = analyzer.tokenStream("name","护眼带光源");
        ts.reset();
        while (ts.incrementToken()){
            System.out.println(ts.reflectAsString(false));
        }
    }
}
