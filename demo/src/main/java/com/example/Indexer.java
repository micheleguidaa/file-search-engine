package com.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Indexer {

    public static void main(String[] args) throws IOException {
        // Percorso dei file da indicizzare
        String docsPath = "files";
        String indexPath = "index";

        File folder = new File(docsPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files == null || files.length == 0) {
            System.out.println("Nessun file .txt trovato in " + docsPath);
            return;
        }

        // Analyzer per ogni campo
        CharArraySet stopWords = new CharArraySet(Arrays.asList("in", "dei", "di"), true);
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put("filename", new KeywordAnalyzer());
        perFieldAnalyzers.put("content", new StandardAnalyzer(stopWords));

        Analyzer perFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), perFieldAnalyzers);

        Directory directory = FSDirectory.open(Paths.get(indexPath));
        IndexWriterConfig config = new IndexWriterConfig(perFieldAnalyzer);
        IndexWriter writer = new IndexWriter(directory, config);

        for (File file : files) {
            Document doc = new Document();

            // Campo: nome file
            doc.add(new StringField("filename", file.getName(), Field.Store.YES));

            // Campo: contenuto file
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            doc.add(new TextField("content", sb.toString(), Field.Store.YES));

            writer.addDocument(doc);
            System.out.println("Indicizzato: " + file.getName());
        }

        writer.commit();
        writer.close();
        directory.close();

        System.out.println("Indicizzazione completata. Indice salvato in: " + indexPath);
    }
}