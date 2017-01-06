/**
 * DataMgmt - Lab 3 - Kim Leng Chhun, Jeremy Mariano
 * =============================================================================
 * 
 * This project explores the possibilities of the Lucene indexing framework.
 * The code follows the order of the lab exercises.
 * 
 * IMPORTANT NOTE: Indexes are not recreated on each run.
 * 				   You must delete them manually.
 * 
 * You must provide two parameters:
 * 		<cacm.txt path>
 * 		<common_words.txt path>
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
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInvertState;
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
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LuceneLab
{
	/**
	 * Path to the cacm.txt file
	 */
	private static String cacmFilePath;
	
	/**
	 * Path to the query.txt file
	 */
	private static String queryFilePath;
	
	/**
	 * Main function
	 */
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
			System.out.println("Missing second argument. Provide a common_words.txt file path.");
			
			return;
		}
		String stopWordsFilePath = args[1];

		// Get query.txt path from third argument
		if (args.length < 2)
		{
			System.out.println("Missing third argument. Provide a query.txt file path.");
			
			return;
		}
		queryFilePath = args[2];

		// Get qrels.txt path from fourth argument
		if (args.length < 2)
		{
			System.out.println("Missing fourth argument. Provide a qrels.txt file path.");
			
			return;
		}
		Path qRelsPath = FileSystems.getDefault().getPath(args[3]);
		
		/**
		 * I. Indexing
		 * =====================================================================
		 */

		// Create an index with a standard analyzer
		System.out.println("Creating index with standard analyser if it doesn't exist...");
		StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
		Path standardIndexPath = createIndex("indexes/standard", standardAnalyzer);

		// Create an index with a whitespace analyzer
		System.out.println("Creating index with whitespace analyser if it doesn't exist...");
		WhitespaceAnalyzer whitespaceAalyzer = new WhitespaceAnalyzer();
		Path whitespaceIndexPath = createIndex("indexes/whitespace", whitespaceAalyzer);
		
		// Create an index with an english analyzer
		System.out.println("Creating index with english analyser if it doesn't exist...");
		EnglishAnalyzer	englishAnalyzer = new EnglishAnalyzer();
		Path englishIndexPath = createIndex("indexes/english", englishAnalyzer);
		
		// Parsing common_words.txt file
		BufferedReader in = new BufferedReader(new FileReader(stopWordsFilePath));
		String stopWord;
		CharArraySet stopWords = new CharArraySet(0, false);
		while((stopWord = in.readLine()) != null)
		{
			stopWords.add(stopWord);
		}
		
		// Create an index with an english analyzer and custom stop words
		System.out.println("Creating index with english analyser and custom stop words if it doesn't exist...");
		EnglishAnalyzer customEnglishAnalyzer = new EnglishAnalyzer(stopWords);
		Path customEnglishIndexPath = createIndex("indexes/english-custom", customEnglishAnalyzer);
		
		/**
		 * II. Querying
		 * =====================================================================
		 */

		System.out.println("Querying indexes...");
	}

	/**
	 * evaluate the index and print out the metrics
	 * @throws ParseException 
	 * @throws IOException 
	 */
	
	private static void evaluateIndex(Path indexPath, Analyzer analyzer) throws ParseException, IOException
	{
		// Creating parser
		QueryParser parser = new QueryParser("content", analyzer);
		
		// Creating index searcher		
		Directory indexDir = FSDirectory.open(indexPath);
		IndexReader indexReader = DirectoryReader.open(indexDir);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		// Parsing query file
		ArrayList<ScoreDoc[]> hitsList = new ArrayList<ScoreDoc[]>();
		BufferedReader in = new BufferedReader(new FileReader(queryFilePath));
		String line;
		while((line = in.readLine()) != null)
		{
			// If line is not empty
			if (!line.trim().isEmpty())
			{
				// Parsing line
				String[] splitLine = line.split("\t");
				Integer id = Integer.parseInt(splitLine[0]);
				String queryString = QueryParser.escape(splitLine[1]);
				
				// Querying index
				Query query = parser.parse(queryString);
				hitsList.add(indexSearcher.search(query, 1000).scoreDocs);
			}
		}
	}

	/**
	 * Create an index if it does not exist.
	 * You should delete the "indexes" folder if you want to recreate them.
	 */
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
			
			// Create a custom field for title and content
			FieldType customFieldType = new FieldType();
			customFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS); // Controls how much information is stored in the postings lists.
			customFieldType.setTokenized(true); // Tokenize the field's contents using configured analyzer
			customFieldType.setStoreTermVectors(true); // Store term vectors
			customFieldType.setStoreTermVectorPositions(true);
			customFieldType.setStoreTermVectorOffsets(true);
			customFieldType.setStored(true); // Store the field to show it in the results
			customFieldType.freeze(); // Prevents future changes
			
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
				
				// Index the ID
				int id = Integer.parseInt(fields[0]);
				IntField idField = new IntField("id", id, Field.Store.YES);
				doc.add(idField);
				
				// Indexing title
				String title = fields[2];
				Field titleField = new Field("content", title, customFieldType);
				doc.add(titleField);
				
				// Create a summary field if one exists
				if (fields.length > 3)
				{
					String summary = fields[3];
					Field summaryField = new Field("content", summary, customFieldType);
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
}
