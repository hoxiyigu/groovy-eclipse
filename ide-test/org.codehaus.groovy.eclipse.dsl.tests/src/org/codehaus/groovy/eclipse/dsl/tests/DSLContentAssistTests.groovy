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
package org.codehaus.groovy.eclipse.dsl.tests

import org.codehaus.groovy.eclipse.codeassist.tests.CompletionTestCase
import org.codehaus.groovy.eclipse.dsl.GroovyDSLCoreActivator
import org.codehaus.groovy.eclipse.test.EclipseTestSetup
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.junit.Before
import org.junit.Test

final class DSLContentAssistTests extends CompletionTestCase {

    private static final String COMMAND_CHAIN_NO_ARGS = '''\
        contribute(currentType('Inner')) {
          method name:'flart', noParens:true, type:'Inner'
        }
        '''.stripIndent()
    private static final String COMMAND_CHAIN_ONE_ARG = '''\
        contribute(currentType('Inner')) {
          method name:'flart', noParens:true, type:'Inner', params:[a:Integer]
        }
        '''.stripIndent()
    private static final String COMMAND_CHAIN_TWO_ARGS = '''\
        contribute(currentType('Inner')) {
          method name:'flart', noParens:true, type:'Inner', params:[a:Integer, b:String]
        }
        '''.stripIndent()
    private static final String NO_PARENS_FOR_DELEGATE = '''\
        contribute(currentType('Inner')) {
          delegatesTo type: 'Other', noParens: true
        }
        '''.stripIndent()
    private static final String SET_DELEGATE_ON_INT = '''\
        contribute(currentType(Integer) & enclosingCallName('foo')) {
          setDelegateType(String)
        }
        '''.stripIndent()

    @Before
    void setUp() {
        DSLInferencingTestCase.refreshExternalFoldersProject()
        EclipseTestSetup.addClasspathContainer(GroovyDSLCoreActivator.CLASSPATH_CONTAINER_ID)
        EclipseTestSetup.withProject { IProject project ->
            GroovyDSLCoreActivator.getDefault().getContextStoreManager().initialize(project, true)
          //GroovyDSLCoreActivator.getDefault().getContainerListener().ignoreProject(project)
        }
    }

    private String[] createDsls(String... dsls) {
        System.out.println("Now creating $dsls.length DSLD files.")
        int i = 0
        for (dsl in dsls) {
            println "Creating:\n$dsl"
            IFile file = EclipseTestSetup.addPlainText(dsl, "dsl${i++}.dsld")
            assert file.exists() : "File $file just created, but doesn't exist"
        }
        return dsls
    }

    //

    @Test
    void testDSLProposalFirstStaticField() {
        String contents = '''\
            @Singleton class Foo { static aaa }
            Foo.
            '''.stripIndent()
        ICompletionProposal[] proposals = orderByRelevance(createProposalsAtOffset(contents, getIndexOf(contents, '.')))
        assertProposalOrdering(proposals, 'instance', 'aaa')
    }

    @Test
    void testDSLProposalFirstStaticMethod() {
        String contents = '''\
            @Singleton class Foo { static aaa() { } }
            Foo.
            '''.stripIndent()
        ICompletionProposal[] proposals = orderByRelevance(createProposalsAtOffset(contents, getIndexOf(contents, '.')))
        assertProposalOrdering(proposals, 'getInstance', 'aaa')
    }

    @Test
    void testDSLProposalFirstMethod1() {
        String contents = '''\
            import groovy.swing.SwingBuilder
              new SwingBuilder().edt {
              delegate.x
            }
            '''.stripIndent()
        ICompletionProposal[] proposals = orderByRelevance(createProposalsAtOffset(contents, getIndexOf(contents, 'delegate.')))
        assertProposalOrdering(proposals, 'frame', 'registerBinding')
    }

    @Test
    void testDSLProposalFirstMethod2() {
        String contents = '''\
            import groovy.swing.SwingBuilder
              new SwingBuilder().edt {
            }
            '''.stripIndent()
        ICompletionProposal[] proposals = orderByRelevance(createProposalsAtOffset(contents, getIndexOf(contents, '{\n')))
        assertProposalOrdering(proposals, 'frame', 'registerBinding')
    }

    @Test // proposals should not exist since not applied to 'this'
    void testDSLProposalFirstMethod3() {
        String contents = '''\
            import groovy.swing.SwingBuilder
              new SwingBuilder().edt {
              this.x
            }
            '''.stripIndent()
        ICompletionProposal[] proposals = orderByRelevance(createProposalsAtOffset(contents, getIndexOf(contents, 'this.')))
        proposalExists(proposals, 'frame', 0)
        proposalExists(proposals, 'registerBinding', 0)
    }

    @Test // GRECLIPSE-1324
    void testEmptyClosure1() {
        createDsls(SET_DELEGATE_ON_INT)
        String contents = '''\
            1.foo {
              // here
            }
            '''.stripIndent()
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, '\n  '))
        // should see proposals from String, not Integer
        proposalExists(proposals, 'substring', 2)
        proposalExists(proposals, 'bytes', 1)
        proposalExists(proposals, 'abs', 0)
        proposalExists(proposals, 'capitalize', 1)
        proposalExists(proposals, 'digits', 0)
    }

    @Test // GRECLIPSE-1324
    void testEmptyClosure2() {
        createDsls(SET_DELEGATE_ON_INT)
        String contents = '''\
            1.foo {
              to
            }
            '''.stripIndent()
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ' to') + 1)
        // should see proposals from String, not Integer
        proposalExists(proposals, 'toUpperCase()', 1)
        proposalExists(proposals, 'toHexString()', 0)
    }

    @Test
    void testCommandChain1() {
        createDsls(COMMAND_CHAIN_NO_ARGS)
        String contents = '''\
            class Inner { }
            def val = new Inner()
            val.fla
            '''.stripIndent()
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, '.fla'))
        proposalExists(proposals, 'flart', 1)
        ICompletionProposal proposal = findFirstProposal(proposals, 'flart', false)
        applyProposalAndCheck(new Document(contents), proposal, contents.replace('val.fla', 'val.flart'))
    }

    @Test
    void testCommandChain2() {
        createDsls(COMMAND_CHAIN_NO_ARGS)
        String contents = '''\
            class Inner { }
            def val = new Inner()
            val.flart foo fl
            '''.stripIndent()
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ' fl'))
        proposalExists(proposals, 'flart', 1)
        ICompletionProposal proposal = findFirstProposal(proposals, 'flart', false)
        applyProposalAndCheck(new Document(contents), proposal, contents.replace(' fl', ' flart'))
    }

    @Test
    void testCommandChain3() {
        createDsls(COMMAND_CHAIN_NO_ARGS)
        String contents = '''\
            class Inner { }
            def val = new Inner()
            val.flart foo, baz fl
            '''.stripIndent()
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ' fl'))
        proposalExists(proposals, 'flart', 1)
        ICompletionProposal proposal = findFirstProposal(proposals, 'flart', false)
        applyProposalAndCheck(new Document(contents), proposal, contents.replace(' fl', ' flart'))
    }

    @Test
    void testCommandChain4() {
        createDsls(COMMAND_CHAIN_ONE_ARG)
        String contents = '''\
            class Inner { }
            def val = new Inner()
            val.flart foo, baz fl
            '''.stripIndent()
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ' fl'))
        proposalExists(proposals, 'flart', 1)
        ICompletionProposal proposal = findFirstProposal(proposals, 'flart', false)
        applyProposalAndCheck(new Document(contents), proposal, contents.replace(' fl', ' flart 0 '))
    }

    @Test
    void testCommandChain5() {
        createDsls(COMMAND_CHAIN_TWO_ARGS)
        String contents = '''\
            class Inner { }
            def val = new Inner()
            val.flart foo, baz fl
            '''.stripIndent()
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, ' fl'))
        proposalExists(proposals, 'flart', 1)
        ICompletionProposal proposal = findFirstProposal(proposals, 'flart', false)
        applyProposalAndCheck(new Document(contents), proposal, contents.replace(' fl', ' flart 0, "" '))
    }

    @Test
    void testDelegatesToNoParens1() {
        createDsls(NO_PARENS_FOR_DELEGATE)
        String contents = '''\
            class Other {
              def blart(a, b, c) { }
              def flart(a) { }
            }
            class Inner { }
            def val = new Inner()
            val.bl
            '''.stripIndent()
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, 'val.bl'))
        ICompletionProposal proposal = findFirstProposal(proposals, 'blart', false)
        applyProposalAndCheck(new Document(contents), proposal, contents.replace('val.bl', 'val.blart val, val, val '))
    }

    @Test
    void testDelegatesToNoParens2() {
        createDsls(NO_PARENS_FOR_DELEGATE)
        String contents = '''\
            class Other {
              def blart(a, b, c) { }
              def flart(a) { }
            }
            class Inner { }
            def val = new Inner()
            val.fl
            '''.stripIndent()
        ICompletionProposal[] proposals = createProposalsAtOffset(contents, getIndexOf(contents, 'val.fl'))
        ICompletionProposal proposal = findFirstProposal(proposals, 'flart', false)
        applyProposalAndCheck(new Document(contents), proposal, contents.replace('val.fl', 'val.flart val '))
    }
}
