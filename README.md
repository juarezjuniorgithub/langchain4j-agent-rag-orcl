# langchain4j-agent-rag-orcl
[The LangChain4J Agentic AI API — Creating RAG Agents with Oracle AI Vector Search, OracleEmbeddingStore, and the Oracle Database 23ai](https://juarezjunior.medium.com/the-langchain4j-agentic-ai-api-creating-rag-agents-with-oracle-ai-vector-search-e11846fc266d)

---

# The LangChain4J Agentic AI API — Creating RAG Agents with Oracle AI Vector Search, OracleEmbeddingStore, and the Oracle Database 23ai

### Introduction

In this post, we’ll explore how to build an Agentic RAG (Retrieval-Augmented Generation) application with LangChain4j that uses the [Oracle Database 23ai](https://www.oracle.com) as a vector store.

Our LangChain4J RAG agent will act as a technical support agent that provides descriptions for [Oracle Database Error Messages (ORA Index)](https://docs.oracle.com) by searching a simple knowledge base stored in the Oracle Database 23ai as vector embeddings.

In a nutshell, our technical support agentic scenario will present:

- How to use the `dev.langchain4j.agentic.AgenticServices` class to create agents with LangChain4J and its Agents and Agentic AI API.
- How to use the `@Tool` annotation (`dev.langchain4j.agent.tool.Tool`) to expose database search functionality as a callable agent function.
- Use the **OracleEmbeddingStore** and the Oracle Database 23ai to store vector embeddings, and then retrieve them using a content retriever, as I already demonstrated in a previous blog post, but using a slightly different strategy as I split the ingestion and retrieval operations between the `OracleRagAgent.java` class (the RAG agent class) and the `OracleDatabaseTool.java` class (the tool class).
- Also note that in comparison with the sample code I created such a previous blog post, another difference is that now I’m using the `AgenticServices` class instead of the AgenticServices one, so once again, note that I’m using the Agents and Agentic AI API.

Given that, primarily the components used in this blog post belong to the [langchain4j-agentic](https://mvnrepository.com/artifact/dev.langchain4j/langchain4j-agentic) module available in LangChain4J, so they relate to its Agents and Agentic AI API — some examples are the `AgenticServices` class (`dev.langchain4j.agentic.AgenticServices`) and the `@Tool` annotation (`dev.langchain4j.agent.tool.Tool`).

Interesting to mention, I did not find a public/published Javadoc for the `langchain4j-agentic` module used in this blog post, so many references were left in italic to highlight that. Otherwise, I’d have included the links to allow you to easily check the documentation as you read my blog post, as I always do in all my blog posts.

Besides, it can be the case that breaking changes may happen, if so (remember this is about open-source software). I did my best to create a simple scenario that will allow you to kick the tyres and explore the Agents and Agentic AI API and the `langchain4j-agentic` module, combined with our Oracle embedding store module (`OracleEmbeddingStore`) to create a simple Agentic RAG scenario.

Anyway, I may update this blog post with the Javadoc link and address any other breaking changes in the near future.

So without further ado, let’s get started!

---

### Prerequisites

- JDK — [Java Development Kit](https://www.oracle.com) 17+
- [Oracle Database Free Release 23ai — Container Image](https://container-registry.oracle.com)
- [Oracle JDBC Driver 23ai](https://repo1.maven.org)
- Your preferred Java IDE — [Eclipse](https://www.eclipse.org), [IntelliJ](https://www.jetbrains.com), [VS Code](https://code.visualstudio.com)
- [Apache Maven](https://maven.apache.org) or Gradle

---

### Step 1: Configure your Java application

Configure your `pom.xml` with the following dependencies, which include the Oracle JDBC driver, UCP (Universal Connection Pool), and the LangChain4J modules for Oracle Embedding Store, in‑process embedding model, and other dependencies. Ensure you use the latest version of `langchain4j-oracle`.

Using Maven:

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <jdbc.version>23.9.0.25.07</jdbc.version>
</properties>

<dependencies>
    <!-- Oracle Database 23ai JDBC / UCP -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc17</artifactId>
        <version>${jdbc.version}</version>
    </dependency>
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ucp17</artifactId>
        <version>${jdbc.version}</version>
    </dependency>

    <!-- LangChain4j -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>1.3.0</version>
    </dependency>

    <!-- LangChain4j Agentic AI -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-agentic</artifactId>
        <version>1.3.0-beta9</version>
    </dependency>

    <!-- LangChain4j Oracle -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-oracle</artifactId>
        <version>1.3.0-beta9</version>
    </dependency>

    <!-- LangChain4j OpenAI -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai</artifactId>
        <version>1.3.0</version>
    </dependency>

    <!-- in‑process embedding model -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-embeddings-all-minilm-l6-v2-q</artifactId>
        <version>1.3.0-beta9</version>
    </dependency>

    <!-- SLF4J -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

---

### Step 2: Define an agent contract interface

The agent interface describes the contract of our service and its role, that is, how the agent should act. I use the `@Agent` and `@UserMessage` annotations from LangChain4j to define its contract and behavior.

This declarative approach allows the LangChain4j framework to generate a runtime implementation that uses the instructions provided by the annotations, which is an interesting highlight here.

```java
package com.oracle.dev.jdbc.langchain4j;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface OracleRag {

    @UserMessage("""
        You are a technical support agent working with Oracle Database 23ai.
        Generate error code descriptions for the provided Oracle Database error codes (the error code pattern is ORA-*****)
        with a single sentence with the description of the given error code.
        Return only an error code description and nothing else.
        The error code description is {{errorCodeDescription}}.
        """)
    @Agent("Generates an error code description based on the provided Oracle Database error cocde")
    String getErrorCodeDescription(@V("errorCodeDescription") String errorCodeDescription);
}
```

In a nutshell, here’s what this interface defines:

- The `@UserMessage` annotation defines the user message instruction and template.
- `@Agent` annotation marks the method as an agentic function.
- The `@V` annotation binds method parameters into the prompt template.

---

### Step 3: Create a tool for Oracle Database Integration

Now, we implement a retrieval tool that performs searches against the knowledge base, which is supported by vector embeddings created from ingesting a simple text file `oracle-error-codes-descriptions.txt` located under `/resources` containing the Oracle Database error codes and their descriptions.

Similarly to my previous blog post, it’s backed by the `OracleEmbeddingStore` and a content retriever instance. Note that the Java method `searchKnowledgeBase()` uses the `@Tool` annotation to make it callable during agent execution. The RAG agent will call this tool automatically as designed.

```java
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
        this.retriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(10)
            .minScore(0.5)
            .build();
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
```

Again, our tool supports this core functionality:

- The `@Tool` annotation makes the Java method `searchKnowledgeBase()` callable by the agent.
- An `EmbeddingStoreContentRetriever` is used to connect the embedding model and the Oracle Database vector store.

---

### Step 4: Create an Agentic AI application class

This step wires all the main components together in a runnable Agentic AI application. Here, we combine the chat model, the embedding model, and the `OracleEmbeddingStore`. It also helps in ingesting the sample Oracle Database error codes and descriptions into the vector store, creates the tool, instantiates the agent (`dev.langchain4j.agentic.AgenticServices`), and then runs the sample questions.

```java
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

public class OracleRagAgent {

    public static void main(String[] args) throws SQLException {

        DataSource dataSource = OracleDbUtils.getPooledDataSource();

        // Chat model
        ChatModel chatModel = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o-mini")
            .build();

        // Embedding store (Oracle DB as vector store)
        EmbeddingModel embeddingModel = new dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = OracleEmbeddingStore.builder()
            .dataSource(dataSource)
            .embeddingTable("rag_agent_embeddings", CreateOption.CREATE_IF_NOT_EXISTS)
            .build();

        // Index a few sample documents (simple demo ingestion)
        addErrorCodes(embeddingModel, embeddingStore);

        // Create a knowledge‑base tool over the Oracle embedding store
        OracleDatabaseTool kbTool = new OracleDatabaseTool(embeddingStore, embeddingModel);

        // Build an Agentic service that uses the Oracle Database tool
        OracleRag techSupportAgent = AgenticServices.agentBuilder(OracleRag.class)
            .chatModel(chatModel)
            .outputName("errorCodeDescription")
            .tools(kbTool)
            .build();

        // Ask a question. The tech support agent will use the Oracle Database tool to retrieve context
        String errorCode = "What is the error code description for ORA-00060?";
        String answer = techSupportAgent.getErrorCodeDescription(errorCode);

        System.out.println("Query: " + errorCode);
        System.out.println("Response: " + answer + "\n");

        errorCode = "What is the error code description for ORA-ORA-28018?";
        answer = techSupportAgent.getErrorCodeDescription(errorCode);

        System.out.println("Query: " + errorCode);
        System.out.println("Response: " + answer + "\n");
    }

    public static void addErrorCodes(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        List<Document> documents = new ArrayList<>();
        try (var inputStream = OracleRagAgent.class.getClassLoader()
                .getResourceAsStream("oracle-error-codes-descriptions.txt")) {
            if (inputStream == null) {
                throw new IllegalStateException("Resource oracle-error-codes-descriptions.txt not found on classpath. Make sure it is located under src/main/resources/");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                documents = reader.lines().filter(line -> !line.trim().isEmpty()).map(Document::from).collect(Collectors.toList());
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
```

Essentially, the Agentic AI application comprises the following functionality:

- Agentic service creation with `AgenticServices.agentBuilder()` method.
- Automatic tool usage when answering technical support questions.
- A Java method to ingest the Oracle Database error code and descriptions and store them using the OracleEmbeddingStore.

---

### Step 5: Run the sample application

When you run this, the agent loads error codes into the vector store, builds a retrieval tool, and then answers technical support queries.

> Note, the core focus of this blog post is to introduce how you can use the `dev.langchain4j.agentic.AgenticServices` class to create Agentic AI applications in Java with LangChain4J.

Needless to say, the sample data set I ingested and used to store the vector embeddings from the Oracle Database error codes and descriptions (again, a text file named `oracle-error-codes-descriptions.txt` under `/resources`) does not comprise an exhaustive list of all Oracle Database error codes and descriptions, as it’s for illustrative purposes only.

---

### Wrapping it up

That’s it! In this post, we built a simple Agentic AI application using LangChain4j, the [OracleEmbeddingStore](https://docs.langchain4j.dev), [Oracle AI Vector Search](https://docs.oracle.com), and the [Oracle Database 23ai](https://www.oracle.com). The key takeaway is that Agentic AI is not just about simple interactions where LLM models answer questions, but creating intelligent agents that can reason, retrieve data with RAG and tools, and perform actions within your existing Java applications.

This blog post only introduced the basic building blocks of creating Agentic AI applications in Java with LangChain4J and the OracleEmbeddingStore, so my focus was on presenting the basic components, their integration, and the `dev.langchain4j.agentic.AgenticServices` API. In my next blog post about this topic, I’ll show you how to integrate two different agents that can collaborate to execute a more elaborate scenario.

However, before that, we’ll explore how to use [MCP (Model Context Protocol)](https://modelcontextprotocol.io) with LangChain4J and the Oracle Database 23ai!

As one last tip, I invite you to check out the new [Database Navigator](https://plugins.jetbrains.com) tool published at JetBrains Marketplace. In a nutshell, it features a robust SQL and PL/SQL editor with advanced capabilities for managing database connections, executing scripts, editing database objects, and modifying data and code with IntelliJ!

I will soon post more examples of scenarios involving LangChain4J, the OracleEmbeddingStore, [GraalVM](https://www.graalvm.org), [Micronaut](https://micronaut.io), [Quarkus](https://quarkus.io), and other GenAI / Agentic AI-related topics!

We hope you liked this blog post. Stay tuned!

