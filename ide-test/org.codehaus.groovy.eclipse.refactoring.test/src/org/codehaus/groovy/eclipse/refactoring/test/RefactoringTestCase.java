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
package org.codehaus.groovy.eclipse.refactoring.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import org.codehaus.groovy.eclipse.refactoring.core.rename.ForcePreviewParticipant;
import org.codehaus.groovy.eclipse.refactoring.test.internal.TestRenameParticipantShared;
import org.codehaus.groovy.eclipse.refactoring.test.internal.TestRenameParticipantSingle;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.osgi.framework.FrameworkUtil;

public abstract class RefactoringTestCase extends TestCase {

    /**
     * If <code>true</code> a descriptor is created from the change.
     * The new descriptor is then used to create the refactoring again
     * and run the refactoring. As this is very time consuming this should
     * be <code>false</code> by default.
     */
    private static final boolean DESCRIPTOR_TEST= false;

    private IPackageFragmentRoot fRoot;
    private IPackageFragment fPackageP;

    public boolean fIsPreDeltaTest= false;

    public static final String TEST_PATH_PREFIX= "";

    protected static final String TEST_INPUT_INFIX= "/in/";
    protected static final String TEST_OUTPUT_INFIX= "/out/";
    protected static final String CONTAINER= "src";

    protected static final List<String> PROJECT_RESOURCE_CHILDREN= Arrays.asList(".project", ".classpath", ".settings");

    public RefactoringTestCase(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        fRoot= RefactoringTestSetup.getDefaultSourceFolder();
        fPackageP= RefactoringTestSetup.getPackageP();
        fIsPreDeltaTest= false;

        System.out.println("----------------------------------------");
        System.out.println("Test:" + getClass() + "." + getName());
        RefactoringCore.getUndoManager().flush();
        ForcePreviewParticipant.mute();
    }

    protected void performDummySearch() throws Exception {
        performDummySearch(getPackageP());
    }

    /**
     * Removes contents of {@link #getPackageP()}, of {@link #getRoot()} (except for p) and of the
     * Java project (except for src and the JRE library).
     *
     * @throws Exception in case of errors
     */
    @Override
    protected void tearDown() throws Exception {
        try {
            refreshFromLocal();
            performDummySearch();

            if (getPackageP().exists()){
                tryDeletingAllJavaChildren(getPackageP());
                tryDeletingAllNonJavaChildResources(getPackageP());
            }

            if (getRoot().exists()){
                IJavaElement[] packages= getRoot().getChildren();
                for (int i= 0; i < packages.length; i++){
                    IPackageFragment pack= (IPackageFragment)packages[i];
                    if (!pack.equals(getPackageP()) && pack.exists() && !pack.isReadOnly())
                        if (pack.isDefaultPackage())
                            pack.delete(true, null);
                        else
                            JavaProjectHelper.delete(pack.getResource()); // also delete packages with subpackages
                }
            }


            restoreTestProject();
        } finally {
            TestRenameParticipantShared.reset();
            TestRenameParticipantSingle.reset();
            ForcePreviewParticipant.unmute();

        }
    }

    private void restoreTestProject() throws Exception {
        IJavaProject javaProject= getRoot().getJavaProject();
        if (javaProject.exists()) {
            IClasspathEntry srcEntry= getRoot().getRawClasspathEntry();
            try {
                IClasspathEntry[] jreEntries= RefactoringTestSetup.getJRELibrariesAsRawClasspathEntry();
                IClasspathEntry[] cpes= javaProject.getRawClasspath();
                ArrayList<IClasspathEntry> newCPEs= new ArrayList<IClasspathEntry>();
                boolean cpChanged= false;
                for (int i= 0; i < cpes.length; i++) {
                    IClasspathEntry cpe= cpes[i];
                    boolean isJREEntry = false;
                    for (int j = 0; j < jreEntries.length; j++) {
                        if (cpe.equals(jreEntries[j])) {
                            isJREEntry = true;
                            break;
                        }
                    }
                    if (cpe.equals(srcEntry) || isJREEntry) {
                        newCPEs.add(cpe);
                    } else {
                        cpChanged= true;
                    }
                }
                if (cpChanged) {
                    IClasspathEntry[] newCPEsArray= newCPEs.toArray(new IClasspathEntry[newCPEs.size()]);
                    javaProject.setRawClasspath(newCPEsArray, null);
                }
            } catch (JavaModelException e) {
                System.err.println("Exception thrown when trying to restore project to original state.  We can probable ignore this.");
                e.printStackTrace();
            }

            Object[] nonJavaResources= javaProject.getNonJavaResources();
            for (int i= 0; i < nonJavaResources.length; i++) {
                Object kid= nonJavaResources[i];
                if (kid instanceof IResource) {
                    IResource resource= (IResource) kid;
                    if (! PROJECT_RESOURCE_CHILDREN.contains(resource.getName())) {
                        JavaProjectHelper.delete(resource);
                    }
                }
            }
        }
    }

    private void refreshFromLocal() throws Exception {
        if (getRoot().exists())
            getRoot().getResource().refreshLocal(IResource.DEPTH_INFINITE, null);
        else if (getPackageP().exists())//don't refresh package if root already refreshed
            getPackageP().getResource().refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    private static void tryDeletingAllNonJavaChildResources(IPackageFragment pack) throws Exception {
        Object[] nonJavaKids= pack.getNonJavaResources();
        for (int i= 0; i < nonJavaKids.length; i++) {
            if (nonJavaKids[i] instanceof IResource) {
                IResource resource= (IResource)nonJavaKids[i];
                JavaProjectHelper.delete(resource);
            }
        }
    }

    private static void tryDeletingAllJavaChildren(IPackageFragment pack) throws Exception {
        IJavaElement[] kids= pack.getChildren();
        for (int i= 0; i < kids.length; i++){
            if (kids[i] instanceof ISourceManipulation){
                if (kids[i].exists() && !kids[i].isReadOnly())
                    JavaProjectHelper.delete(kids[i]);
            }
        }
    }

    protected IPackageFragmentRoot getRoot() {
        return fRoot;
    }

    protected IPackageFragment getPackageP() {
        return fPackageP;
    }

    protected IJavaProject getProject() {
        return fRoot.getJavaProject();
    }

    protected final RefactoringStatus performRefactoring(RefactoringDescriptor descriptor) throws Exception {
        return performRefactoring(descriptor, true);
    }

    protected final RefactoringStatus performRefactoring(RefactoringDescriptor descriptor, boolean providesUndo) throws Exception {
        Refactoring refactoring= createRefactoring(descriptor);
        return performRefactoring(refactoring, providesUndo);
    }

    protected final Refactoring createRefactoring(RefactoringDescriptor descriptor) throws Exception {
        RefactoringStatus status= new RefactoringStatus();
        Refactoring refactoring= descriptor.createRefactoring(status);
        assertNotNull("refactoring should not be null", refactoring);
        assertTrue("status should be ok", status.isOK());
        return refactoring;
    }

    protected final RefactoringStatus performRefactoring(Refactoring ref, boolean performOnFail) throws Exception {
        return performRefactoring(ref, true, performOnFail);
    }

    protected final RefactoringStatus performRefactoring(Refactoring ref, boolean providesUndo, boolean performOnFail) throws Exception {
        performDummySearch();
        IUndoManager undoManager= getUndoManager();
        if (DESCRIPTOR_TEST){
            final CreateChangeOperation create= new CreateChangeOperation(
                    new CheckConditionsOperation(ref, CheckConditionsOperation.ALL_CONDITIONS),
                    RefactoringStatus.FATAL);
            create.run(new NullProgressMonitor());
            RefactoringStatus checkingStatus= create.getConditionCheckingStatus();
            if (checkingStatus.hasError())
                return checkingStatus;
            Change change= create.getChange();
            ChangeDescriptor descriptor= change.getDescriptor();
            if (descriptor instanceof RefactoringChangeDescriptor) {
                RefactoringChangeDescriptor rcd= (RefactoringChangeDescriptor) descriptor;
                RefactoringDescriptor refactoringDescriptor= rcd.getRefactoringDescriptor();
                if (refactoringDescriptor instanceof JavaRefactoringDescriptor) {
                    JavaRefactoringDescriptor jrd= (JavaRefactoringDescriptor) refactoringDescriptor;
                    RefactoringStatus validation= jrd.validateDescriptor();
                    if (validation.hasError() && !performOnFail)
                        return validation;
                    RefactoringStatus refactoringStatus= new RefactoringStatus();
                    Class<? extends JavaRefactoringDescriptor> expected= jrd.getClass();
                    RefactoringContribution contribution= RefactoringCore.getRefactoringContribution(jrd.getID());
                    jrd= (JavaRefactoringDescriptor) contribution.createDescriptor(jrd.getID(), jrd.getProject(), jrd.getDescription(), jrd.getComment(), contribution.retrieveArgumentMap(jrd), jrd.getFlags());
                    assertEquals(expected, jrd.getClass());
                    ref= jrd.createRefactoring(refactoringStatus);
                    if (refactoringStatus.hasError() && !performOnFail)
                        return refactoringStatus;
                    TestRenameParticipantSingle.reset();
                }
            }
        }
        final CreateChangeOperation create= new CreateChangeOperation(
            new CheckConditionsOperation(ref, CheckConditionsOperation.ALL_CONDITIONS),
            RefactoringStatus.FATAL);
        final PerformChangeOperation perform= new PerformChangeOperation(create);
        perform.setUndoManager(undoManager, ref.getName());
        IWorkspace workspace= ResourcesPlugin.getWorkspace();
        executePerformOperation(perform, workspace);
        RefactoringStatus status= create.getConditionCheckingStatus();
        if ((!status.hasError() && !performOnFail) || (status.hasError() && performOnFail))
            return status;
        assertTrue("Change wasn't executed", perform.changeExecuted());
        Change undo= perform.getUndoChange();
        if (providesUndo) {
            assertNotNull("Undo doesn't exist", undo);
            assertTrue("Undo manager is empty", undoManager.anythingToUndo());
        } else {
            assertNull("Undo manager contains undo but shouldn't", undo);
        }
        return null;
    }

    protected void executePerformOperation(final PerformChangeOperation perform, IWorkspace workspace) throws Exception {
        workspace.run(perform, new NullProgressMonitor());
    }

    public RefactoringStatus performRefactoringWithStatus(Refactoring ref, boolean performOnFail) throws Exception {
        RefactoringStatus status= performRefactoring(ref, performOnFail);
        if (status == null)
            return new RefactoringStatus();
        return status;
    }

    protected final Change performChange(Refactoring refactoring, boolean storeUndo) throws Exception {
        CreateChangeOperation create= new CreateChangeOperation(refactoring);
        PerformChangeOperation perform= new PerformChangeOperation(create);
        if (storeUndo) {
            perform.setUndoManager(getUndoManager(), refactoring.getName());
        }
        ResourcesPlugin.getWorkspace().run(perform, new NullProgressMonitor());
        assertTrue("Change wasn't executed", perform.changeExecuted());
        return perform.getUndoChange();
    }

    protected final Change performChange(final Change change) throws Exception {
        PerformChangeOperation perform= new PerformChangeOperation(change);
        ResourcesPlugin.getWorkspace().run(perform, new NullProgressMonitor());
        assertTrue("Change wasn't executed", perform.changeExecuted());
        return perform.getUndoChange();
    }

    protected IUndoManager getUndoManager() {
        IUndoManager undoManager= RefactoringCore.getUndoManager();
        undoManager.flush();
        return undoManager;
    }

    /* ===================  helpers  ================= */
    protected IType getType(ICompilationUnit cu, String name) throws Exception {
        IType[] types= cu.getAllTypes();
        for (int i= 0; i < types.length; i++)
            if (types[i].getTypeQualifiedName('.').equals(name) ||
                types[i].getElementName().equals(name))
                return types[i];
        return null;
    }

    /*
     * subclasses override to inform about the location of their test cases
     */
    protected String getRefactoringPath() {
        return "";
    }

    /*
     *  example "RenameType/"
     */
    protected String getTestPath() {
        return TEST_PATH_PREFIX + getRefactoringPath();
    }

    protected String createTestFileName(String cuName, String infix) {
        return getTestPath() + getName() + infix + cuName + ".groovy";
    }

    protected String getInputTestFileName(String cuName) {
        return createTestFileName(cuName, TEST_INPUT_INFIX);
    }

    /*
     * @param subDirName example "p/" or "org/eclipse/jdt/"
     */
    protected String getInputTestFileName(String cuName, String subDirName) {
        return createTestFileName(cuName, TEST_INPUT_INFIX + subDirName);
    }

    protected String getOutputTestFileName(String cuName) {
        return createTestFileName(cuName, TEST_OUTPUT_INFIX);
    }

    /*
     * @param subDirName example "p/" or "org/eclipse/jdt/"
     */
    protected String getOutputTestFileName(String cuName, String subDirName) {
        return createTestFileName(cuName, TEST_OUTPUT_INFIX + subDirName);
    }

    protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName) throws Exception {
        return createCUfromTestFile(pack, cuName, true);
    }

    protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName, String subDirName) throws Exception {
        return createCUfromTestFile(pack, cuName, subDirName, true);
    }

    protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName, boolean input) throws Exception {
        String contents= input
                    ? getFileContents(getInputTestFileName(cuName))
                    : getFileContents(getOutputTestFileName(cuName));
        return createCU(pack, cuName + ".groovy", contents);
    }

    protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName, String subDirName, boolean input) throws Exception {
        String contents= input
            ? getFileContents(getInputTestFileName(cuName, subDirName))
            : getFileContents(getOutputTestFileName(cuName, subDirName));

        return createCU(pack, cuName + ".groovy", contents);
    }

    protected void printTestDisabledMessage(String explanation){
        System.out.println("\n" +getClass().getName() + "::"+ getName() + " disabled (" + explanation + ")");
    }

    //-----------------------
    public static InputStream getStream(String content){
        return new ByteArrayInputStream(content.getBytes());
    }

    public static IPackageFragmentRoot getSourceFolder(IJavaProject javaProject, String name) throws Exception{
        IPackageFragmentRoot[] roots= javaProject.getPackageFragmentRoots();
        for (int i= 0; i < roots.length; i++) {
            if (! roots[i].isArchive() && roots[i].getElementName().equals(name))
                return roots[i];
        }
        return null;
    }

    public String getFileContents(String fileName) throws Exception {
        return getContents(getFileInputStream(fileName));
    }

    public static String getContents(IFile file) throws Exception {
        return getContents(file.getContents());
    }

    public static ICompilationUnit createCU(IPackageFragment pack, String name, String contents) throws Exception {
        assertTrue(!pack.getCompilationUnit(name).exists());
        ICompilationUnit cu= pack.createCompilationUnit(name, contents, true, null);
        cu.save(null, true);
        return cu;
    }

    public static String getContents(InputStream in) throws Exception {
        BufferedReader br= new BufferedReader(new InputStreamReader(in));

        StringBuffer sb= new StringBuffer(300);
        try {
            int read= 0;
            while ((read= br.read()) != -1)
                sb.append((char) read);
        } finally {
            br.close();
        }
        return sb.toString();
    }

    public InputStream getFileInputStream(String fileName) throws Exception {
        return FrameworkUtil.getBundle(getClass()).getEntry("/resources/" + fileName).openStream();
    }

    public static String removeExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    public static void performDummySearch(IJavaElement element) throws Exception{
        new SearchEngine().searchAllTypeNames(
            null,
            SearchPattern.R_EXACT_MATCH,
            "XXXXXXXXX".toCharArray(), // make sure we search a concrete name. This is faster according to Kent
            SearchPattern.R_EXACT_MATCH,
            IJavaSearchConstants.CLASS,
            SearchEngine.createJavaSearchScope(new IJavaElement[]{element}),
            new Requestor(),
            IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
            null);
    }

    public static IMember[] merge(IMember[] a1, IMember[] a2, IMember[] a3){
        return JavaElementUtil.merge(JavaElementUtil.merge(a1, a2), a3);
    }

    public static IMember[] merge(IMember[] a1, IMember[] a2){
        return JavaElementUtil.merge(a1, a2);
    }

    public static IField[] getFields(IType type, String[] names) {
        if (names == null )
            return new IField[0];
        Set<IField> fields= new HashSet<IField>();
        for (int i = 0; i < names.length; i++) {
            IField field= type.getField(names[i]);
            assertTrue("field " + field.getElementName() + " does not exist", field.exists());
            fields.add(field);
        }
        return fields.toArray(new IField[fields.size()]);
    }

    public static IType[] getMemberTypes(IType type, String[] names) {
        if (names == null )
            return new IType[0];
        Set<IType> memberTypes= new HashSet<IType>();
        for (int i = 0; i < names.length; i++) {
            IType memberType;
            if (names[i].indexOf('.') != -1) {
                String[] path= names[i].split("\\.");
                memberType= type.getType(path[0]);
                for (int j= 1; j < path.length; j++) {
                    memberType= memberType.getType(path[j]);
                }
            } else
                memberType= type.getType(names[i]);
            assertTrue("member type " + memberType.getElementName() + " does not exist", memberType.exists());
            memberTypes.add(memberType);
        }
        return memberTypes.toArray(new IType[memberTypes.size()]);
    }

    public static IMethod[] getMethods(IType type, String[] names, String[][] signatures) {
        if (names == null || signatures == null)
            return new IMethod[0];
        List<IMethod> methods= new ArrayList<IMethod>(names.length);
        for (int i = 0; i < names.length; i++) {
            IMethod method= type.getMethod(names[i], signatures[i]);
            assertTrue("method " + method.getElementName() + " does not exist", method.exists());
            if (!methods.contains(method))
                methods.add(method);
        }
        return methods.toArray(new IMethod[methods.size()]);
    }

    public static IType[] findTypes(IType[] types, String[] namesOfTypesToPullUp) {
        List<IType> found= new ArrayList<IType>(types.length);
        for (int i= 0; i < types.length; i++) {
            IType type= types[i];
            for (int j= 0; j < namesOfTypesToPullUp.length; j++) {
                String name= namesOfTypesToPullUp[j];
                if (type.getElementName().equals(name))
                    found.add(type);
            }
        }
        return found.toArray(new IType[found.size()]);
    }

    public static IField[] findFields(IField[] fields, String[] namesOfFieldsToPullUp) {
        List<IField> found= new ArrayList<IField>(fields.length);
        for (int i= 0; i < fields.length; i++) {
            IField field= fields[i];
            for (int j= 0; j < namesOfFieldsToPullUp.length; j++) {
                String name= namesOfFieldsToPullUp[j];
                if (field.getElementName().equals(name))
                    found.add(field);
            }
        }
        return found.toArray(new IField[found.size()]);
    }

    public static IMethod[] findMethods(IMethod[] selectedMethods, String[] namesOfMethods, String[][] signaturesOfMethods){
        List<IMethod> found= new ArrayList<IMethod>(selectedMethods.length);
        for (int i= 0; i < selectedMethods.length; i++) {
            IMethod method= selectedMethods[i];
            String[] paramTypes= method.getParameterTypes();
            for (int j= 0; j < namesOfMethods.length; j++) {
                String methodName= namesOfMethods[j];
                if (! methodName.equals(method.getElementName()))
                    continue;
                String[] methodSig= signaturesOfMethods[j];
                if (! areSameSignatures(paramTypes, methodSig))
                    continue;
                found.add(method);
            }
        }
        return found.toArray(new IMethod[found.size()]);
    }

    private static boolean areSameSignatures(String[] s1, String[] s2){
        if (s1.length != s2.length)
            return false;
        for (int i= 0; i < s1.length; i++) {
            if (! s1[i].equals(s2[i]))
                return false;
        }
        return true;
    }

    /**
     * Line-based version of junit.framework.Assert.assertEquals(String, String)
     * without considering line delimiters.
     * @param expected the expected value
     * @param actual the actual value
     */
    public static void assertEqualLines(String expected, String actual) {
        assertEqualLines("", expected, actual);
    }

    /**
     * Line-based version of junit.framework.Assert.assertEquals(String, String, String)
     * without considering line delimiters.
     * @param message the message
     * @param expected the expected value
     * @param actual the actual value
     */
    public static void assertEqualLines(String message, String expected, String actual) {
        String[] expectedLines= Strings.convertIntoLines(expected);
        String[] actualLines= Strings.convertIntoLines(actual);

        String expected2= (expectedLines == null ? null : Strings.concatenate(expectedLines, "\n"));
        String actual2= (actualLines == null ? null : Strings.concatenate(actualLines, "\n"));
        assertEquals(message, expected2, actual2);
    }

    private static class Requestor extends TypeNameRequestor {
    }
}
