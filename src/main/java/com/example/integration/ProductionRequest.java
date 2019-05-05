package com.example.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
class ProductionRequest {

	private String description;

	@JsonProperty("interview-wav")
	private String interviewWav;

	@JsonProperty("introduction-wav")
	private String introductionWav;

	private String timestamp;

	@JsonProperty("manifest")
	private String manifest;

	ProductionRequest() {
	}

	public ProductionRequest(String interview, String intro, String manifest,
			String timestamp, String description) {
		this.introductionWav = intro;
		this.interviewWav = interview;
		this.manifest = manifest;
		this.timestamp = timestamp;
		this.description = description;
	}

}
