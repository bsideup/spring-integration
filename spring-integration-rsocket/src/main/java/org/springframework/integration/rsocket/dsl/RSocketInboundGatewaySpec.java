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

import org.springframework.core.ResolvableType;
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.rsocket.AbstractRSocketConnector;
import org.springframework.integration.rsocket.inbound.RSocketInboundGateway;
import org.springframework.messaging.rsocket.RSocketStrategies;

/**
 * The {@link MessagingGatewaySpec} implementation for the {@link RSocketInboundGateway}.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class RSocketInboundGatewaySpec extends MessagingGatewaySpec<RSocketInboundGatewaySpec, RSocketInboundGateway> {

	RSocketInboundGatewaySpec(String... path) {
		super(new RSocketInboundGateway(path));
	}

	/**
	 * Configure {@link RSocketStrategies} instead of a default one.
	 * @param rsocketStrategies the {@link RSocketStrategies} to use.
	 * @return the spec
	 * @see RSocketInboundGateway#setRSocketStrategies(RSocketStrategies)
	 */
	public RSocketInboundGatewaySpec rsocketStrategies(RSocketStrategies rsocketStrategies) {
		this.target.setRSocketStrategies(rsocketStrategies);
		return this;
	}

	/**
	 * Provide an {@link AbstractRSocketConnector} reference for an explicit endpoint mapping.
	 * @param rsocketConnector the {@link AbstractRSocketConnector} to use.
	 * @return the spec
	 * @see RSocketInboundGateway#setRSocketConnector(AbstractRSocketConnector)
	 */
	public RSocketInboundGatewaySpec rsocketConnector(AbstractRSocketConnector rsocketConnector) {
		this.target.setRSocketConnector(rsocketConnector);
		return this;
	}

	/**
	 * Specify the type of payload to be generated when the inbound RSocket request
	 * content is read by the converters/encoders.
	 * @param requestElementType The payload type.
	 * @return the spec
	 * @see RSocketInboundGateway#setRequestElementType(ResolvableType)
	 */
	public RSocketInboundGatewaySpec requestElementType(ResolvableType requestElementType) {
		this.target.setRequestElementType(requestElementType);
		return this;
	}

}
