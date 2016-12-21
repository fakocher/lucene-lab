/**
 * This project explores the possibilities of the Lucene indexing framework.
 * 
 * Parameters:
 * 		<analyzer>         : one of the following analyzers:
 * 			- StandardAnalyzer 	      or sda
 * 			- WhitespaceAnalyzer 	  or wa
 * 			- EnglishAnalyzer 		  or ea
 * 			- ShingleAnalyzerWrapper2 or saw2
 * 			- ShingleAnalyzerWrapper3 or saw3
 * 			- StopAnalyzer 			  or spa
 * 		<file path>        : Path of the text file to index
 * 		<query>            : Query to run on the index
 * 		<common words path>: Only if you use StopAnalyzer
 * 
 * Example:
 * 		C:\cacm.txt test
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class LuceneLab
{
	public static void main(String[] args) throws IOException, ParseException
	{
		// Get analyzer from first argument
		if (args.length < 1)
		{
			System.out.println("Missing first argument. Provide an analyser.");
			System.out.println("StandardAnalyzer, WhitespaceAnalyzer, EnglishAnalyzer,");
			System.out.println("ShingleAnalyzerWrapper2, ShingleAnalyzerWrapper3, StopAnalyzer");
			return;
		}
		String analyzerName = args[0];
		
		// Get file path from second argument
		if (args.length < 2)
		{
			System.out.println("Missing second argument. Provide a file path.");
			return;
		}
		String filePath = args[1];

		// Get query from third argument
		if (args.length < 3)
		{
			System.out.println("Missing third argument. Provide a query.");
			return;
		}
		String queryString = args[2];

		// Create an analyzer
		Analyzer analyzer;
		switch (analyzerName)
		{
			case "WhitespaceAnalyzer":
				analyzer = new WhitespaceAnalyzer();  
				break;
				
			case "EnglishAnalyzer":
				analyzer = new EnglishAnalyzer();  
				break;
				
			case "ShingleAnalyzerWrapper2":
				analyzer = new ShingleAnalyzerWrapper(2, 2);
				break;
				
			case "ShingleAnalyzerWrapper3":
				analyzer = new ShingleAnalyzerWrapper(3, 3);
				break;
				
			case "StopAnalyzer":
				if (args.length < 4)
				{
					System.out.println("Missing fourth argument. Provide a common words file path.");
					return;
				}
				File stopWordsFile = new File(args[3]);
				analyzer = new StopAnalyzer();
				break;
				
			case "StandardAnalyzer":
			default:
				analyzer = new StandardAnalyzer();  
				break;
		}
		
		// Create an index writer configuration
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		
		// Create and replace existing index
		iwc.setOpenMode(OpenMode.CREATE);
		
		// Not pack newly written segments in a compound file: 
		// keep all segments of index separately on disk
		iwc.setUseCompoundFile(false);
		
		// Create index writer
		Path indexPath = FileSystems.getDefault().getPath("index");
		Directory indexDir = FSDirectory.open(indexPath);
		IndexWriter indexWriter = new IndexWriter(indexDir, iwc);
		
		// Create reader to read cacm.txt
		BufferedReader in = new BufferedReader(new FileReader(filePath));
		
		// Loop over the text file lines
		String line;
		while((line = in.readLine()) != null)
		{
			// Create new document
			Document doc = new Document();
			
			// Gather the field values from the file line
			String[] fields  = line.split("\t");
			
			// Create an ID field
			int id = Integer.parseInt(fields[0]);
			IntField idField = new IntField("id", id, Field.Store.YES);
			doc.add(idField);
			
			// Create a string field for each author
			String[] authors = fields[1].split(";");
			StringField[] authorFields = new StringField[authors.length];
			for (int i = 0; i < authors.length; i++)
			{
				if (authors[i] != "")
				{
					authorFields[i] = new StringField("author", authors[i], Field.Store.YES);
				}
			}
			for (StringField authorField: authorFields)
			{
				doc.add(authorField);
			}
			
			// Create a string title field
			String title = fields[2];
			FieldType titleFieldType = new FieldType();
			titleFieldType.setIndexOptions(IndexOptions.DOCS); // Controls how much information is stored in the postings lists.
			titleFieldType.setTokenized(true); // Tokenize the field's contents using configured analyzer
			titleFieldType.setStoreTermVectors(true); // Store term vectors
			titleFieldType.setStored(true); // Store the field to show it in the results
			titleFieldType.freeze(); // Prevents future changes
			Field titleField = new Field("title", title, titleFieldType);
			doc.add(titleField);
			
			// Create a summary field if one exists
			if (fields.length > 3)
			{
				String summary = fields[3];
				FieldType summaryFieldType = new FieldType();
				summaryFieldType.setIndexOptions(IndexOptions.DOCS); // Controls how much information is stored in the postings lists.
				summaryFieldType.setTokenized(true); // Tokenize the field's contents using configured analyzer
				summaryFieldType.setStoreTermVectors(true); // Store term vectors
				summaryFieldType.freeze(); // Prevents future changes
				Field summaryField = new Field("summary", summary, summaryFieldType);
				doc.add(summaryField);
			}
			
			// Add document to index
			indexWriter.addDocument(doc);
		}

		// Close file reader
		in.close();
		
		// Close index writer
		indexWriter.close();

		// Create query parser
		String fieldNames[] = {"author", "title", "summary"};
		MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldNames, analyzer);
		
		// Parse query
		Query query = parser.parse(queryString);

		// Create index reader
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		// Create index searcher
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		// Search query
		ScoreDoc[] hits = indexSearcher.search(query, 1000).scoreDocs;
		
		// Retrieve results
		System.out.println("Results found: " + hits.length);
		for (ScoreDoc hit: hits) {
			Document doc = indexSearcher.doc(hit.doc);
			System.out.println(doc.get("id") + ": " + doc.get("title") + " (" + hit.score + ")");
		}
		
		// Close index reader
		indexReader.close();
		indexDir.close();
	}
}
