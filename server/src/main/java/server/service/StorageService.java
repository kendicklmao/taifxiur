package server.service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

public class StorageService {
    private static final String ENDPOINT = "https://uxmbyzqylbtuqyyatzwj.storage.supabase.co/storage/v1/s3";
    private static final String REGION = "ap-northeast-1";
    private static final String ACCESS_KEY = "b271007c9a144ebe6499dc925fe848aa";
    private static final String SECRET_KEY = "df128b42a3fef72b2470525f28a4b8d9779f75b26b5700309bd6b9cf7cf0ce2b";
    private static final String BUCKET_NAME = "Auction";

    private final S3Client s3;

    public StorageService() {
        s3 = S3Client.builder()
                .endpointOverride(URI.create(ENDPOINT))
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .build();
    }

    public String uploadFile(String key, byte[] fileData, String contentType) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(fileData));
        return String.format("https://uxmbyzqylbtuqyyatzwj.storage.supabase.co/storage/v1/object/public/%s/%s", BUCKET_NAME, key);
    }
}
