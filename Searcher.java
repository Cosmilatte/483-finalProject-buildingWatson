package javaFinal;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Searcher {

    private final IndexSearcher searcher;
    private final MultiFieldQueryParser parser;
    Analyzer analyzer = new StandardAnalyzer();

    public Searcher(String indexDir) throws Exception {

        Path indexPath = Paths.get(indexDir);
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
        searcher = new IndexSearcher(reader);

        // Use unigram analyzer for parsing queries
        //Analyzer unigramAnalyzer = new StandardAnalyzer();
        parser = new MultiFieldQueryParser(
                new String[]{"content_unigram", "content_bigram"},
                analyzer);

        /*
        // Boost bigram matches
        String[] fields = { "content_unigram", "content_bigram"};//, "title_text" };
        
        Map<String,Float> boosts = new HashMap<>();
        boosts.put("content_unigram", 1.0f);
        boosts.put("content_bigram", 1.4f);
        //boosts.put("title_text", 2.0f);

        parser = new MultiFieldQueryParser(fields, unigramAnalyzer, boosts);*/

    }

    /*
    public Document searchTopDoc(String clue) throws Exception {
        Query query = parser.parse(clue);
        TopDocs results = searcher.search(query, 1);

        if (results.scoreDocs.length == 0) {
            return null;
        }

        ScoreDoc sd = results.scoreDocs[0];
        //return searcher.storedFields().document(sd.doc);
        return searcher.storedFields().document(sd.doc);
    }*/
    
    public Document searchTopDoc(String clue) throws Exception {

        // Unigram query
        QueryParser uniParser = new QueryParser("content_unigram", analyzer);
        Query unigramQuery = uniParser.parse(clue);

        // Bigram query
        QueryParser biParser = new QueryParser("content_bigram", analyzer);
        Query bigramQuery = new BoostQuery(biParser.parse(clue), 1.3f);

        // Title query (VERY small boost)
        QueryParser titleParser = new QueryParser("title_text", analyzer);
        Query titleQuery = new BoostQuery(titleParser.parse(clue), 0.3f);

        // Combine
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(unigramQuery, BooleanClause.Occur.SHOULD);
        builder.add(bigramQuery, BooleanClause.Occur.SHOULD);
        builder.add(titleQuery, BooleanClause.Occur.SHOULD);

        Query finalQuery = builder.build();

        TopDocs results = searcher.search(finalQuery, 1);

        if (results.scoreDocs.length == 0) {
            return null;
        }

        return searcher.storedFields().document(results.scoreDocs[0].doc);
    }

    public static void main(String[] args) throws Exception {
    	Searcher s = new Searcher("C:/Users/jessi/lucene-index/wiki_index");  // <-- your index path

        try (BufferedReader br = new BufferedReader(new FileReader("questions.txt"))) {

            int count = 0;
            int noneCount = 0;

            String category;

            while ((category = br.readLine()) != null) {

                String clue = br.readLine();     // second line
                clue = clue.replaceAll("[^A-Za-z0-9 ]", " ");
                clue = clue.replaceAll("\\s+", " ").trim();

                String answer = br.readLine();   // third line
                br.readLine();                   // blank line separator

                System.out.println("====================================");
                System.out.println("CATEGORY: " + category);
                System.out.println("CLUE: " + clue);
                System.out.println("ANSWER (gold): " + answer);

                Document top = s.searchTopDoc(clue);

                if (top == null) {
                    System.out.println("TOP DOC: <none>");
                    noneCount++;
                } else {
                    String predicted = top.get("title");
                    System.out.println("TOP DOC: " + predicted);

                    if (predicted != null && predicted.equalsIgnoreCase(answer)) {
                        count++;
                    }
                }
            }

            System.out.println("Answers Matched: " + count);
            System.out.println("No Answers Found: " + noneCount);
        }
    }

}
