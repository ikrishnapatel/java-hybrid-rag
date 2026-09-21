package com.hybridrag.service;

import org.springframework.ai.document.Document;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class BM25SearchService {

    private final Directory indexDirectory;
    private final StandardAnalyzer analyzer = new StandardAnalyzer();

    public BM25SearchService() {
        try {
            File luceneDir = Paths.get("data/lucene_index").toFile();
            if (!luceneDir.exists()) {
                luceneDir.mkdirs();
            }
            this.indexDirectory = FSDirectory.open(luceneDir.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Lucene BM25 persistent directory", e);
        }
    }

    public synchronized void indexSegments(List<Document> segments) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        config.setSimilarity(new BM25Similarity());

        try (IndexWriter writer = new IndexWriter(indexDirectory, config)) {
            for (int i = 0; i < segments.size(); i++) {
                Document segment = segments.get(i);
                org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
                doc.add(new StringField("id", java.util.UUID.randomUUID().toString(), Field.Store.YES));
                doc.add(new TextField("text", segment.getContent(), Field.Store.YES));
                writer.addDocument(doc);
            }
            writer.commit();
        }
    }

    public List<BM25Result> search(String queryText, int maxResults) throws Exception {
        List<BM25Result> results = new ArrayList<>();
        if (!DirectoryReader.indexExists(indexDirectory)) {
            return results;
        }

        try (DirectoryReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());

            QueryParser parser = new QueryParser("text", analyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);
            
            String cleanQuery = queryText != null ? queryText.trim() : "";
            if (cleanQuery.isEmpty()) {
                return results;
            }

            Query query;
            try {
                query = parser.parse(cleanQuery);
            } catch (Exception parseException) {
                query = parser.parse(QueryParser.escape(cleanQuery));
            }

            TopDocs topDocs = searcher.search(query, maxResults);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                results.add(new BM25Result(
                        doc.get("id"),
                        doc.get("text"),
                        (double) scoreDoc.score
                ));
            }
        }
        return results;
    }

    public static class BM25Result {
        private final String id;
        private final String text;
        private final Double score;

        public BM25Result(String id, String text, Double score) {
            this.id = id;
            this.text = text;
            this.score = score;
        }

        public String getId() { return id; }
        public String getText() { return text; }
        public Double getScore() { return score; }
    }
}
