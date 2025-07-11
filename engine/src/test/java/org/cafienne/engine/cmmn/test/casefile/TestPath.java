package org.cafienne.engine.cmmn.test.casefile;

import org.cafienne.engine.cmmn.actorapi.command.casefile.UpdateCaseFileItem;
import org.cafienne.engine.cmmn.definition.CaseDefinition;
import org.cafienne.engine.cmmn.instance.casefile.InvalidPathException;
import org.cafienne.engine.cmmn.instance.Path;
import org.cafienne.engine.cmmn.test.TestScript;
import org.cafienne.json.ValueMap;
import org.cafienne.util.Guid;
import org.junit.Test;

import static org.cafienne.engine.cmmn.test.TestScript.*;

public class TestPath {
    private final String caseName = "CaseFileTest";
    private final String caseInstanceId = new Guid().toString();
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/casefile/casefiletest.xml");
    private final TestScript testCase = new TestScript(caseName);
    private final ValueMap rootValue = new ValueMap("aaa", new ValueMap("aaa1", "true", "child_of_aaa", new ValueMap("child_of_aaa_1", "true")));

    @Test
    public void testValidPaths() {
        assertValidPathSyntax("abc   / ");
        assertValidPathSyntax("/aac/a[0] /");
        assertValidPathSyntax("/aac[100]");
        assertValidPathSyntax("abc/def/ghi[23]/jklm[30000003]/abc  /jdf[12]  /");
        assertValidPathSyntax("abc   / ");
    }

    @Test
    public void testInvalidPaths() {
        assertInvalidPathSyntax("/aac[b/", "Path should end with a closing bracket, as it has an opening bracket '/aac[b/'");
        assertInvalidPathSyntax("/aac[b]a/", "Path should end with a closing bracket, as it has an opening bracket '/aac[b]a/'");
        assertInvalidPathSyntax("/aac[b]/", "'b' is not a valid index in path '/aac[b]/'");
        assertInvalidPathSyntax("/aac[0]a/", "Path should end with a closing bracket, as it has an opening bracket '/aac[0]a/'");
        assertInvalidPathSyntax("/aac[-100]/", "'-100' is not a valid index. Path index may not be negative '/aac[-100]/'");
        assertInvalidPathSyntax("/aac[b]", "'b' is not a valid index in path '/aac[b]'");
        assertInvalidPathSyntax("/aac[0]a", "Path should end with a closing bracket, as it has an opening bracket '/aac[0]a'");
        assertInvalidPathSyntax("/aac[-100]", "'-100' is not a valid index. Path index may not be negative '/aac[-100]'");
        assertInvalidPathSyntax("/aac/[0] ", "Missing name part in path, cannot start with an opening bracket '/aac/[0] '");
        assertInvalidPathSyntax("/aaa////", "Path should not contain empty elements '//'");
        assertInvalidPathSyntax("////aacb/", "Path should not contain empty elements '//");
    }

    @Test
    public void testUndefinedPath() {
        TestScript.debugMessage(rootValue.toString());

        testCase.addStep(createCaseCommand(testUser, caseInstanceId, definitions));

        assertInvalidCaseFilePath("/aaa/def/", "The path '/aaa/def/' is invalid, since the part 'def' is not defined in the case file");
        assertInvalidCaseFilePath("/aacb/", "The path '/aacb/' is invalid, since the part 'aacb' is not defined in the case file");
        assertInvalidCaseFilePath("/aaa[0]/", "The path '/aaa[0]/' is invalid because the CaseFileItem is not of type array");
        assertInvalidCaseFilePath("aaa/child_of_aaa/test[0]", "The path 'aaa/child_of_aaa/test[0]' is invalid, since the part 'test' is not defined in the case file");
        assertInvalidCaseFilePath("aaa/child_of_aaa[0]", "The path 'aaa/child_of_aaa[0]' is invalid because the CaseFileItem is not of type array");

        testCase.runTest();
    }

    private void assertValidPathSyntax(String path) {
        try {
            new Path(path);
        } catch (InvalidPathException ipe) {
            throw new AssertionError("Did not expect path " + path + " to have invalid path syntax: " + ipe.getMessage());
        }
    }

    private void assertInvalidPathSyntax(String invalidPath, String expectedMessage) {
        try {
            new Path(invalidPath);
            throw new AssertionError("Did not expect path " + invalidPath + " to have proper path syntax");
        } catch (InvalidPathException ipe) {
            // Well done.
            print(ipe.getMessage());

            if (! ipe.getMessage().contains(expectedMessage)) {
                throw new AssertionError("Failure message does not match expected message\nExpected: " + expectedMessage + "\nReceived: " + ipe.getMessage());
            }
        }
    }

    private void assertInvalidCaseFilePath(String invalidPath, String expectedMessage) {
        UpdateCaseFileItem update = new UpdateCaseFileItem(testUser, caseInstanceId, rootValue, new Path(invalidPath));
        testCase.assertStepFails(update, expectedMessage);
    }

    void print(Object o) {
        System.out.println(o.toString());
    }
}
