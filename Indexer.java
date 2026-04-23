package javaFinal;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Indexer {

    private final Path indexPath;
    private final Analyzer unigramAnalyzer = new StandardAnalyzer();
    private final Analyzer bigramAnalyzer = new BigramAnalyzer();
    private Map<String, String> redirectMap = new HashMap<>();
    
    public Indexer(String indexDir) {
        this.indexPath = Paths.get(indexDir);
    }

    public void indexCorpus(Path corpusDir) throws Exception {
        Directory dir = FSDirectory.open(indexPath);

        IndexWriterConfig config = new IndexWriterConfig(unigramAnalyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        try (IndexWriter writer = new IndexWriter(dir, config)) {
            Files.walk(corpusDir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        indexMultiPageFile(writer, path);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        }
    }

    /**
     * Each file contains thousands of Wikipedia pages.
     * Each page begins with a title line like: [[BBC]]
     */
    private void indexMultiPageFile(IndexWriter writer, Path file) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {

            String line;
            StringBuilder pageBuffer = new StringBuilder();
            String currentTitle = null;

            while ((line = reader.readLine()) != null) {

                // Detect start of a new page: [[TITLE]]
                if (line.startsWith("[[") && line.endsWith("]]")) {

                    // If we already collected a page, index it
                    if (currentTitle != null && pageBuffer.length() > 0) {
                        indexSinglePage(writer, currentTitle, pageBuffer.toString(), file);
                    }

                    // Start new page
                    currentTitle = line.substring(2, line.length() - 2).trim();
                    pageBuffer.setLength(0); // clear buffer
                } else {
                    // Accumulate page content
                    pageBuffer.append(line).append('\n');
                }
            }

            // Index last page in file
            if (currentTitle != null && pageBuffer.length() > 0) {
                indexSinglePage(writer, currentTitle, pageBuffer.toString(), file);
            }
        }
    }


    
    private String getRedirectTarget(String text) {
    	String lower = text.toLowerCase();

        if (lower.startsWith("#redirect")) {
            int start = text.indexOf("[[");
            int end = text.indexOf("]]");

            if (start != -1 && end != -1 && end > start) {
                return text.substring(start + 2, end).trim();
            }
        }
        return null;
    }
    
    private void indexSinglePage(IndexWriter writer, String title, String text, Path file) throws Exception {

    	// 1. Detect redirect
    	String redirectTarget = getRedirectTarget(text);
	
	    if (redirectTarget != null) {
	        // Store redirect mapping
	        redirectMap.put(title, redirectTarget);
	        return; // Do NOT index redirect pages
	    }
	
	    // 2. Collect all aliases (redirects pointing to this page)
	    List<String> aliases = new ArrayList<>();
	    for (Map.Entry<String, String> e : redirectMap.entrySet()) {
	        if (e.getValue().equals(title)) {
	            aliases.add(e.getKey());
	        }
	    }
	
	    // 3. Build expanded title field
	    StringBuilder expandedTitle = new StringBuilder(title);
	    for (String alias : aliases) {
	        expandedTitle.append(" ").append(alias);
	    }
	    
        // Extract first 3 paragraphs (much better for Jeopardy)
        //String shortText = extractFirstParagraphs(text, 3);
    	String fullText = text;

        Document doc = new Document();

        doc.add(new StoredField("title", title));
     // Store expanded title for searching
        doc.add(new TextField("title_text", expandedTitle.toString(), Field.Store.NO));
        
        doc.add(new StringField("doc_id", file.getFileName().toString(), Field.Store.YES));

        // Unigram field
        doc.add(new TextField("content_unigram", fullText, Field.Store.NO));

        // Bigram field
        FieldType bigramType = new FieldType(TextField.TYPE_NOT_STORED);
        bigramType.setStoreTermVectors(true);
        doc.add(new Field("content_bigram",
                bigramAnalyzer.tokenStream("content_bigram", fullText),
                bigramType));

        writer.addDocument(doc);
    }



    public static void main(String[] args) throws Exception {
    	long start = System.currentTimeMillis();

        Indexer indexer = new Indexer("C:/Users/jessi/lucene-index/wiki_index");
        indexer.indexCorpus(Paths.get("wiki-subset-20140602"));

        long end = System.currentTimeMillis();

        long elapsedMs = end - start;
        double elapsedSec = elapsedMs / 1000.0;
        double elapsedMin = elapsedSec / 60.0;

        System.out.println("Finished Indexing!");
        System.out.println("Time: " + elapsedSec + " seconds");
        System.out.println("Time: " + elapsedMin + " minutes");
    	
    }
}
