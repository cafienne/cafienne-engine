package org.cafienne.cmmn.test;

import org.cafienne.cmmn.test.basic.RepeatRule;
import org.cafienne.cmmn.test.casefile.CaseFileTransitionTest;
import org.cafienne.cmmn.test.casefile.RepetitiveFileItems;
import org.cafienne.cmmn.test.task.TestGetListGetDetails;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    RepeatRule.class,
    RepetitiveFileItems.class,
    CaseFileTransitionTest.class,
    TestGetListGetDetails.class
    })
public class RepetitionTests {

}
