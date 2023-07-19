/*********************************************************************
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   Kentyou - initial implementation
**********************************************************************/
package org.eclipse.sensinact.northbound.ws.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.sensinact.core.notification.AbstractResourceNotification;
import org.eclipse.sensinact.core.notification.ClientDataListener;
import org.eclipse.sensinact.core.notification.ClientLifecycleListener;
import org.eclipse.sensinact.core.session.SensiNactSession;
import org.eclipse.sensinact.northbound.query.api.AbstractQueryDTO;
import org.eclipse.sensinact.northbound.query.api.AbstractResultDTO;
import org.eclipse.sensinact.northbound.query.api.IQueryHandler;
import org.eclipse.sensinact.northbound.query.dto.SensinactPath;
import org.eclipse.sensinact.northbound.query.dto.notification.AbstractResourceNotificationDTO;
import org.eclipse.sensinact.northbound.query.dto.notification.ErrorResultNotificationDTO;
import org.eclipse.sensinact.northbound.query.dto.notification.ResourceDataNotificationDTO;
import org.eclipse.sensinact.northbound.query.dto.notification.ResourceLifecycleNotificationDTO;
import org.eclipse.sensinact.northbound.query.dto.notification.ResultResourceNotificationDTO;
import org.eclipse.sensinact.northbound.query.dto.query.QuerySubscribeDTO;
import org.eclipse.sensinact.northbound.query.dto.query.QueryUnsubscribeDTO;
import org.eclipse.sensinact.northbound.query.dto.result.ErrorResultDTO;
import org.eclipse.sensinact.northbound.query.dto.result.ResultSubscribeDTO;
import org.eclipse.sensinact.northbound.query.dto.result.ResultUnsubscribeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Websocket endpoint
 */
@WebSocket(idleTimeout = 0)
public class WebSocketEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEndpoint.class);

    /**
     * JSON mapper
     */
    private final ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).build();

    /**
     * Current WebSocket session
     */
    private final AtomicReference<Session> wsSession = new AtomicReference<>();

    /**
     * Current user session
     */
    private final SensiNactSession userSession;

    /**
     * Parent socket pool
     */
    private final WebSocketCreator pool;

    /**
     * Query handler
     */
    private final IQueryHandler queryHandler;

    /**
     * Active subscriptions
     */
    private final Set<String> subscriptions = new HashSet<>();

    /**
     * @param pool             WebSocket connections pool
     * @param sensiNactSession User session manager
     * @param queryHandler     Query handler
     */
    public WebSocketEndpoint(final WebSocketCreator pool, final SensiNactSession sensiNactSession,
            final IQueryHandler queryHandler) {
        this.pool = pool;
        this.userSession = sensiNactSession;
        this.queryHandler = queryHandler;
    }

    /**
     * Explicit WebSocket closure
     */
    public synchronized void close() {
        Session ws = wsSession.getAndSet(null);
        if (ws == null) {
            return;
        }

        // Close subscriptions first
        if (userSession != null) {
            for (final String listenerId : subscriptions) {
                userSession.removeListener(listenerId);
            }
        }
        subscriptions.clear();

        if (ws.isOpen()) {
            // Close websocket session
            try {
                ws.close();
            } catch (Throwable t) {
                logger.error("Error closing WebSocket: {}", t.getMessage(), t);
            }
        }

        userSession.expire();
    }

    @OnWebSocketConnect
    public void open(final Session session) {
        logger.debug("WebSocket opening - {}", session);
        wsSession.set(session);
    }

    @OnWebSocketClose
    public void onClose(final Session session, int statusCode, final String reason) {
        logger.debug("WebSocket closing - {} ({}: {})", session, statusCode, reason);
        pool.deleteSocketEndpoint(this);

        userSession.expire();
    }

    @OnWebSocketError
    public void onError(final Session session, final Throwable t) {
        logger.warn("Error from WebSocket session {}: {}", session, t.getMessage());
    }

    @OnWebSocketMessage
    public void onMessage(final Session wsSession, final String strContent) {

        if (userSession.isExpired()) {
            wsSession.close(StatusCode.ABNORMAL, "User session expired due to inactivity");
        } else {
            userSession.extend(Duration.of(5, ChronoUnit.MINUTES));
        }

        final AbstractQueryDTO query;
        try {
            query = mapper.readValue(strContent, AbstractQueryDTO.class);
        } catch (Throwable t) {
            logger.error("Error parsing WebSocket query: {}", t.getMessage(), t);
            sendError(wsSession, null, 400, "Error parsing query: " + t.getMessage());
            return;
        }

        try {
            final AbstractResultDTO result;
            switch (query.operation) {
            case SUBSCRIBE:
                handleSubscribe(query.requestId, (QuerySubscribeDTO) query);
                return;

            case UNSUBSCRIBE:
                result = handleUnsubscribe((QueryUnsubscribeDTO) query);
                break;

            default:
                result = queryHandler.handleQuery(userSession, query);
                break;
            }

            result.requestId = query.requestId;

            sendResult(wsSession, result);
        } catch (Throwable t) {
            logger.error("Error handling query {}: {}", query, t.getMessage(), t);
            sendError(wsSession, query.uri, 500, "Error running query: " + t.getMessage());
        }
    }

    /**
     * Registers a subscription of the current session
     *
     * @param requestId
     *
     * @param userSession Caller session
     * @param query       Subscription query
     * @return Result DTO
     */
    private void handleSubscribe(String requestId, final QuerySubscribeDTO query) throws Exception {
        final SensinactPath path = query.uri;
        CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> listenerId = new AtomicReference<>();

        final ResultSubscribeDTO result = new ResultSubscribeDTO();
        List<String> topics;
        if (path.targetsSpecificResource()) {
            result.uri = path.toUri();
            topics = List.of(result.uri.substring(1));
        } else {
            result.uri = "/";
            topics = List.of("*");
        }

        Predicate<AbstractResourceNotification> p;
        if (query.filter != null && !query.filter.isBlank()) {
            // TODO: parse filter
            // TODO: use filter criterion in event
            p = a -> true;
        } else {
            p = a -> true;
        }

        final ClientDataListener cld = (topic, evt) -> {
            Session ws = wsSession.get();
            if (ws == null || !ws.isOpen()) {
                logger.warn("Detected closed WebSocket. Stop listening");
                userSession.removeListener(listenerId.get());
            } else if (p.test(evt) && checkLatch(latch)) {
                sendNotification(ws, listenerId.get(), new ResourceDataNotificationDTO(evt));
            }
        };

        final ClientLifecycleListener cll = (topic, evt) -> {
            Session ws = wsSession.get();
            if (ws == null || !ws.isOpen()) {
                logger.warn("Detected closed WebSocket. Stop listening");
                userSession.removeListener(listenerId.get());
            } else if (p.test(evt) && checkLatch(latch)) {
                sendNotification(ws, listenerId.get(), new ResourceLifecycleNotificationDTO(evt));
            }
        };

        String id = userSession.addListener(topics, cld, null, cll, null);
        listenerId.set(id);

        // Store listener details
        subscriptions.add(id);
        // Send the subscription response to the client
        result.statusCode = 200;
        result.subscriptionId = id;
        result.requestId = requestId;
        try {
            Session ws = wsSession.get();
            if (ws == null || !ws.isOpen()) {
                subscriptions.remove(id);
                userSession.removeListener(id);
            } else {
                sendResult(ws, result);
            }
        } catch (Exception e) {
            subscriptions.remove(id);
            userSession.removeListener(id);

            // Don't release the latch so any pending notifications time out
            throw e;
        }

        // Release the listeners
        latch.countDown();
    }

    private boolean checkLatch(CountDownLatch latch) {
        try {
            return latch.await(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sends a notification to the client
     *
     * @param ws
     *
     * @param listenerId   ID of the subscription
     * @param notification Notification DTO
     */
    private void sendNotification(Session ws, final String listenerId,
            final AbstractResourceNotificationDTO notification) {
        try {
            final ResultResourceNotificationDTO result = new ResultResourceNotificationDTO();
            result.statusCode = 200;
            result.uri = new SensinactPath(notification.provider, notification.service, notification.resource).toUri();
            result.subscriptionId = listenerId;
            result.notification = notification;
            ws.getRemote().sendString(mapper.writeValueAsString(result));
        } catch (IOException e) {
            logger.error("Error sending notification to client: {}", e.getMessage(), e);
            try {
                final ErrorResultNotificationDTO errorNotification = new ErrorResultNotificationDTO(listenerId);
                ws.getRemote().sendString(mapper.writeValueAsString(errorNotification));
            } catch (IOException e2) {
                logger.error("Error sending notification to client: {}. Closing WebSocket.", e2.getMessage(), e2);
                userSession.removeListener(listenerId);
                WebSocketEndpoint.this.close();
            }
        }
    }

    /**
     * Removes a subscription from the current session
     *
     * @param query Subscription removal query
     * @return Result DTO
     */
    private AbstractResultDTO handleUnsubscribe(final QueryUnsubscribeDTO query) {
        if (subscriptions.remove(query.subscriptionId)) {
            userSession.removeListener(query.subscriptionId);
            final ResultUnsubscribeDTO result = new ResultUnsubscribeDTO();
            result.statusCode = 200;
            result.subscriptionId = query.subscriptionId;
            return result;
        }

        return new ErrorResultDTO(404, "Unknown subscription");
    }

    /**
     * Sends a result
     *
     * @param session WebSocket session
     * @param dto     Result DTO
     */
    private void sendResult(final Session session, final AbstractResultDTO dto) {
        try {
            session.getRemote().sendString(mapper.writeValueAsString(dto));
        } catch (IOException e) {
            sendError(session, null, 500, "Error sending results: " + e.getMessage());
        }
    }

    /**
     * Sends an error to the client
     *
     * @param session      WebSocket session
     * @param uri          Target URI, if known
     * @param statusCode   Error status code
     * @param errorMessage Error message
     */
    private void sendError(final Session session, final SensinactPath target, final int statusCode,
            final String errorMessage) {
        final AbstractResultDTO dto = new ErrorResultDTO();
        dto.uri = target != null ? target.toUri() : null;
        dto.statusCode = statusCode;
        dto.error = errorMessage;

        String payload;
        try {
            payload = mapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            logger.error("Error preparing error message payload to client: {}", e.getMessage(), e);
            payload = "{\"uri\": null, \"statusCode\": 500, \"error\": \"Error sending error\", \"result\": null}";
        }

        try {
            session.getRemote().sendString(payload);
        } catch (IOException e) {
            logger.error("Error sending error to client: {}", e.getMessage(), e);
        }
    }
}