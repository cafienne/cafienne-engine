/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class URLUtil {
    private static final String DEFAULT_ENCODING = "UTF8";

    /**
     * Simple wrapper around URL encoding. Uses the UTF8 character set, and wraps/ignores
     * the possible exception if this character encoding is not supported.
     *
     * @param url
     * @return
     */
    public static String encode(String url) {
        String encodedURL = url;
        try {
            encodedURL = URLEncoder.encode(url, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            // Well ... isn't that strange?
        }
        return encodedURL;
    }

    /**
     * Simple wrapper around URL decoding. Uses the UTF8 character set, and wraps/ignores
     * the possible exception if this character encoding is not supported.
     *
     * @param url
     * @return
     */
    public static String decode(String url) {
        String decodedURL = url;
        try {
            decodedURL = URLDecoder.decode(url, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            // Well ... isn't that strange?
        }
        return decodedURL;
    }

}
