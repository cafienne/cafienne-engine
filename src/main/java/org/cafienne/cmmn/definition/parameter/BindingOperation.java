package org.cafienne.cmmn.definition.parameter;

/**
 * Various methods how to update Task Output into a Case File Item
 */
public enum BindingOperation {
    None,
    /**
     * Use Current to do an input binding refinement on the most recently changed
     * case file item in the array of items (default behavior of engine)
     */
    Current,
    /**
     * When using reference, the path of the case file item will be given, instead of the value
     */
    Reference,
    /**
     * Use List to do an input binding refinement to pass the entire case file item array
     */
    List,
    /**
     * Use Indexed to do an input binding refinement
     * based on the index of the first found repeating plan item (either the task or one of it's parent stages)
     */
    Indexed,
    /**
     * When using reference-indexed, the path of the case file item will be given, instead of the value
     */
    ReferenceIndexed,
    /**
     * Use Add to create a new CaseFileItem in an array
     */
    Add,
    /**
     * `
     * Use Replace to replace a single CaseFileItem's content with the output.
     * For Arrays, this will replace the entire array with a single element
     */
    Replace,
    /**
     * Use Update to update the content of the CaseFileItem
     */
    Update,
    /**
     * Use UpdateIndexed to update a specific element in the CaseFileItem array based on the index of the
     * task (or it's first repeating ancestor stage)
     */
    UpdateIndexed,
    /**
     * Use ReplaceIndexed to replace a specific element in the CaseFileItem array based on the index of the
     * task (or it's first repeating ancestor stage)
     */
    ReplaceIndexed,
}
