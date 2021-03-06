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
package org.codehaus.groovy.eclipse.codeassist.tests;

import static org.eclipse.jdt.ui.PreferenceConstants.TYPEFILTER_ENABLED

import org.codehaus.groovy.eclipse.test.EclipseTestSetup
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.junit.Test

/**
 * Tests that type completions are working properly.
 */
final class TypeCompletionTests extends CompletionTestCase {

    private static final String HTML = "HTML"
    private static final String HTML_PROPOSAL = "HTML - javax.swing.text.html"

    @Test
    void testCompletionTypesInScript() {
        String contents = HTML
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, HTML))
        proposalExists(proposals, HTML_PROPOSAL, 1)
    }

    @Test
    void testCompletionTypesInScript2() {
        String contents = "new HTML()"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, HTML))
        proposalExists(proposals, HTML_PROPOSAL, 1)
    }

    @Test
    void testCompletionTypesInMethod() {
        String contents = "def x() {\nHTML\n}"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, HTML))
        proposalExists(proposals, HTML_PROPOSAL, 1)
    }

    @Test
    void testCompletionTypesInMethod2() {
        String contents = "class Foo {\ndef x() {\nHTML\n}}"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, HTML))
        proposalExists(proposals, HTML_PROPOSAL, 1)
    }

    @Test
    void testCompletionTypesInParameter() {
        String contents = "def x(HTML h) { }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, HTML))
        proposalExists(proposals, HTML_PROPOSAL, 1)
    }

    @Test
    void testCompletionTypesInParameter2() {
        String contents = "def x(t, HTML h) { }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, HTML))
        proposalExists(proposals, HTML_PROPOSAL, 1)
    }

    @Test
    void testCompletionTypesInParameter3() {
        String contents = "def x(t, HTML ... h) { }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, HTML))
        proposalExists(proposals, HTML_PROPOSAL, 1)
    }

    @Test
    void testCompletionTypesInParameter4() {
        String contents = "def x(t, h = HTML) { }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, HTML))
        proposalExists(proposals, HTML_PROPOSAL, 1)
    }

    @Test
    void testCompletionTypesInClassBody() {
        String contents = "class Foo {\nHTML\n}"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, HTML))
        proposalExists(proposals, HTML_PROPOSAL, 1)
    }

    @Test
    void testCompletionTypesInExtends() {
        String contents = "class Foo extends HTML { }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, HTML))
        proposalExists(proposals, HTML_PROPOSAL, 1)
    }

    @Test
    void testCompletionTypesInImplements() {
        String contents = "class Foo implements HTMLAnchElem { }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "HTMLAnchElem"))
        proposalExists(proposals, "HTMLAnchorElement - org.w3c.dom.html", 1)
    }

    @Test
    void testCompleteFullyQualifiedTypeInScript() {
        String contents = "javax.swing.text.html.HTMLDocume"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "HTMLDocume"))
        proposalExists(proposals, "HTMLDocument", 1, true)
    }

    @Test
    void testCompleteFullyQualifiedTypeInClass() {
        String contents = "class Foo { javax.swing.text.html.HTMLDocume }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "HTMLDocume"))
        proposalExists(proposals, "HTMLDocument", 1, true)
    }

    @Test
    void testCompleteFullyQualifiedTypeInMethod() {
        String contents = "class Foo { def x() { javax.swing.text.html.HTMLDocume } }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "HTMLDocume"))
        proposalExists(proposals, "HTMLDocument", 1, true)
    }

    @Test
    void testCompleteFullyQualifiedTypeInMethodParams() {
        String contents = "class Foo { def x(javax.swing.text.html.HTMLDocume) { } }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "HTMLDocume"))
        proposalExists(proposals, "HTMLDocument", 1, true)
    }

    @Test
    void testCompleteFullyQualifiedTypeInImports() {
        String contents = "import javax.swing.text.html.HTMLDocume"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "HTMLDocume"))
        proposalExists(proposals, "HTMLDocument", 1, true)
    }

    @Test
    void testCompletePackageInClass() {
        String contents = "class Foo { javax.swing.text.html.p }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ".p"))
        proposalExists(proposals, "javax.swing.text.html.parser", 1, true)
        // ensure no type proposals exist
        proposalExists(proposals, "Icons", 0, true)
    }

    @Test
    void testCompletePackageInMethod() {
        String contents = "class Foo { def x() { javax.swing.text.html.p } }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ".p"))
        proposalExists(proposals, "javax.swing.text.html.parser", 1, true)
        // ensure no type proposals exist
        proposalExists(proposals, "Icons", 0, true)
    }

    @Test
    void testCompletePackageInMethodParams() {
        String contents = "class Foo { def x(javax.swing.text.html.p ) { } }"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ".p"))
        proposalExists(proposals, "javax.swing.text.html.parser", 1, true)
        // ensure no type proposals exist
        proposalExists(proposals, "Icons", 0, true)
    }

    @Test
    void testCompletePackageInImports() {
        String contents = "import javax.swing.text.html.p"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ".p"))
        proposalExists(proposals, "javax.swing.text.html.parser", 1, true)
        // ensure no type proposals exist
        proposalExists(proposals, "Icons", 0, true)
    }

    @Test
    void testCompleteClass1() {
        String contents = "class Foo { }\n def x \n Foo.clas"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ".clas"))
        proposalExists(proposals, "class", 1, true)
    }

    @Test
    void testCompleteClass2() {
        String contents = "class Foo { }\n Foo.class.canonicalName"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ".canonicalName"))
        proposalExists(proposals, "canonicalName", 1, true)
    }

    @Test
    void testCompleteClass3() {
        String contents = "class Foo { }\n Foo.class.getCanonicalName"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ".getCanonicalName"))
        proposalExists(proposals, "getCanonicalName", 1, true)
    }

    @Test
    void testGRECLIPSE673() {
        String contents = "throw new MPE"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "MPE"))
        // found twice: once as a type proposal and once as a constructor proposal
        proposalExists(proposals, "MissingPropertyExceptionNoStack", 2, true)
    }

    @Test
    void testField1() {
        String contents = "class Foo {\n	JFr\n}"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "JFr"))
        proposalExists(proposals, "JFrame - javax.swing", 1, true)
    }

    @Test
    void testField2() {
        String contents = "class Foo {\n	private JFr\n}"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "JFr"))
        proposalExists(proposals, "JFrame - javax.swing", 1, true)
    }

    @Test
    void testField3() {
        String contents = "class Foo {\n	public JFr\n}"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "JFr"))
        proposalExists(proposals, "JFrame - javax.swing", 1, true)
    }

    @Test
    void testField4() {
        String contents = "class Foo {\n	protected JFr\n}"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "JFr"))
        proposalExists(proposals, "JFrame - javax.swing", 1, true)
    }

    @Test
    void testField5() {
        String contents = "class Foo {\n	public static JFr\n}"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "JFr"))
        proposalExists(proposals, "JFrame - javax.swing", 1, true)
    }

    @Test
    void testField6() {
        String contents = "class Foo {\n	public final JFr\n}"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "JFr"))
        proposalExists(proposals, "JFrame - javax.swing", 1, true)
    }

    @Test
    void testField7() {
        String contents = "class Foo {\n	public static final JFr\n}"
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, "JFr"))
        proposalExists(proposals, "JFrame - javax.swing", 1, true)
    }

    @Test
    void testTypeFilter1() {
        try {
            EclipseTestSetup.setJavaPreference(TYPEFILTER_ENABLED, "javax.swing.JFrame")
            ICompletionProposal[] proposals = createProposalsAtOffset("JFr", 2)
            proposalExists(proposals, "JFrame - javax.swing", 0, true)
        } finally {
            EclipseTestSetup.setJavaPreference(TYPEFILTER_ENABLED, "")
        }
    }

    @Test
    void testTypeFilter2() {
        try {
            EclipseTestSetup.setJavaPreference(TYPEFILTER_ENABLED, "javax.swing.*")
            ICompletionProposal[] proposals = createProposalsAtOffset("JFr", 2)
            proposalExists(proposals, "JFrame - javax.swing", 0, true)
        } finally {
            EclipseTestSetup.setJavaPreference(TYPEFILTER_ENABLED, "")
        }
    }
}
