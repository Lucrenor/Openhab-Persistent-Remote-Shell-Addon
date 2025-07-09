/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.remoteshell.internal;

import static org.openhab.binding.remoteshell.internal.remoteshellBindingConstants.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link remoteshellHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author BurakEvren - Initial contribution
 */

@NonNullByDefault
public class remoteshellHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(remoteshellHandler.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private @Nullable remoteshellConfiguration config;

    public remoteshellHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(remoteshellConfiguration.class);
        if (config.hostname == null || config.hostname.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Hostname is not configured.");
        } else {
            updateStatus(ThingStatus.ONLINE);
            // Ensure the shell is stopped on startup
            scheduler.execute(() -> controlShell(OnOffType.OFF));
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            return;
        }

        // Handle the new Switch command
        if (CHANNEL_SHELL_CONTROL.equals(channelUID.getId())) {
            if (command instanceof OnOffType) {
                scheduler.execute(() -> controlShell((OnOffType) command));
            }
            return;
        }

        // Handle the command input
        if (CHANNEL_COMMAND.equals(channelUID.getId())) {
            String shellCommand = command.toString();
            if (shellCommand.isBlank()) {
                return;
            }
            scheduler.execute(() -> executeRemoteCommand(shellCommand));
        }
    }

    private void controlShell(OnOffType command) {
        String endpoint = (command == OnOffType.ON) ? "/start" : "/stop";
        String url = "http://" + config.hostname + ":" + config.port + endpoint;
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            // Update the switch state in the UI
            updateState(CHANNEL_SHELL_CONTROL, command);
        } catch (Exception e) {
            logger.error("Error controlling shell: {}", e.getMessage());
            updateState(CHANNEL_SHELL_CONTROL, OnOffType.OFF); // Set to OFF on error
        }
    }

    private void executeRemoteCommand(String shellCommand) {
        if (config == null) {
            return;
        }
        try {
            String escapedCommand = shellCommand.replace("\"", "\\\"");
            String requestBody = "{\"command\": \"" + escapedCommand + "\"}";
            String url = "http://" + config.hostname + ":" + config.port + "/send";

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                String output = jsonResponse.get("output").getAsString();
                updateState(CHANNEL_LAST_OUTPUT, new StringType(output));
            } else {
                updateState(CHANNEL_LAST_OUTPUT,
                        new StringType("Error: Agent responded with status " + response.statusCode()));
            }
        } catch (Exception e) {
            updateState(CHANNEL_LAST_OUTPUT, new StringType("Connection Error: " + e.getMessage()));
        }
    }
}
