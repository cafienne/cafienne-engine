/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.util;

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
