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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link remoteshellBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author BurakEvren - Initial contribution
 */
@NonNullByDefault
public class remoteshellBindingConstants {
    public static final String BINDING_ID = "remoteshell";
    public static final ThingTypeUID THING_TYPE_REMOTE_DEVICE = new ThingTypeUID(BINDING_ID, "remoteDevice");
    public static final String CHANNEL_COMMAND = "command";
    public static final String CHANNEL_LAST_OUTPUT = "lastOutput";
    public static final String CHANNEL_SHELL_CONTROL = "shellControl";
}
