/* 
 * Copyright 2014 - 2019 CaseFabric B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package testing;

import com.casefabric.cmmn.test.TestScript;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;

public class PeriodTest {

    public static void main(String[] args) {
        String st1 = "P1DT0H0M2S";
        String st = "P1Y";
        Period p = Period.parse(st);
        Duration d = Duration.parse(st1);
        TestScript.debugMessage("DurationL: " + d);
        TestScript.debugMessage("Period: " + p);
        Instant now = Instant.now();
        TestScript.debugMessage("Now: " + now);
        Instant then = now.plus(d);
        TestScript.debugMessage("Now: " + now + ", then: " + then);
        then = then.plus(p);
        TestScript.debugMessage("Now: " + now + ", then: " + then);
    }
}
