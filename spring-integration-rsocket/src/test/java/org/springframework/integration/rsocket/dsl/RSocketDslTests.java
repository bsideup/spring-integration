/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.rsocket.dsl;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.ServerRSocketConnector;
import org.springframework.integration.rsocket.outbound.RSocketOutboundGateway;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
@SpringJUnitConfig
@DirtiesContext
public class RSocketDslTests {

	@Autowired
	@Qualifier("rsocketUpperCaseRequestFlow.gateway")
	private Function<Flux<String>, Flux<String>> rsocketUpperCaseFlowFunction;

	@Test
	void testRsocketUpperCaseFlows() {
		Flux<String> result = this.rsocketUpperCaseFlowFunction.apply(Flux.just("a\n", "b\n", "c\n"));

		StepVerifier.create(result)
				.expectNext("A", "B", "C")
				.verifyComplete();
	}

	@Configuration
	@EnableIntegration
	public static class TestConfiguration {

		@Bean
		public ServerRSocketConnector serverRSocketConnector() {
			return new ServerRSocketConnector("localhost", 0);
		}

		@Bean
		public ClientRSocketConnector clientRSocketConnector(ServerRSocketConnector serverRSocketConnector) {
			int port = serverRSocketConnector.getBoundPort().block();
			ClientRSocketConnector clientRSocketConnector = new ClientRSocketConnector("localhost", port);
			clientRSocketConnector.setAutoStartup(false);
			return clientRSocketConnector;
		}

		@Bean
		public IntegrationFlow rsocketUpperCaseRequestFlow(ClientRSocketConnector clientRSocketConnector) {
			return IntegrationFlows
					.from(Function.class)
					.handle(RSockets.outboundGateway("/uppercase")
							.command((message) -> RSocketOutboundGateway.Command.requestStreamOrChannel)
							.expectedResponseType("T(java.lang.String)")
							.clientRSocketConnector(clientRSocketConnector))
					.get();
		}

		@Bean
		public IntegrationFlow rsocketUpperCaseFlow() {
			return IntegrationFlows
					.from(RSockets.inboundGateway("/uppercase"))
					.<Flux<String>, Flux<String>>transform((flux) -> flux.map(String::toUpperCase))
					.get();
		}

	}

}
