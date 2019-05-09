package com.example.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
class ProductionRequest {

	@JsonProperty("description")
	private String description;

	@JsonProperty("interview-file")
	private String interviewFile;

	@JsonProperty("introduction-file")
	private String introductionFile;

	@JsonProperty("timestamp")
	private String timestamp;

	@JsonProperty("manifest")
	private String manifest;

	ProductionRequest() {
	}

	public ProductionRequest(String interview, String intro, String manifest,
			String timestamp, String description) {
		this.introductionFile = intro;
		this.interviewFile = interview;
		this.manifest = manifest;
		this.timestamp = timestamp;
		this.description = description;
	}

}
