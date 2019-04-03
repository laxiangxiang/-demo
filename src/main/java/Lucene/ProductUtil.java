package Lucene;

import org.apache.commons.io.FileUtils;
import Lucene.Product;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LXX on 2019/3/26.
 */
public class ProductUtil {

    public static void main(String[] args) {
        String fileName = "resource"+File.separator+"140k_products.txt";
        List<Product> products = file2list(fileName);
        System.out.println(products.size());
    }

    public static List<Product> file2list(String fileName) {
        File file = new File(fileName);
        List<Product> products = new ArrayList();
        try {
            List<String> lines = FileUtils.readLines(file,"UTF-8");
            for (String line : lines){
                Product product = line2product(line);
                products.add(product);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return products;
    }

    private static Product line2product(String line) {
        Product p = new Product();
        String[] fields = line.split(",");
        p.setId(Integer.parseInt(fields[0]));
        p.setName(fields[1]);
        p.setCategory(fields[2]);
        p.setPrice(Float.parseFloat(fields[3]));
        p.setPlace(fields[4]);
        p.setCode(fields[5]);
        return p;
    }
}
