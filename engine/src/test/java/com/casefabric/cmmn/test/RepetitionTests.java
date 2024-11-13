package com.casefabric.cmmn.test;

import com.casefabric.cmmn.test.basic.TestRepeatRule;
import com.casefabric.cmmn.test.casefile.TestCaseFileTransition;
import com.casefabric.cmmn.test.casefile.TestRepetitiveFileItems;
import com.casefabric.cmmn.test.task.TestGetListGetDetails;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    TestRepeatRule.class,
    TestRepetitiveFileItems.class,
    TestCaseFileTransition.class,
    TestGetListGetDetails.class
    })
public class RepetitionTests {

}
