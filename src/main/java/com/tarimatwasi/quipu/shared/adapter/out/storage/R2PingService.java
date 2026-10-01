package com.tarimatwasi.quipu.shared.adapter.out.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class R2PingService {

  private final String endpoint;
  private final String accessKeyId;
  private final String secretAccessKey;
  private final String bucketName;

  public R2PingService(
      @Value("${app.r2.endpoint}") String endpoint,
      @Value("${app.r2.access-key-id}") String accessKeyId,
      @Value("${app.r2.secret-access-key}") String secretAccessKey,
      @Value("${app.r2.bucket-name}") String bucketName) {
    this.endpoint = endpoint;
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
    this.bucketName = bucketName;
  }

  public String ping() {
    String key = "dev/ping-" + System.currentTimeMillis() + ".txt";
    // Client built per call so missing/bad config reports FAIL instead of crashing context startup.
    try (S3Client s3Client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of("auto"))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
            .build()) {
      s3Client.putObject(
          PutObjectRequest.builder().bucket(bucketName).key(key).build(),
          RequestBody.fromString("connectivity check"));
      s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
      return "OK";
    } catch (SdkException | IllegalArgumentException e) {
      return "FAIL: " + e.getMessage();
    }
  }
}
