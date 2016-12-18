import org.apache.lucene.analysis.Analyzer;
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

public class Lucene
{
	public static void main(String[] args) throws IOException, ParseException
	{
		// Get file path from first argument
		if (args[0] == null)
		{
			System.out.println("Missing first argument. Provide a file path.");
		}
		String filePath = args[0];
		
		// Create an analyser
		Analyzer analyzer = new StandardAnalyzer();  
		
		// 1.2. create an index writer configuration
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		
		// create and replace existing index
		iwc.setOpenMode(OpenMode.CREATE);
		
		// not pack newly written segments in a compound file: 
		// keep all segments of index separately on disk
		iwc.setUseCompoundFile(false);
		
		// create index writer
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
			
			// Create a field for each author
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
			
			// Create a title field
			String title = fields[2];
			StringField titleField = new StringField("title", title, Field.Store.YES);
			doc.add(titleField);
			
			// Create a summary field if one exists
			if (fields.length > 3)
			{
				String summary = fields[3];
				StringField summaryField = new StringField("summary", summary, Field.Store.YES);
				doc.add(summaryField);
			}
			
			// 1.7. add document to index
			indexWriter.addDocument(doc);
		}
		
		in.close();
		
		// 1.8 close index writer
		indexWriter.close();

		// 2.1. create query parser
		QueryParser parser = new QueryParser("summary", analyzer);
		
		// 2.2. parse query
		Query query = parser.parse("simultaneous");

		// 3.1. create index reader
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		// 3.2. create index searcher
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		// 3.3. search query
		ScoreDoc[] hits = indexSearcher.search(query, 1000).scoreDocs;
		
		// 3.4. retrieve results
		System.out.println("Results found: " + hits.length);
		for (ScoreDoc hit: hits) {
			Document doc = indexSearcher.doc(hit.doc);
			System.out.println(doc.get("id") + ": " + doc.get("title") + " (" + hit.score + ")");
		}
		
		// 3.5. close index reader
		indexReader.close();
		indexDir.close();
	}
}
