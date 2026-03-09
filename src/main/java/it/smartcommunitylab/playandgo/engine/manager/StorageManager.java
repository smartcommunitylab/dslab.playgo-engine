package it.smartcommunitylab.playandgo.engine.manager;

import java.io.ByteArrayInputStream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;

@Component
public class StorageManager {
	@SuppressWarnings("unused")
	private static transient final Logger logger = LoggerFactory.getLogger(StorageManager.class);
	
	@Value("${minio.endpoint}")
	private String endpoint;
	
	@Value("${minio.accessKey}")
	private String accessKey;
	
	@Value("${minio.secretKey}")
	private String secretKey;
	
	@Value("${minio.bucket}")
	private String bucket;
	
	private AmazonS3 s3Client = null;
	
	@PostConstruct
	public void init() {
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		ClientConfiguration clientConfig = new ClientConfiguration();
	    clientConfig.setProtocol(Protocol.HTTPS);
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(
		        endpoint, "us-east-1");		
		s3Client = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withClientConfiguration(clientConfig)
				.withEndpointConfiguration(endpointConfiguration)
				.withPathStyleAccessEnabled(true)
				.build();
	}
	
	public String uploadImage(String objectId, String contentType, byte[] data) throws Exception {
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(data.length);
		meta.setContentType(contentType);
		s3Client.putObject(bucket, objectId, new ByteArrayInputStream(data), meta);
		if(endpoint.endsWith("/")) {
			return endpoint + bucket + "/" + objectId;
		} else {
			return endpoint + "/" + bucket + "/" + objectId;
		}
	}
	
	public void deleteImage(String objectId) throws Exception {
		s3Client.deleteObject(bucket, objectId);
	}
	
	
}
