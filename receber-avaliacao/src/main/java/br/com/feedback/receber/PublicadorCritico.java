package br.com.feedback.receber;

import br.com.feedback.common.Config;
import br.com.feedback.common.EventoCritico;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Publica avaliações críticas no tópico Pub/Sub (PROJECT_ID e PUBSUB_TOPIC). */
public class PublicadorCritico {
    private static final Gson GSON = new Gson();
    private Publisher publisher;

    private synchronized Publisher publisher() throws IOException {
        if (publisher == null) {
            publisher = Publisher.newBuilder(
                    TopicName.of(Config.obrigatoria("PROJECT_ID"), Config.obrigatoria("PUBSUB_TOPIC"))).build();
        }
        return publisher;
    }

    public void publicar(EventoCritico evento) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        PubsubMessage msg = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(GSON.toJson(evento)))
                .build();
        publisher().publish(msg).get(10, TimeUnit.SECONDS);
    }
}
