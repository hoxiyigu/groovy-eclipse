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
package org.codehaus.groovy.eclipse.codeassist.tests

import org.codehaus.groovy.eclipse.GroovyPlugin
import org.codehaus.groovy.eclipse.codeassist.requestor.GroovyCompletionProposalComputer
import org.codehaus.groovy.eclipse.core.preferences.PreferenceConstants
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Tests that completion proposals are sufficiently groovy-like in their output.
 */
final class GroovyLikeCompletionTests extends CompletionTestCase {

    private static final String SCRIPTCONTENTS =
        "any\n" +
        "clone\n" +
        "findIndexOf\n" +
        "inject\n" +
        "class Foo {\n" +
        "  Foo(first, second) { }\n" +
        "  Foo(int third) { }\n" +
        "  def method1(arg) { }\n" +
        "  def method2(arg, Closure c1) { }\n" +
        "  def method3(arg, Closure c1, Closure c2) { }\n" +
        "}\n" +
        "new Foo()"
    private final static String CLOSURE_CONTENTS =
        "class Other {\n" +
        "    def first\n" +
        "    def second2() { } \n" +
        "}\n" +
        " \n" +
        "class MyOtherClass extends Other {\n" +
        "    def meth() {\n" +
        "        \"\".foo {\n" +
        "            substring(0)\n" +  // should find
        "            first\n" +  // should find
        "            second2()\n" +  // should find
        "            delegate.substring(0)\n" +  // should find
        "            delegate.first(0)\n" + // should not find
        "            delegate.second2(0)\n" + // should not find
        "            this.substring(0)\n" + // should not find
        "            this.first(0)\n" + // should find
        "            this.second2(0)\n" +  // should find
        "            wait\n" +  // should find 2 only
        "        }\n" +
        "    }\n" +
        "}"
    private final static String CLOSURE_CONTENTS2 =
        "class Other {\n" +
        "    def first\n" +
        "    def second2() { } \n" +
        "}\n" +
        "class Other2 extends Other { }\n" +
        "class MyOtherClass extends Other {\n" +
        "    def meth() {\n" +
        "        new Other2().foo {\n" +
        "            first\n" +  // should find 2 only
        "        }\n" +
        "    }\n" +
        "}"

    @Before
    void setUp() {
        IPreferenceStore prefs = GroovyPlugin.getDefault().getPreferenceStore()
        prefs.setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_BRACKETS, true)
        prefs.setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_NOPARENS, true)
        prefs.setValue(PreferenceConstants.GROOVY_CONTENT_NAMED_ARGUMENTS, false)
        prefs.setValue(PreferenceConstants.GROOVY_CONTENT_PARAMETER_GUESSING, false)
    }

    @After
    void tearDown() {
        IPreferenceStore prefs = GroovyPlugin.getDefault().getPreferenceStore()
        prefs.setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_BRACKETS, true)
        prefs.setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_NOPARENS, true)
        prefs.setValue(PreferenceConstants.GROOVY_CONTENT_NAMED_ARGUMENTS, false)
        prefs.setValue(PreferenceConstants.GROOVY_CONTENT_PARAMETER_GUESSING, true)
    }

    @Test
    void testMethodWithClosure() {
        ICompilationUnit unit = addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        ICompletionProposal[] proposals = performContentAssist(unit, getIndexOf(SCRIPTCONTENTS, "any"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "any {  }", 1)
    }

    @Test
    void testMethodWithNoArgs() {
        ICompilationUnit unit = addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        ICompletionProposal[] proposals = performContentAssist(unit, getIndexOf(SCRIPTCONTENTS, "clone"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "clone()", 1)
    }

    @Test
    void testMethodWith2Args() {
        ICompilationUnit unit = addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        ICompletionProposal[] proposals = performContentAssist(unit, getIndexOf(SCRIPTCONTENTS, "findIndexOf"), GroovyCompletionProposalComputer)
        checkReplacementRegexp(proposals, "findIndexOf\\(\\w+\\) \\{  \\}", 1)
    }

    @Test
    void testMethodWithClosureNotGroovyLike() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_BRACKETS, false)
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_NOPARENS, false)
        ICompilationUnit unit = addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        ICompletionProposal[] proposals = performContentAssist(unit, getIndexOf(SCRIPTCONTENTS, "any"), GroovyCompletionProposalComputer)
        checkReplacementRegexp(proposals, "any\\(\\w+\\)", 1)
    }

    @Test
    void testMethodWith2ArgsNotGroovyLike() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_BRACKETS, false)
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_NOPARENS, false)
        ICompilationUnit unit = addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        ICompletionProposal[] proposals = performContentAssist(unit, getIndexOf(SCRIPTCONTENTS, "findIndexOf"), GroovyCompletionProposalComputer)
        checkReplacementRegexp(proposals, "findIndexOf\\(\\w+, \\w+\\)", 1)
    }

    @Test
    void testClosureApplication1a() {
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method1"
        String expected = "new Foo().method1(arg)"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method1")
    }

    @Test
    void testClosureApplication1b() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_NOPARENS, false)
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method1"
        String expected = "new Foo().method1(arg)"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method1")
    }

    @Test
    void testClosureApplication1c() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_BRACKETS, false)
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method1"
        String expected = "new Foo().method1(arg)"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method1")
    }

    @Test
    void testClosureApplication1d() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_BRACKETS, false)
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_NOPARENS, false)
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method1"
        String expected = "new Foo().method1(arg)"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method1")
    }

    @Test
    void testClosureApplication2a() {
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method2"
        String expected = "new Foo().method2(arg) {  }"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method2")
    }

    @Test
    void testClosureApplication2b() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_NOPARENS, false)
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method2"
        String expected = "new Foo().method2(arg, {  })"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method2")
    }

    @Test
    void testClosureApplication2c() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_BRACKETS, false)
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method2"
        String expected = "new Foo().method2(arg) c1"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method2")
    }

    @Test
    void testClosureApplication2d() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_BRACKETS, false)
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_NOPARENS, false)
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method2"
        String expected = "new Foo().method2(arg, c1)"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method2")
    }

    @Test
    void testClosureApplication3a() {
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method3"
        String expected = "new Foo().method3(arg, {  }) {  }"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method3")
    }

    @Test
    void testClosureApplication3b() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_NOPARENS, false)
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method3"
        String expected = "new Foo().method3(arg, {  }, {  })"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method3")
    }

    @Test
    void testClosureApplication3c() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_BRACKETS, false)
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method3"
        String expected = "new Foo().method3(arg, c1) c2"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method3")
    }

    @Test
    void testClosureApplication3d() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_BRACKETS, false)
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_ASSIST_NOPARENS, false)
        addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        String contents = "new Foo().method3"
        String expected = "new Foo().method3(arg, c1, c2)"
        checkProposalApplicationNonType(contents, expected, contents.length(), "method3")
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion1() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS, " substring"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "substring(beginIndex)", 1)
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion2() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS, " first"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "first", 1)
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion3() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS, " second2"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "second2()", 1)
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion4() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS, "delegate.substring"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "substring(beginIndex)", 1)
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion5() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS, "delegate.first"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "first", 0)
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion6() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS, "delegate.second2"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "second2", 0)
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion7() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS, "this.substring"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "substring", 0)
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion8() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS, "this.first"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "first", 1)
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion9() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS, "this.second2"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "second2()", 1)
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion10() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS, "wait"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "wait()", 1)
    }

    @Test // accessing members of super types in closures
    void testClosureCompletion11() {
        ICompilationUnit groovyUnit = addGroovySource(CLOSURE_CONTENTS2, "File", "")
        ICompletionProposal[] proposals = performContentAssist(groovyUnit, getLastIndexOf(CLOSURE_CONTENTS2, "first"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "first", 1)
    }

    @Test
    void testNamedArguments0() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_NAMED_ARGUMENTS, true)
        ICompilationUnit unit = addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        ICompletionProposal[] proposals = performContentAssist(unit, getIndexOf(SCRIPTCONTENTS, "clone"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "clone()", 1)
    }

    @Ignore @Test
    void testNamedArguments1() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_NAMED_ARGUMENTS, true)
        ICompilationUnit unit = addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        ICompletionProposal[] proposals = performContentAssist(unit, getIndexOf(SCRIPTCONTENTS, "new Foo"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "(first:first, second:second)", 1)
    }

    @Ignore @Test
    void testNamedArguments2() {
        GroovyPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.GROOVY_CONTENT_NAMED_ARGUMENTS, true)
        ICompilationUnit unit = addGroovySource(SCRIPTCONTENTS, "GroovyLikeCompletions", "")
        ICompletionProposal[] proposals = performContentAssist(unit, getIndexOf(SCRIPTCONTENTS, "new Foo"), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "(third:third)", 1)
    }

    @Ignore @Test // GRECLIPSE-268
    void testGString1() {
        ICompilationUnit unit = addGroovySource('""""""', "File", "")
        ICompletionProposal[] proposals = performContentAssist(unit, "\"\"\"".length(), GroovyCompletionProposalComputer)
        assert proposals.length == 0 : "Should not have found any proposals, but found:\n" + printProposals(proposals)
    }

    @Test // GRECLIPSE-268
    void testGString2() {
        ICompilationUnit unit = addGroovySource('"""${this}"""', "File", "")
        ICompletionProposal[] proposals = performContentAssist(unit, "\"\"\"".length(), GroovyCompletionProposalComputer)
        assert proposals.length == 0 : "Should not have found any proposals, but found:\n" + printProposals(proposals)
    }

    @Test // GRECLIPSE-268
    void testGString3() {
        ICompilationUnit unit = addGroovySource('"""this"""', "File", "")
        ICompletionProposal[] proposals = performContentAssist(unit, "\"\"\"this".length(), GroovyCompletionProposalComputer)
        assert proposals.length == 0 : "Should not have found any proposals, but found:\n" + printProposals(proposals)
    }

    @Test // GRECLIPSE-268
    void testGString4() {
        String contents = 'def flarb;\n"""${flarb}"""'
        ICompilationUnit unit = addGroovySource(contents, "File", "")
        ICompletionProposal[] proposals = performContentAssist(unit, getIndexOf(contents, '${flarb'), GroovyCompletionProposalComputer)
        checkReplacementString(proposals, "flarb", 1)
    }
}
