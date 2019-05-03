package com.example.integration;

import lombok.extern.log4j.Log4j2;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageHeaders;

@Log4j2
class LoggingHandler implements GenericHandler<Object> {

	@Override
	public Object handle(Object o, MessageHeaders messageHeaders) {
		log.info("payload: \n" + o.toString());
		log.info("headers:");
		messageHeaders.forEach((k, v) -> log.info("\t" + k + '=' + v));
		return o;
	}

}
