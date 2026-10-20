package com.aeroplan.scheduling.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sfn.SfnClient;

/** Real AWS SDK client beans, only created when the "aws" Spring profile is active. */
@Configuration
@Profile("aws")
public class AwsClientConfig {

    @Bean
    public SfnClient sfnClient(@Value("${AWS_REGION:us-east-2}") String region) {
        return SfnClient.builder().region(Region.of(region)).build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient(@Value("${AWS_REGION:us-east-2}") String region) {
        return DynamoDbClient.builder().region(Region.of(region)).build();
    }
}
