package javaFinal;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
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

    public Searcher(String indexDir) throws Exception {

        Path indexPath = Paths.get(indexDir);
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
        searcher = new IndexSearcher(reader);

        // Use unigram analyzer for parsing queries
        Analyzer unigramAnalyzer = new StandardAnalyzer();

        // Query both fields
        //parser = new MultiFieldQueryParser(
        //        new String[]{"content_unigram", "content_bigram"},
        //        unigramAnalyzer
        //);

        // Boost bigram matches
        String[] fields = { "content_unigram", "content_bigram" };
        Map<String,Float> boosts = new HashMap<>();
        boosts.put("content_unigram", 1.0f);
        boosts.put("content_bigram", 1.4f);

        parser = new MultiFieldQueryParser(fields, unigramAnalyzer, boosts);

    }

    public Document searchTopDoc(String clue) throws Exception {
        Query query = parser.parse(clue);
        TopDocs results = searcher.search(query, 1);

        if (results.scoreDocs.length == 0) {
            return null;
        }

        ScoreDoc sd = results.scoreDocs[0];
        //return searcher.storedFields().document(sd.doc);
        Document d = searcher.storedFields().document(sd.doc);
        
        return d;
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
