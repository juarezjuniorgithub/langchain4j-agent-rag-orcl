package com.oracle.dev.jdbc.langchain4j;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface OracleRag {

  @UserMessage("""
      You are a technical support agent working with Oracle AI Database 26ai.
      Generate error code descriptions for the provided Oracle AI Database error codes (the error code pattern is ORA-*****)
      with a single sentence containing the description of the given error code.
      The error code is {{errorCode}}.
      """)
  @Agent("Generates an error code description based on the provided Oracle Database error cocde")
  String getErrorCodeDescription(@V("errorCode") String errorCode);

}
