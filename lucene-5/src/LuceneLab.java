/**
 * DataMgmt - Lab 3 - Kim Leng Chhun, Jeremy Mariano
 * =============================================================================
 * 
 * This project explores the possibilities of the Lucene indexing framework.
 * The code follows the order of the lab exercises.
 * 
 * You must provide two parameters :
 * 		<file path>
 * 		<common words path>
 * 
 * Example:
 * 		C:\cacm.txt C:\common_words.txt
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
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.HighFreqTerms.DocFreqComparator;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class LuceneLab
{
	private static String cacmFilePath;
	
	public static void main(String[] args) throws Exception
	{
		/**
		 * Getting the arguments
		 * =====================================================================
		 */
		
		// Get cacm.txt path from first argument
		if (args.length < 1)
		{
			System.out.println("Missing first argument. Provide a cacm.txt file path.");
			
			return;
		}
		cacmFilePath = args[0];

		// Get common_words.txt path from second argument
		if (args.length < 2)
		{
			System.out.println("Missing second argument. Provide a common words file path.");
			
			return;
		}
		Path stopWordsPath = FileSystems.getDefault().getPath(args[1]);
		
		/**
		 * 5.a Indexing
		 * =====================================================================
		 */

		// Create an index with a standard analyzer
		StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
		Path standardIndexPath = createIndex("indexes/standard", standardAnalyzer);

		/**
		 * 5.b Using different Analyzers
		 * =====================================================================
		 */

		// Create an index with a whitespace analyzer
		WhitespaceAnalyzer whitespaceAalyzer = new WhitespaceAnalyzer();
		Path whitespaceIndexPath = createIndex("indexes/whitespace", whitespaceAalyzer);

		// Create an index with an english analyzer
		EnglishAnalyzer	englishAnalyzer = new EnglishAnalyzer();
		Path englishIndexPath = createIndex("indexes/english", englishAnalyzer);

		// Create an index with a shingle analyzer wrapper, size 2
		ShingleAnalyzerWrapper shingleAnalyzerWrapper2 = new ShingleAnalyzerWrapper(2, 2);
		Path shingle2IndexPath = createIndex("indexes/shingle-2", shingleAnalyzerWrapper2);

		// Create an index with a shingle analyzer wrapper, size 3
		ShingleAnalyzerWrapper shingleAnalyzerWrapper3 = new ShingleAnalyzerWrapper(3, 3);
		Path shingle3IndexPath = createIndex("indexes/shingle-3", shingleAnalyzerWrapper3);

		// Create an index with a stop analyzer, size 3
		StopAnalyzer stopAnalyzer = new StopAnalyzer(stopWordsPath);
		Path stopIndexPath = createIndex("indexes/stop", stopAnalyzer);
		
		/**
		 * 5.c Reading Index
		 * =====================================================================
		 */

		// Create index reader
		Directory indexDir = FSDirectory.open(standardIndexPath);
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		// Look for the author with the highest number of publication
		DocFreqComparator comparator = new HighFreqTerms.DocFreqComparator();
		TermStats termStats[] = HighFreqTerms.getHighFreqTerms(indexReader, 1, "author", comparator);
		String authorName = termStats[0].termtext.utf8ToString();
		String numberOfPublications = Integer.toString(termStats[0].docFreq);
		System.out.println("Author with the highest number of publications:");
		System.out.println(authorName + " (" + numberOfPublications + ")");
		
		// List the top 10 terms in the title field with their frequency.
		termStats = HighFreqTerms.getHighFreqTerms(indexReader, 10, "title", comparator);
		System.out.println("\nTop 10 terms in the title field:");
		for (TermStats termStatsItem: termStats)
		{
			String title = termStatsItem.termtext.utf8ToString();
			String frequency = Integer.toString(termStatsItem.docFreq);
			System.out.println(title + " (" + frequency + ")");
		}
		
		// Close index reader
		indexReader.close();
		
		/**
		 * 5.d Searching TODO
		 * =====================================================================
		 */

		// Create query parser
		String fieldNames[] = {"title", "summary"};
		MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldNames, englishAnalyzer);

		// Create index reader
		indexDir = FSDirectory.open(englishIndexPath);
		indexReader = DirectoryReader.open(indexDir);
		
		// Create index searcher
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		// Handle some queries
		handleQuery("\"Information Retrieval\"", parser, indexSearcher);
		handleQuery("Information AND Retrieval", parser, indexSearcher);
		handleQuery("+Retrieval Information NOT Database", parser, indexSearcher);
		handleQuery("Info*", parser, indexSearcher);
		handleQuery("\"Information Retrieval\"~5", parser, indexSearcher);
		
		// Close index reader
		indexReader.close();
		
		/**
		 * 5.e Tuning the Lucene Score TODO
		 * =====================================================================
		 */
	}

	private static Path createIndex(String indexPathName, Analyzer analyzer) throws IOException
	{
		// Check if index does not already exist
		Path indexPath = FileSystems.getDefault().getPath(indexPathName);
		Directory indexDir = FSDirectory.open(indexPath);
		if (!DirectoryReader.indexExists(indexDir))
		{
			// Create an index writer configuration
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			
			// Create and replace existing index
			iwc.setOpenMode(OpenMode.CREATE);
			
			// Not pack newly written segments in a compound file: 
			// keep all segments of index separately on disk
			iwc.setUseCompoundFile(false);
			
			// Create index writer
			IndexWriter indexWriter = new IndexWriter(indexDir, iwc);
			
			// Create reader to read cacm.txt
			BufferedReader in = new BufferedReader(new FileReader(cacmFilePath));
			
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
				StringField[] authorFields = new StringField[authors.length - 1];
				for (int i = 0; i < authors.length - 1; i++)
				{
					authorFields[i] = new StringField("author", authors[i], Field.Store.YES);
				}
				for (StringField authorField: authorFields)
				{
					doc.add(authorField);
				}
				
				// Create a string title field
				String title = fields[2];
				FieldType titleFieldType = new FieldType();
				titleFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS); // Controls how much information is stored in the postings lists.
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
					summaryFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS); // Controls how much information is stored in the postings lists.
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
		}
			
		return indexPath;
	}
	
	private static void handleQuery(String queryString, QueryParser parser, IndexSearcher indexSearcher) throws IOException, ParseException
	{
		// Parse query
		Query query = parser.parse(queryString);
		
		// Search query
		ScoreDoc[] hits = indexSearcher.search(query, 1000).scoreDocs;

		// Print results
		System.out.println("\nQuery: `" + queryString + "` (" + hits.length + " result(s))");
		for (int i = 0; i < 10 && i < hits.length; i++) {
			Document doc = indexSearcher.doc(hits[i].doc);
			System.out.println(doc.get("id") + ": " + doc.get("title") + " (" + hits[i].score + ")");
		}
	}
}
