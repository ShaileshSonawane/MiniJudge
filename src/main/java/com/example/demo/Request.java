package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Request {
		@JsonProperty("code")
		public String code;
		@JsonProperty("input")
		public String input;
}
