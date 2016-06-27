/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Gemalto M2M GmbH
 *******************************************************************************/
package org.eclipse.leshan.core.node.codec;

import java.util.Arrays;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonEncoder;
import org.eclipse.leshan.core.node.codec.opaque.LwM2mNodeOpaqueEncoder;
import org.eclipse.leshan.core.node.codec.text.LwM2mNodeTextEncoder;
import org.eclipse.leshan.core.node.codec.tlv.LwM2mNodeTlvEncoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultLwM2mNodeEncoder implements LwM2mNodeEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLwM2mNodeEncoder.class);

    @Override
    public byte[] encode(LwM2mNode node, ContentFormat format, LwM2mPath path, LwM2mModel model) {
        Validate.notNull(node);
        Validate.notNull(format);

        LOG.debug("Encoding node {} for path {} and format {}", node, path, format);

        byte[] encoded = null;
        switch (format.getCode()) {
        case ContentFormat.TLV_CODE:
            encoded = LwM2mNodeTlvEncoder.encode(node, path, model);
            break;
        case ContentFormat.TEXT_CODE:
            encoded = LwM2mNodeTextEncoder.encode(node, path, model);
            break;
        case ContentFormat.OPAQUE_CODE:
            encoded = LwM2mNodeOpaqueEncoder.encode(node, path, model);
            break;
        case ContentFormat.JSON_CODE:
            encoded = LwM2mNodeJsonEncoder.encode(node, path, model);
            break;
        default:
            throw new IllegalArgumentException("Cannot encode " + node + " with format " + format);
        }

        LOG.trace("Encoded node {}: {}", node, Arrays.toString(encoded));
        return encoded;
    }
}