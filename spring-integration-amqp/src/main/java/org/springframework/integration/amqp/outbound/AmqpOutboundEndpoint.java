/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.amqp.outbound;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnCallback;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.Lifecycle;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.amqp.support.MappingUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Adapter that converts and sends Messages to an AMQP Exchange.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class AmqpOutboundEndpoint extends AbstractAmqpOutboundEndpoint
		implements ConfirmCallback, ReturnCallback {

	private static final Duration DEFAULT_CONFIRM_TIMEOUT = Duration.ofSeconds(5);

	private final AmqpTemplate amqpTemplate;

	private final RabbitTemplate rabbitTemplate;

	private boolean expectReply;

	private boolean waitForConfirm;

	private Duration waitForConfirmTimeout = DEFAULT_CONFIRM_TIMEOUT;

	public AmqpOutboundEndpoint(AmqpTemplate amqpTemplate) {
		Assert.notNull(amqpTemplate, "amqpTemplate must not be null");
		this.amqpTemplate = amqpTemplate;
		if (amqpTemplate instanceof RabbitTemplate) {
			setConnectionFactory(((RabbitTemplate) amqpTemplate).getConnectionFactory());
			this.rabbitTemplate = (RabbitTemplate) amqpTemplate;
		}
		else {
			this.rabbitTemplate = null;
		}
	}

	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	/**
	 * Set to true if you want to block the calling thread until a publisher confirm has
	 * been received. Requires a template configured for returns. If a confirm is not
	 * received within the confirm timeout or a negative acknowledgment or returned
	 * message is received, an exception will be thrown. Does not apply to the gateway
	 * since it blocks awaiting the reply.
	 * @param waitForConfirm true to block until the confirmation or timeout is received.
	 * @since 5.2
	 * @see #setConfirmTimeout(long)
	 */
	public void setWaitForConfirm(boolean waitForConfirm) {
		this.waitForConfirm = waitForConfirm;
	}

	@Override
	public String getComponentType() {
		return this.expectReply ? "amqp:outbound-gateway" : "amqp:outbound-channel-adapter";
	}


	@Override
	public RabbitTemplate getRabbitTemplate() {
		return this.rabbitTemplate;
	}

	@Override
	protected void endpointInit() {
		if (getConfirmCorrelationExpression() != null) {
			Assert.notNull(this.rabbitTemplate,
					"RabbitTemplate implementation is required for publisher confirms");
			this.rabbitTemplate.setConfirmCallback(this);
		}
		if (getReturnChannel() != null) {
			Assert.notNull(this.rabbitTemplate,
					"RabbitTemplate implementation is required for publisher confirms");
			this.rabbitTemplate.setReturnCallback(this);
		}
		Duration confirmTimeout = getConfirmTimeout();
		if (confirmTimeout != null) {
			this.waitForConfirmTimeout = confirmTimeout;
		}
	}

	@Override
	protected void doStop() {
		if (this.amqpTemplate instanceof Lifecycle) {
			((Lifecycle) this.amqpTemplate).stop();
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		CorrelationData correlationData = generateCorrelationData(requestMessage);
		String exchangeName = generateExchangeName(requestMessage);
		String routingKey = generateRoutingKey(requestMessage);
		if (this.expectReply) {
			return sendAndReceive(exchangeName, routingKey, requestMessage, correlationData);
		}
		else {
			send(exchangeName, routingKey, requestMessage, correlationData);
			if (this.waitForConfirm && correlationData != null) {
				waitForConfirm(requestMessage, correlationData);
			}
			return null;
		}
	}

	private void waitForConfirm(Message<?> requestMessage, CorrelationData correlationData) {
		try {
			Confirm confirm = correlationData.getFuture().get(this.waitForConfirmTimeout.toMillis(),
					TimeUnit.MILLISECONDS);
			if (!confirm.isAck()) {
				throw new AmqpException("Negative publisher confirm received: " + confirm);
			}
			if (correlationData.getReturnedMessage() != null) {
				throw new AmqpException("Message was returned by the broker");
			}
		}
		catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException e) {
			throw new AmqpException("Failed to get publisher confirm", e);
		}
		catch (TimeoutException e) {
			throw new MessageTimeoutException(requestMessage, this + ": Timed out awaiting publisher confirm", e);
		}
	}

	private void send(String exchangeName, String routingKey,
			final Message<?> requestMessage, CorrelationData correlationData) {
		if (this.rabbitTemplate != null) {
			MessageConverter converter = this.rabbitTemplate.getMessageConverter();
			org.springframework.amqp.core.Message amqpMessage = MappingUtils.mapMessage(requestMessage, converter,
					getHeaderMapper(), getDefaultDeliveryMode(), isHeadersMappedLast());
			addDelayProperty(requestMessage, amqpMessage);
			this.rabbitTemplate.send(exchangeName, routingKey, amqpMessage, correlationData);
		}
		else {
			this.amqpTemplate.convertAndSend(exchangeName, routingKey, requestMessage.getPayload(),
					message -> {
						getHeaderMapper().fromHeadersToRequest(requestMessage.getHeaders(),
								message.getMessageProperties());
						return message;
					});
		}
	}

	private AbstractIntegrationMessageBuilder<?> sendAndReceive(String exchangeName, String routingKey,
			Message<?> requestMessage, CorrelationData correlationData) {

		Assert.state(this.rabbitTemplate != null,
				"RabbitTemplate implementation is required for publisher confirms");
		MessageConverter converter = this.rabbitTemplate.getMessageConverter();
		org.springframework.amqp.core.Message amqpMessage = MappingUtils.mapMessage(requestMessage, converter,
				getHeaderMapper(), getDefaultDeliveryMode(), isHeadersMappedLast());
		addDelayProperty(requestMessage, amqpMessage);
		org.springframework.amqp.core.Message amqpReplyMessage =
				this.rabbitTemplate.sendAndReceive(exchangeName, routingKey, amqpMessage,
						correlationData);

		if (amqpReplyMessage == null) {
			return null;
		}
		return buildReply(converter, amqpReplyMessage);
	}

	@Override
	public void confirm(CorrelationData correlationData, boolean ack, String cause) {
		handleConfirm(correlationData, ack, cause);
	}

	@Override
	public void returnedMessage(org.springframework.amqp.core.Message message, int replyCode, String replyText,
			String exchange, String routingKey) {

		// no need for null check; we asserted we have a RabbitTemplate in doInit()
		MessageConverter converter = this.rabbitTemplate.getMessageConverter();
		Message<?> returned = buildReturnedMessage(message, replyCode, replyText, exchange,
				routingKey, converter);
		getReturnChannel().send(returned);
	}

}
