package com.example.integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

@Data
@Log4j2
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "podcast")
class PodcastIntegrationProperties {

	private File inboundPodcastsDirectory;

	private String requestsQueue = "podcast-requests";

	private String requestsExchange = this.requestsQueue;

	private String requestsRoutingKey = this.requestsQueue;

	private String repliesQueue = "podcast-replies";

	private String repliesExchange = this.repliesQueue;

	private String repliesRoutingKey = this.repliesQueue;

}
