/*
 * Copyright 2009-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.eclipse.refactoring.test.rename;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.eclipse.refactoring.test.RefactoringTestCase;
import org.codehaus.groovy.eclipse.refactoring.test.RefactoringTestSetup;
import org.codehaus.groovy.eclipse.refactoring.test.internal.ParticipantTesting;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public final class RenameFieldTests extends RefactoringTestCase {

    public static junit.framework.Test suite() {
        return new RefactoringTestSetup(new junit.framework.TestSuite(RenameFieldTests.class));
    }

    public static junit.framework.Test setUpTest(junit.framework.Test test) {
        return new RefactoringTestSetup(test);
    }

    public RenameFieldTests(String name) {
        super(name);
    }

    protected String getRefactoringPath() {
        return "RenameField/";
    }

    protected void setUp() throws Exception {
        super.setUp();
        fIsPreDeltaTest= true;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void helper2_0(String typeName, String fieldName,
            String newFieldName, boolean updateReferences,
            boolean createDelegates, boolean renameGetter, boolean renameSetter,
            boolean performOnError, boolean updateTextual)
            throws Exception {
        ICompilationUnit cu = createCUfromTestFile(getPackageP(), "A");
        try {
            IType classA = getType(cu, typeName);
            if (classA == null) {
                classA = cu.getJavaProject().findType(typeName);
            }
            IField field = classA.getField(fieldName);
            boolean isEnum = JdtFlags.isEnum(field);
            String id = isEnum ? IJavaRefactorings.RENAME_ENUM_CONSTANT
                    : IJavaRefactorings.RENAME_FIELD;
            RenameJavaElementDescriptor descriptor = RefactoringSignatureDescriptorFactory
                    .createRenameJavaElementDescriptor(id);
            descriptor.setUpdateReferences(updateReferences);
            descriptor.setJavaElement(field);
            descriptor.setNewName(newFieldName);
            if (!isEnum) {
                descriptor.setRenameGetters(renameGetter);
                descriptor.setRenameSetters(renameSetter);
                descriptor.setKeepOriginal(createDelegates);
                descriptor.setUpdateTextualOccurrences(updateTextual);
                descriptor.setDeprecateDelegate(true);
            }
            RenameRefactoring refactoring = (RenameRefactoring) createRefactoring(descriptor);
            RenameFieldProcessor processor = (RenameFieldProcessor) refactoring
                    .getProcessor();

            List<IAnnotatable> elements = new ArrayList<IAnnotatable>();
            elements.add(field);
            List<RenameArguments> args = new ArrayList<RenameArguments>();
            args.add(new RenameArguments(newFieldName, updateReferences));
            if (renameGetter) {
                elements.add(processor.getGetter());
                args.add(new RenameArguments(processor.getNewGetterName(),
                        updateReferences));
            }
            if (renameSetter) {
                elements.add(processor.getSetter());
                args.add(new RenameArguments(processor.getNewSetterName(),
                        updateReferences));
            }
            String[] renameHandles = ParticipantTesting.createHandles(elements
                    .toArray());

            RefactoringStatus result = performRefactoring(refactoring, performOnError);
            assertTrue("was supposed to pass", result==null || result.isOK());
            assertEqualLines("invalid renaming",
                    getFileContents(getOutputTestFileName("A")), cu.getSource());

            ParticipantTesting.testRename(renameHandles, args.toArray(new RenameArguments[args.size()]));

            assertTrue("anythingToUndo", RefactoringCore.getUndoManager()
                    .anythingToUndo());
            assertTrue("! anythingToRedo", !RefactoringCore.getUndoManager()
                    .anythingToRedo());

            RefactoringCore.getUndoManager().performUndo(null,
                    new NullProgressMonitor());
            assertEqualLines("invalid undo",
                    getFileContents(getInputTestFileName("A")), cu.getSource());

            assertTrue("! anythingToUndo", !RefactoringCore.getUndoManager()
                    .anythingToUndo());
            assertTrue("anythingToRedo", RefactoringCore.getUndoManager()
                    .anythingToRedo());

            RefactoringCore.getUndoManager().performRedo(null,
                    new NullProgressMonitor());
            assertEqualLines("invalid redo",
                    getFileContents(getOutputTestFileName("A")), cu.getSource());
        } finally {
            performDummySearch();
            cu.delete(true, null);
        }
    }

    private void helper2(boolean updateReferences) throws Exception {
        helper2_0("A", "f", "g", updateReferences, false, false, false, false, false);
    }
    private void helperPerformOnError(boolean updateReferences) throws Exception {
        helper2_0("A", "f", "g", updateReferences, false, false, false, true, false);
    }
    private void helperScript() throws Exception {
        helper2_0("B", "f", "g", true, false, false, false, false, false);
    }

    private void helper2() throws Exception {
        helper2(true);
    }

    public void testInitializer1() throws Exception {
        helper2();
    }

    public void testInitializer2() throws Exception {
        helper2();
    }

    public void testInitializer3() throws Exception {
        helper2();
    }

    public void test1() throws Exception {
        helper2();
    }
    public void test2() throws Exception {
        helper2();
    }
    public void test3() throws Exception {
        helper2();
    }
    public void test4() throws Exception {
        helper2();
    }
    public void test5() throws Exception {
        helperPerformOnError(true);
    }
    public void test6() throws Exception {
        helper2();
    }
    public void test7() throws Exception {
        helperPerformOnError(true);
    }
    public void test8() throws Exception {
        helper2();
    }
    public void test9() throws Exception {
        helper2();
    }
    public void test10() throws Exception {
        helper2();
    }
    public void test11() throws Exception {
        createCU(((IPackageFragmentRoot) getPackageP().getParent()).createPackageFragment("o", true, null), "Other.java",
                "package o;\npublic class Other { public static int FOO;\n }");
        helper2_0("o.Other", "FOO", "BAR", true, false, false, false, false, false);
    }
    public void testScript1() throws Exception {
        helperScript();
    }
    public void testScript2() throws Exception {
        helperScript();
    }
    public void test12() throws Exception {
        helper2_0("A", "f", "g", true, false, false, false, false, true);
    }
    public void test13() throws Exception {
        helper2_0("A", "f", "g", true, false, false, false, false, true);
    }
    public void test14() throws Exception {
        helper2_0("MyBean", "foo", "baz", true, false, false, false, false, true);
    }
}
