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

/**
 * The {@link remoteshellConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author BurakEvren - Initial contribution
 */
@NonNullByDefault
public class remoteshellConfiguration {
    public String hostname = "";
    public int port = 9090;
}
