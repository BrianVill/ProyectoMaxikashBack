package com.loki.tasks;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.HttpRequest;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Helper service to enqueue HTTP tasks using Google Cloud Tasks.
 */
@Service
public class CloudTasksService {

    private final String projectId;
    private final String locationId;
    private final String queueId;
    private final String baseUrl;

    public CloudTasksService(
            @Value("${gcp.tasks.project}") String projectId,
            @Value("${gcp.tasks.location}") String locationId,
            @Value("${gcp.tasks.queue}") String queueId,
            @Value("${tasks.worker.base-url:}") String baseUrl) {
        this.projectId = projectId;
        this.locationId = locationId;
        this.queueId = queueId;
        this.baseUrl = baseUrl;
    }

    public void enqueueTask(String url, String payload) throws IOException {
        String resolvedUrl = resolveUrl(url);
        try (CloudTasksClient client = CloudTasksClient.create()) {
            String queuePath = QueueName.of(projectId, locationId, queueId).toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .setUrl(resolvedUrl)
                    .setHttpMethod(HttpMethod.POST)
                    .putHeaders("Content-Type", "application/json")
                    .setBody(ByteString.copyFromUtf8(payload))
                    .build();
            Task task = Task.newBuilder().setHttpRequest(request).build();
            client.createTask(queuePath, task);
        }
    }

    private String resolveUrl(String url) {
        if (url == null || url.isBlank()) {
            return baseUrl;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + (url.startsWith("/") ? url : "/" + url);
    }
}
