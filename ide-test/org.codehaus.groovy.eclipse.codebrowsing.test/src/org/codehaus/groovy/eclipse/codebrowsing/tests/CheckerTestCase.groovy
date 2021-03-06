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
package org.codehaus.groovy.eclipse.codebrowsing.tests

import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.eclipse.codebrowsing.fragments.ASTFragmentFactory
import org.codehaus.groovy.eclipse.codebrowsing.fragments.IASTFragment
import org.codehaus.groovy.eclipse.core.compiler.GroovySnippetCompiler
import org.codehaus.groovy.eclipse.test.TestProject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName

abstract class CheckerTestCase {

    private TestProject testProject
    private GroovySnippetCompiler compiler

    @Rule
    public TestName test = new TestName()

    @Before
    final void setUpTestCase() {
        println '----------------------------------------'
        println 'Starting: ' + test.getMethodName()

        testProject = new TestProject()
        TestProject.setAutoBuilding(false)
        compiler = new GroovySnippetCompiler(testProject.getGroovyProjectFacade())
    }

    @After
    final void tearDownTestCase() {
        compiler.cleanup()
        testProject.dispose()
    }

    protected ModuleNode createModuleFromText(String text) {
        return compiler.compile(text)
    }

    protected IASTFragment getLastFragment(ModuleNode module) {
        Expression expr = getLastExpression(module)
        if (expr != null) {
            def factory = new ASTFragmentFactory()
            return factory.createFragment(expr)
        }
    }

    protected Expression getLastExpression(ModuleNode module) {
        Statement last = module.statementBlock.statements[-1]
        if (last instanceof ReturnStatement) {
            return ((ReturnStatement) last).getExpression()
        } else if (last instanceof ExpressionStatement) {
            return ((ExpressionStatement) last).getExpression()
        }
        assert false : 'Could not find expression in module'
    }
}
