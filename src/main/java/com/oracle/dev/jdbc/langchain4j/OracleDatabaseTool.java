package com.oracle.dev.jdbc.langchain4j;

import java.util.List;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class OracleDatabaseTool {

  private final ContentRetriever retriever;

  public OracleDatabaseTool(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
    this.retriever = EmbeddingStoreContentRetriever.builder().embeddingStore(embeddingStore)
        .embeddingModel(embeddingModel).maxResults(10).minScore(0.5).build();
  }

  @Tool("Searches the Oracle Database knowledge base for the respective Oracle error code description to answer a tech support agent's question")
  public String searchKnowledgeBase(String query) {
    List<Content> contents = retriever.retrieve(Query.from(query));
    if (contents == null || contents.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (Content content : contents) {
      TextSegment segment = content.textSegment();
      if (segment != null) {
        sb.append(segment.text()).append('\n');
      }
    }
    return sb.toString();
  }
}
