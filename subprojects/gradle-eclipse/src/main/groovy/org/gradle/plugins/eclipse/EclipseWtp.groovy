/*
 * Copyright 2007-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.plugins.eclipse;


import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.listener.ActionBroadcast
import org.gradle.plugins.eclipse.model.Facet
import org.gradle.plugins.eclipse.model.WbProperty
import org.gradle.plugins.eclipse.model.WbResource
import org.gradle.plugins.eclipse.model.Wtp
import org.gradle.plugins.eclipse.model.internal.WtpFactory

/**
 * Generates Eclipse configuration files for Eclipse WTP.
 *
 * @author Hans Dockter
 */
public class EclipseWtp extends ConventionTask {
    /**
     * The file that is merged into the to be produced org.eclipse.wst.common.component file. This
     * file must not exist.
     */
    File orgEclipseWstCommonComponentInputFile

    @OutputFile
    /**
     * The output file for the org.eclipse.wst.common.component metadata.
     */
    File orgEclipseWstCommonComponentOutputFile

    /**
     * The file that is merged into the to be produced org.eclipse.wst.common.project.facet.core file. This
     * file must not exist.
     */
    File orgEclipseWstCommonProjectFacetCoreInputFile

    @OutputFile
    /**
     * The output file for the org.eclipse.wst.common.project.facet.core metadata.
     */
    File orgEclipseWstCommonProjectFacetCoreOutputFile

    /**
     * The source sets to be transformed into wb-resource elements.
     */
    Iterable<SourceSet> sourceSets

    /**
     * The configurations which files are to be transformed into dependent-module elements of
     * the org.eclipse.wst.common.component file.
     */
    Set<Configuration> plusConfigurations

    /**
     * The configurations which files are to be excluded from the dependent-module elements of
     * the org.eclipse.wst.common.component file.
     */
    Set<Configuration> minusConfigurations

    /**
     * The facets to be added as installed elements to the org.eclipse.wst.common.project.facet.core file.
     */
    List<Facet> facets = []

    /**
     * The deploy name to be used in the org.eclipse.wst.common.component file.
     */
    String deployName;

    /**
     * The variables to be used for replacing absolute path in dependent-module elements of
     * the org.eclipse.wst.common.component file.
     */
    Map<String, String> variables = [:]

    /**
     * Additional wb-resource elements.
     */
    List<WbResource> resources = []

    /**
     * Additional property elements.
     */
    List<WbProperty> properties = []

    protected WtpFactory modelFactory = new WtpFactory()

    def ActionBroadcast<Map<String, Node>> withXmlActions = new ActionBroadcast<Map<String, Node>>();
    def ActionBroadcast<Wtp> beforeConfiguredActions = new ActionBroadcast<Wtp>();
    def ActionBroadcast<Wtp> whenConfiguredActions = new ActionBroadcast<Wtp>();

    def EclipseWtp() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    protected void generateXml() {
        Wtp wtp = modelFactory.createWtp(this)
        wtp.toXml(orgEclipseWstCommonComponentOutputFile, orgEclipseWstCommonProjectFacetCoreOutputFile)
    }

    /**
     * Adds a facet for the org.eclipse.wst.common.project.facet.core file.
     *
     * @param args A map that must contain a name and version key with corresponding values.
     */
    void facet(Map args) {
        facets.add(new Facet(args.name, args.version))
    }

    /**
     * Adds variables to be used for replacing absolute path in dependent-module elements of
     * the org.eclipse.wst.common.component file.
     *
     * @param variables A map where the keys are the variable names and the values are the variable values.
     */
    void variables(Map variables) {
        assert variables != null
        this.variables.putAll variables
    }

    /**
     * Adds a property to be added to the org.eclipse.wst.common.component file.
     *
     * @param args A map that must contain a name and value key with corresponding values.
     */
    void property(Map args) {
        properties.add(new WbProperty(args.name, args.value))
    }

    /**
     * Adds a wb-resource to be added to the org.eclipse.wst.common.component file.
     *
     * @param args A map that must contain a deployPath and sourcePath key with corresponding values.
     */
    void resource(Map args) {
        resources.add(new WbResource(args.deployPath, args.sourcePath))
    }

    void withXml(Closure closure) {
        withXmlActions.add(closure);
    }

    void beforeConfigured(Closure closure) {
        beforeConfiguredActions.add(closure);
    }

    void whenConfigured(Closure closure) {
        whenConfiguredActions.add(closure);
    }
}
