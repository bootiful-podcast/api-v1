package com.example.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
class ProductionRequest {

	private String description;

	@JsonProperty("interview-wav")
	private String interviewWav;

	@JsonProperty("introduction-wav")
	private String introductionWav;

	private String timestamp;

}
