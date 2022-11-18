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

package org.cafienne.cmmn.test;

import akka.serialization.JSerializer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TestSerializer extends JSerializer {

    private Map<String, Object> cache = new LinkedHashMap<>();

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        String id = new String(bytes);
        if (cache.containsKey(id)) {
            Object o = cache.get(id);
            cache.remove(id, o);
            return o;
        }

        return null;
    }

    @Override
    public int identifier() {
        return 999;
    }

    @Override
    public byte[] toBinary(Object o) {
        String id = UUID.randomUUID().toString();
        cache.put(id, o);
        return id.getBytes();
    }

    @Override
    public boolean includeManifest() {
        return false;
    }
}
