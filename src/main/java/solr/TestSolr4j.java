package solr;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class TestSolr4j {

	public static void main(String[] args) throws SolrServerException, IOException {
//		batchSaveOrUpdate();
//        SolrUtil.query("name:手机",0,10);
        SolrUtil.queryHighLight("name:手机");
	}

	private static void batchSaveOrUpdate() throws SolrServerException,IOException{
        List<Product> products = ProductUtil.file2list("resource"+ File.separator+"140k_products.txt");
        SolrUtil.batchSaveOrUpdate(products);
    }
}
