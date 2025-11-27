package com.oracle.dev.jdbc.langchain4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.oracle.CreateOption;
import dev.langchain4j.store.embedding.oracle.OracleEmbeddingStore;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class OracleRagAgent {

  public static void main(String[] args) throws SQLException {

    DataSource dataSource = OracleDbUtils.getPooledDataSource();

    // Chat model
    ChatModel chatModel = OpenAiChatModel.builder().apiKey(System.getenv("OPENAI_API_KEY")).modelName(GPT_4_O_MINI)
        .build();

    // Embedding store (Oracle DB as vector store)
    EmbeddingModel embeddingModel = new dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel();
    EmbeddingStore<TextSegment> embeddingStore = OracleEmbeddingStore.builder().dataSource(dataSource)
        .embeddingTable("rag_agent_embeddings", CreateOption.CREATE_IF_NOT_EXISTS).build();

    // Index a few sample documents (simple demo ingestion)
    addErrorCodes(embeddingModel, embeddingStore);

    // Create a knowledge-base tool over the Oracle embedding store
    OracleDatabaseTool kbTool = new OracleDatabaseTool(embeddingStore, embeddingModel);

    // Build an Agentic service that uses the Oracle Database tool
    OracleRag techSupportAgent = AgenticServices.agentBuilder(OracleRag.class).chatModel(chatModel)
        .outputName("errorCodeDescription").tools(kbTool).build();

    // Ask a question. The tech support agent will use the Oracle Database tool to
    // retrieve context
    String errorCode = "ORA-00060";
    String answer = techSupportAgent.getErrorCodeDescription(errorCode);

    System.out.println("Query: What is the error code description for ORA-00060?");
    System.out.println("Response: " + answer + "\n");

    errorCode = "ORA-28018";
    answer = techSupportAgent.getErrorCodeDescription(errorCode);

    System.out.println("Query: What is the error code description for ORA-28018?");
    System.out.println("Response: " + answer + "\n");

  }

  public static void addErrorCodes(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
    List<Document> documents = new ArrayList<>();

    try (var inputStream = OracleRagAgent.class.getClassLoader()
        .getResourceAsStream("oracle-error-codes-descriptions.txt")) {
      if (inputStream == null) {
        throw new IllegalStateException("Resource oracle-error-codes-descriptions.txt not found on classpath. "
            + "Make sure it is located under src/main/resources/");
      }

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        documents = reader.lines().filter(line -> !line.trim().isEmpty()).map(Document::from)
            .collect(Collectors.toList());
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to load oracle-error-codes-descriptions.txt from resources", e);
    }

    if (embeddingModel != null && embeddingStore != null) {
      for (Document doc : documents) {
        Embedding embedding = embeddingModel.embed(doc.text()).content();
        embeddingStore.add(embedding, TextSegment.from(doc.text()));
      }
    }
    System.out.println("Loaded " + documents.size() + " Oracle error codes into the embedding store.\n");
  }

}
