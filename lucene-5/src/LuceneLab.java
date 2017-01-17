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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
	 * Path to the qrels.txt file
	 */
	private static String qrelsFilePath;
	
	/**
	 * Relevant docs for each query
	 */
	private static List<Integer>[] relevantDocs;
	
	/** 
	 * Number of queries
	 */
	private static final int NB_QUERIES = 64;
	
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
		qrelsFilePath = args[3];
		
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
		WhitespaceAnalyzer whitespaceAnalyzer = new WhitespaceAnalyzer();
		Path whitespaceIndexPath = createIndex("indexes/whitespace", whitespaceAnalyzer);
		
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

		// Getting a hits list for each query for each analyzer
		System.out.println("Querying indexes...");
		System.out.println();
		ArrayList<ScoreDoc[]> standardHitsList = queryIndex(standardIndexPath, standardAnalyzer);
		ArrayList<ScoreDoc[]> whitespaceHitsList = queryIndex(whitespaceIndexPath, whitespaceAnalyzer);
		ArrayList<ScoreDoc[]> englishHitsList = queryIndex(englishIndexPath, englishAnalyzer);
		ArrayList<ScoreDoc[]> customEnglishHitsList = queryIndex(customEnglishIndexPath, customEnglishAnalyzer);
		
		/**
		 * III. Evaluation
		 * =====================================================================
		 */
		
		/**
		 * 1) Summary statistics
		 * ---------------------
		 */
		System.out.println("1) Summary statistics");
		System.out.println("---------------------");
		System.out.println();
		
		/**
		 * a. total number of documents
		 */

		System.out.println("a. total number of documents");
		System.out.println();
		
		int totalNbDocs = countDocs(standardIndexPath);
		
		System.out.println(totalNbDocs);
		System.out.println();
		
		/**
		 * b. total number retrieved documents for all queries 
		 */
		
		// Creating relevant array from file
		createRelevantDocsArray();
		
		// Computing statistics for all indexes
		int[][] standardStatistics = computeStatistics(standardHitsList);
		int[][] whitespaceStatistics = computeStatistics(whitespaceHitsList);
		int[][] englishStatistics = computeStatistics(englishHitsList);
		int[][] customEnglishStatistics = computeStatistics(customEnglishHitsList);

		System.out.println("b. total number retrieved documents for all queries");
		System.out.println();
		
		printStats(standardStatistics, 0, "standard");
		printStats(whitespaceStatistics, 0, "whitespace");
		printStats(englishStatistics, 0, "english");
		printStats(customEnglishStatistics, 0, "custom english");

		System.out.println();
		
		/**
		 * c. total number of relevant documents for all queries 
		 */

		System.out.println("c. total number of relevant documents for all queries");
		System.out.println();

		// Print relevant docs
		for (int i = 0; i < relevantDocs.length; i++)
		{
			if (relevantDocs[i] == null)
			{
				System.out.println((i + 1) + ": 0");
			}
			else
			{
				System.out.println((i + 1) + ": " + relevantDocs[i].size());
			}
		}
		
		System.out.println();
		
		/**
		 * d. total number of relevant documents retrieved for all queries. 
		 */

		System.out.println("d. total number of relevant documents retrieved for all queries");
		System.out.println();
		
		printStats(standardStatistics, 1, "standard");
		printStats(whitespaceStatistics, 1, "whitespace");
		printStats(englishStatistics, 1, "english");
		printStats(customEnglishStatistics, 1, "custom english");
		
		
		/**
		 * 2) Average Precision at Standard Recall Levels
		 * ----------------------------------------------
		 */
		
		// for each index, compute the average precision at standard recall levels
		// and generate a CSV file for plotting use
		
		System.out.println("2) Average Precision at Standard Recall Levels");
		System.out.println();
		System.out.println("Writing CSV file...");
		
		PrintWriter csvWriter = new PrintWriter(new File("precision-recall.csv"));
		
		csvWriter.print("");
		
		computePrecision("standard", standardStatistics, standardHitsList, csvWriter);
		computePrecision("whitespace", whitespaceStatistics, whitespaceHitsList, csvWriter);
		computePrecision("english", englishStatistics, englishHitsList, csvWriter);
		computePrecision("custom-english", customEnglishStatistics, customEnglishHitsList, csvWriter);
		
		csvWriter.close();
		
		System.out.println("DONE! Check CSV files.");
	}

	/**
	 * evaluate the index and print out the metrics
	 */
	private static ArrayList<ScoreDoc[]> queryIndex(Path indexPath, Analyzer analyzer) throws ParseException, IOException
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
				int id = Integer.parseInt(splitLine[0]);
				String queryString = QueryParser.escape(splitLine[1]);
				
				// Querying index
				Query query = parser.parse(queryString);
				ScoreDoc[] hits = indexSearcher.search(query, 10000).scoreDocs;
				hitsList.add(hits);
			}
		}
		
		// Closing index reader
		indexReader.close();
		
		return hitsList;
	}
	
	/**
	 * Create an array with the number of relevant documents for each query
	 * based on the qrels.txt file.
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	private static void createRelevantDocsArray() throws NumberFormatException, IOException
	{
		relevantDocs = new ArrayList[NB_QUERIES];
		BufferedReader in = new BufferedReader(new FileReader(qrelsFilePath));
		
		// Looping over the queries
		String line;
		while((line = in.readLine()) != null)
		{
			// Parsing line
			String[] splitLine = line.split(";");
			int id = Integer.parseInt(splitLine[0]);
			String[] relDocs = splitLine[1].split(",");
			
			relevantDocs[id - 1] = new ArrayList<Integer>();
			
			for (int i = 0; i < relDocs.length; i++)
			{
				relevantDocs[id - 1].add(Integer.parseInt(relDocs[i]));
			}
		}
	}
	
	/**
	 * Compute statistics with a hit list.
	 * for each query: total retrieved, total relevant retrieved
	 */
	private static int[][] computeStatistics(ArrayList<ScoreDoc[]> hitsList)
	{
 		int[][] statistics = new int[NB_QUERIES][2];
		
		// Loop over queries
		for (int i = 0; i < NB_QUERIES; i++)
		{
			// Get total retrieved
			statistics[i][0] = hitsList.get(i).length;

			// Compute total relevant retrieved
			statistics[i][1] = 0;
			
			if (relevantDocs[i] != null)
			{
				for (int j = 0; j < hitsList.get(i).length; j++)
				{
					// Check if the document retrieved is relevant
					if (relevantDocs[i].contains(hitsList.get(i)[j].doc))
					{
						statistics[i][1] += 1;
					}
				}
			}
		}
		
		return statistics;
	}
	
	/**
	 * Compute the average precision at standard recall levels
	 * and generate a CSV file for plotting use
	 */
	private static void computePrecision(String analyzerName, int[][] stats, ArrayList<ScoreDoc[]> hitsList, PrintWriter csvWriter) throws IOException
	{
		float[][] precisionRecall = new float[NB_QUERIES][11];
		float[] precisionRecallAverage = new float[11];
		
		// Loop over queries to compute precision
		for (int queryIndex = 0; queryIndex < NB_QUERIES; queryIndex++)
		{
			// If there is no relevant document, the precision is always 0.
			if (stats[queryIndex][1] != 0)
			{
				int recallIndex = 0;
				int relevantRetrieved = 0;
				float recall = 0;
						
				// Calculate recall increment
				double recallIncrement = 1.0 / ((double) stats[queryIndex][1]);
						
				// Loop over the retrieved docs
				for (int hitsIndex = 0; hitsIndex < hitsList.get(queryIndex).length; hitsIndex++)
				{
					// Check if the document retrieved is relevant
					if (relevantDocs[queryIndex].contains(hitsList.get(queryIndex)[hitsIndex].doc))
					{
						relevantRetrieved++;
						recall += recallIncrement;
						
						float precision = ((float) relevantRetrieved) / ((float) (hitsIndex + 1));
						
						while (recallIndex / 10.0 <= recall)
						{
							precisionRecall[queryIndex][recallIndex] = precision;
							
						    recallIndex += 1;
						}
					}
				}
			}
		}

		// Prepare CSV file
        StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(analyzerName);
		stringBuilder.append(",");
		DecimalFormat format = new DecimalFormat("##.0000");
		
		// Loop over recall levels to compute average precision
		// Also write the CSV file
		for (int recallIndex = 0; recallIndex < 11; recallIndex++)
		{
			float precisionSum = 0;

			for (int queryIndex = 0; queryIndex < NB_QUERIES; queryIndex++)
			{
				precisionSum += precisionRecall[queryIndex][recallIndex];
			}
			
			precisionRecallAverage[recallIndex] = precisionSum / NB_QUERIES;

			stringBuilder.append(format.format(precisionRecallAverage[recallIndex]));
			stringBuilder.append(",");
		}

		csvWriter.write(stringBuilder.toString());
		csvWriter.write("\n");
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
	
	private static int countDocs(Path indexPath) throws IOException
	{
		// Creating index reader		
		Directory indexDir = FSDirectory.open(indexPath);
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		// Get total number of docs
		int totalNbDocs = indexReader.numDocs();
		
		// Closing index reader
		indexReader.close();
		
		return totalNbDocs;
	}

	/**
	 * Print given stats
	 */
	private static void printStats(int[][] stats, int index, String analyzerName)
	{
		System.out.println("With " + analyzerName + " analyzer");
		System.out.println();
		
		for (int i = 0; i < stats.length; i++)
		{
			System.out.println((i + 1) + ": " + stats[i][index]);
		}
		
		System.out.println();
	}
}
