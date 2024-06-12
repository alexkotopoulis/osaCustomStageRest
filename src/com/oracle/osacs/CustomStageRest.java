package com.oracle.osacs;

import com.oracle.cep.api.event.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.cep.api.annotations.OsaStage;
import com.oracle.cep.api.stage.EventProcessor;
import com.oracle.cep.api.stage.ProcessorContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

class BookResult {
	String isbn;
	String title;
	String publishedDate;
	String publisher;
}

@SuppressWarnings("serial")
@OsaStage(name = "RestBooks", description = "Provide info for a given book", inputSpec = "input, isbn:string", outputSpec = "output, isbn:string, title:string, publishedDate:string, publisher:string")
public class CustomStageRest implements EventProcessor {

	EventFactory eventFactory;
	EventSpec outputSpec;

	static Properties props = new Properties();

	static {
		try {
			props.load(CustomStageRest.class.getResourceAsStream("/CustomStageRest.properties"));
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	@Override
	public void init(ProcessorContext ctx, Map<String, String> config) {
		eventFactory = ctx.getEventFactory();
		OsaStage meta = CustomStageRest.class.getAnnotation(OsaStage.class);
		String spec = meta.outputSpec();
		outputSpec = TupleEventSpec.fromAnnotation(spec);
	}

	@Override
	public void close() {
	}

	@Override
	public Event processEvent(Event event) {
		Attr isbnAttr = event.getAttr("isbn");

		Map<String, Object> values = new HashMap<String, Object>();
		if (!isbnAttr.isNull()) {
			String isbn = (String) isbnAttr.getObjectValue();

			BookResult result = getBook(isbn);

			values.put("isbn", isbn);
			values.put("title", result.title);
			values.put("publishedDate", result.publishedDate);
			values.put("publisher", result.publisher);

		} else {
			values.put("isbn", "");
			values.put("title", "");
			values.put("publishedDate", "");
			values.put("publisher", "");
		}
		Event outputEvent = eventFactory.createEvent(outputSpec, values, event.getTime());
		return outputEvent;
	}

	
	/**
	 * Calls the Google Books REST API to get book information based on the ISBN ID
	 * @param isbn
	 * @return BookResult book information
	 */
	public BookResult getBook(String isbn) {
		HttpRequestBase request;
		BookResult result = null;

		String uri = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

		request = new HttpGet(uri);

		CloseableHttpClient client = HttpClientBuilder.create().build();

		String proxyHost = props.getProperty("proxyHost");
		String proxyPort = props.getProperty("proxyPort");
		if (proxyHost != null && proxyPort != null) {
			int proxyPortInt = Integer.parseInt(proxyPort);
			HttpHost proxy = new HttpHost(proxyHost, proxyPortInt);
			RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
			request.setConfig(config);
		}

		try {
			HttpResponse response = client.execute(request);
			String resultJson = EntityUtils.toString(response.getEntity());
			StatusLine sl = response.getStatusLine();
			int code = sl.getStatusCode();
			if (code < 200 || code >= 300) {
				System.err.println("" + code + " : " + sl.getReasonPhrase());
			}

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readValue(resultJson, JsonNode.class);
			JsonNode bookArray = root.path("items");

			if (bookArray.size() > 0) {
				result = new BookResult();
				JsonNode book = bookArray.path(0).path("volumeInfo"); // We only consider the first book for this ISBN
				result.isbn = isbn;
				result.title = book.path("title").asText();
				result.publishedDate = book.path("publishedDate").asText();
				result.publisher = book.path("publisher").asText();
				return result;
			} else {
				return null; // No book found
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
