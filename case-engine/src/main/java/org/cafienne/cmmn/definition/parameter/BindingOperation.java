package org.cafienne.cmmn.definition.parameter;

/**
 * Various methods how to update Task Output into a Case File Item
 */
public enum BindingOperation {
    /**
     * Use Add to create a new CaseFileItem in an array
     */
    Add,
    /**`
     * Use Replace to replace a single CaseFileItem's content with the output.
     * For Arrays, this will replace the entire array with a single element
     */
    Replace,
    /**
     * Use Update to update the content of the CaseFileItem
     */
    Update
}
