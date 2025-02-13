package com.sap.ai.sdk.app.controllers;

import static com.sap.ai.sdk.foundationmodels.openai.OpenAiModel.GPT_35_TURBO;
import static org.assertj.core.api.Assertions.assertThat;

import com.sap.ai.sdk.foundationmodels.openai.OpenAiClient;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionOutput;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatCompletionParameters;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatMessage.OpenAiChatUserMessage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class OpenAiTest {
  @Test
  void chatCompletion() {
    final var completion = OpenAiController.chatCompletion();

    final var message = completion.getChoices().get(0).getMessage();
    assertThat(message.getRole()).isEqualTo("assistant");
    assertThat(message.getContent()).isNotEmpty();
  }

  @Test
  void chatCompletionImage() {
    final var completion = OpenAiController.chatCompletionImage();

    final var message = completion.getChoices().get(0).getMessage();
    assertThat(message.getRole()).isEqualTo("assistant");
    assertThat(message.getContent()).isNotEmpty();
  }

  @Test
  void streamChatCompletion() {
    final var request =
        new OpenAiChatCompletionParameters()
            .setMessages(List.of(new OpenAiChatUserMessage().addText("Who is the prettiest?")));

    final var totalOutput = new OpenAiChatCompletionOutput();
    final var emptyDeltaCount = new AtomicInteger(0);
    OpenAiClient.forModel(GPT_35_TURBO)
        .streamChatCompletionDeltas(request)
        .peek(totalOutput::addDelta)
        // foreach consumes all elements, closing the stream at the end
        .forEach(
            delta -> {
              final String deltaContent = delta.getDeltaContent();
              log.info("deltaContent: {}", deltaContent);
              if (deltaContent.isEmpty()) {
                emptyDeltaCount.incrementAndGet();
              }
            });

    // the first two and the last delta don't have any content
    // see OpenAiChatCompletionDelta#getDeltaContent
    assertThat(emptyDeltaCount.get()).isLessThanOrEqualTo(3);

    assertThat(totalOutput.getChoices()).isNotEmpty();
    assertThat(totalOutput.getChoices().get(0).getMessage().getContent()).isNotEmpty();
    assertThat(totalOutput.getPromptFilterResults()).isNotNull();
    assertThat(totalOutput.getChoices().get(0).getContentFilterResults()).isNotNull();
  }

  @Test
  void chatCompletionTools() {
    final var completion = OpenAiController.chatCompletionTools();

    final var message = completion.getChoices().get(0).getMessage();
    assertThat(message.getRole()).isEqualTo("assistant");
    assertThat(message.getTool_calls()).isNotNull();
    assertThat(message.getTool_calls().get(0).getFunction().getName()).isEqualTo("fibonacci");
  }

  @Test
  void embedding() {
    final var embedding = OpenAiController.embedding();

    // {"object":"list","model":"ada","data":[{"object":"embedding","embedding":[-0.0070958645....,-0.0014557659],"index":0}],"usage":{"completion_tokens":null,"prompt_tokens":2,"total_tokens":2}}
    assertThat(embedding.getData().get(0).getEmbedding()).hasSizeGreaterThan(1);
    assertThat(embedding.getModel()).isEqualTo("ada");
    assertThat(embedding.getObject()).isEqualTo("list");
  }
}
