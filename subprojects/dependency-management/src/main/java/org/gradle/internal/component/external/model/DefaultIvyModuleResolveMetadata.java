/*
 * Copyright 2014 the original author or authors.
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
package org.gradle.internal.component.external.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint;
import org.gradle.api.internal.artifacts.ivyservice.NamespaceId;
import org.gradle.internal.component.external.descriptor.Artifact;
import org.gradle.internal.component.external.descriptor.Configuration;
import org.gradle.internal.component.model.ConfigurationMetadata;
import org.gradle.internal.component.model.Exclude;
import org.gradle.internal.component.model.ModuleSource;
import org.gradle.util.CollectionUtils;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultIvyModuleResolveMetadata extends AbstractModuleComponentResolveMetadata<IvyConfigurationMetadata> implements IvyModuleResolveMetadata {
    private final ImmutableMap<String, Configuration> configurationDefinitions;
    private final ImmutableList<Artifact> artifactDefinitions;
    private final ImmutableList<Exclude> excludes;
    private final ImmutableMap<NamespaceId, String> extraAttributes;
    private final ImmutableList<? extends ComponentVariant> variants;
    private final ImmutableList<? extends ConfigurationMetadata> graphVariants;
    private final String branch;
    // Since a single `Artifact` is shared between configurations, share the metadata type as well.
    private Map<Artifact, ModuleComponentArtifactMetadata> artifacts;

    DefaultIvyModuleResolveMetadata(DefaultMutableIvyModuleResolveMetadata metadata) {
        super(metadata);
        this.configurationDefinitions = metadata.getConfigurationDefinitions();
        this.branch = metadata.getBranch();
        this.artifactDefinitions = metadata.getArtifactDefinitions();
        this.excludes = metadata.getExcludes();
        this.extraAttributes = metadata.getExtraAttributes();
        this.variants = metadata.getVariants();
        this.graphVariants = metadata.getVariantsForGraphTraversal();
    }

    private DefaultIvyModuleResolveMetadata(DefaultIvyModuleResolveMetadata metadata, ModuleSource source) {
        super(metadata, source);
        this.configurationDefinitions = metadata.configurationDefinitions;
        this.branch = metadata.branch;
        this.artifactDefinitions = metadata.artifactDefinitions;
        this.excludes = metadata.excludes;
        this.extraAttributes = metadata.extraAttributes;
        this.variants = metadata.variants;
        this.graphVariants = metadata.graphVariants;
    }

    @Override
    protected IvyConfigurationMetadata createConfiguration(ModuleComponentIdentifier componentId, String name, boolean transitive, boolean visible, ImmutableList<IvyConfigurationMetadata> parents) {
        Set<ModuleComponentArtifactMetadata> artifacts = new LinkedHashSet<ModuleComponentArtifactMetadata>();
        collectArtifactsFor(name, artifacts);
        for (IvyConfigurationMetadata parent : parents) {
            artifacts.addAll(parent.getArtifacts());
        }

        return new IvyConfigurationMetadata(componentId, name, transitive, visible, parents, excludes, ImmutableList.copyOf(artifacts));
    }

    private void collectArtifactsFor(String name, Collection<ModuleComponentArtifactMetadata> dest) {
        if (artifacts == null) {
            artifacts = new IdentityHashMap<Artifact, ModuleComponentArtifactMetadata>();
        }
        for (Artifact artifact : artifactDefinitions) {
            if (artifact.getConfigurations().contains(name)) {
                ModuleComponentArtifactMetadata artifactMetadata = artifacts.get(artifact);
                if (artifactMetadata == null) {
                    artifactMetadata = new DefaultModuleComponentArtifactMetadata(getComponentId(), artifact.getArtifactName());
                    artifacts.put(artifact, artifactMetadata);
                }
                dest.add(artifactMetadata);
            }
        }
    }

    @Override
    public DefaultIvyModuleResolveMetadata withSource(ModuleSource source) {
        return new DefaultIvyModuleResolveMetadata(this, source);
    }

    @Override
    public MutableIvyModuleResolveMetadata asMutable() {
        return new DefaultMutableIvyModuleResolveMetadata(this);
    }

    @Override
    public ImmutableMap<String, Configuration> getConfigurationDefinitions() {
        return configurationDefinitions;
    }

    @Override
    public ImmutableList<Artifact> getArtifactDefinitions() {
        return artifactDefinitions;
    }

    @Override
    public ImmutableList<Exclude> getExcludes() {
        return excludes;
    }

    public String getBranch() {
        return branch;
    }

    public ImmutableMap<NamespaceId, String> getExtraAttributes() {
        return extraAttributes;
    }

    @Override
    public ImmutableList<? extends ComponentVariant> getVariants() {
        return variants;
    }

    @Override
    public ImmutableList<? extends ConfigurationMetadata> getVariantsForGraphTraversal() {
        return graphVariants;
    }

    @Override
    public IvyModuleResolveMetadata withDynamicConstraintVersions() {
        List<ModuleDependencyMetadata> transformed = CollectionUtils.collect(getDependencies(), new Transformer<ModuleDependencyMetadata, ModuleDependencyMetadata>() {
            @Override
            public ModuleDependencyMetadata transform(ModuleDependencyMetadata dependency) {
                if (dependency instanceof IvyDependencyMetadata) {
                    String dynamicConstraintVersion = ((IvyDependencyMetadata) dependency).getDynamicConstraintVersion();
                    return dependency.withRequestedVersion(new DefaultMutableVersionConstraint(dynamicConstraintVersion));
                }

                return dependency;
            }
        });
        // TODO:DAZ Should "just" create a new instance here to avoid the setDependencies() call
        MutableIvyModuleResolveMetadata mutableMetadata = asMutable();
        mutableMetadata.setDependencies(transformed);
        return mutableMetadata.asImmutable();
    }
}
