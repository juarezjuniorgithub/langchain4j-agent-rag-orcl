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
