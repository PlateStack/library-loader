/*
 *  Copyright (C) 2017 José Roberto de Araújo Júnior
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.platestack.libraryloader.ivy;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final public class LibraryResolver
{
    @NotNull
    private static final LibraryResolver instance = new LibraryResolver();

    @NotNull
    public static LibraryResolver getInstance()
    {
        return instance;
    }

    @NotNull
    private final IvySettings settings = new IvySettings();

    private LibraryResolver()
    {
        //addMavenRepository("Maven Central", "http://repo1.maven.org/maven2/");
        //addMavenRepository("JCenter", "https://jcenter.bintray.com/");

        ChainResolver chain = new ChainResolver();
        chain.setName("platestack chain");

        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setName("central");
        resolver.setM2compatible(true);
        resolver.setUsepoms(true);
        resolver.setUseMavenMetadata(true);
        chain.add(resolver);

        resolver = new IBiblioResolver();
        resolver.setName("jcenter");
        resolver.setM2compatible(true);
        resolver.setUsepoms(true);
        resolver.setUseMavenMetadata(true);
        resolver.setRoot("https://jcenter.bintray.com/");
        chain.add(resolver);

        resolver = new IBiblioResolver();
        resolver.setName("herd");
        resolver.setM2compatible(true);
        resolver.setUsepoms(true);
        resolver.setUseMavenMetadata(true);
        resolver.setRoot("https://modules.ceylon-lang.org/maven/1/");
        chain.add(resolver);

        settings.addResolver(chain);
        settings.setDefaultResolver(chain.getName());
    }

    public static void setUserDir(final File file)
    {
        getInstance().settings.setDefaultIvyUserDir(file);
    }

    public static List<MavenArtifact> readArtifacts(final Path file) throws IOException
    {
        return readArtifacts(Files.readAllLines(file).stream());
    }

    public static List<MavenArtifact> readArtifacts(final InputStream stream) throws IOException
    {
        return readArtifacts(new BufferedReader(new InputStreamReader(stream, "UTF-8")).lines());
    }

    public static List<MavenArtifact> readArtifacts(final Stream<String> lines)
    {
        return lines
                .map(line-> line.replaceAll("#.*", ""))
                .map(String::trim).filter(line-> !line.isEmpty())
                .map(line -> Arrays.stream(line.split(":", 3)).map(String::trim).toArray(String[]::new))
                .filter(parts-> parts.length == 3 && Arrays.stream(parts).noneMatch(String::isEmpty))
                .map(parts-> new MavenArtifact(parts[0], parts[1], parts[2]))
                .collect(Collectors.toList());
    }

    @NotNull
    final public synchronized List<File> resolve(@NotNull final MavenArtifact project, @NotNull MavenArtifact... dependencies)
            throws IOException, ParseException
    {
        return resolve(project, Arrays.stream(dependencies).collect(Collectors.toSet()));
    }

    @NotNull
    final public synchronized List<File> resolve(@NotNull final MavenArtifact project, @NotNull Iterable<MavenArtifact> dependencies)
            throws IOException, ParseException
    {
        return resolve(project, StreamSupport.stream(dependencies.spliterator(), false).collect(Collectors.toSet()));
    }

    @NotNull
    final public synchronized List<File> resolve(@NotNull final MavenArtifact project, @NotNull Set<MavenArtifact> dependencies)
            throws IOException, ParseException
    {
        return Arrays.stream(callIvy(project, dependencies)).map(ArtifactDownloadReport::getLocalFile).collect(Collectors.toList());
    }

    @NotNull
    final public synchronized List<MavenArtifact> dependencies(@NotNull final MavenArtifact project, @NotNull Set<MavenArtifact> dependencies)
            throws IOException, ParseException
    {
        return Arrays.stream(callIvy(project, dependencies))
                .map(ArtifactDownloadReport::getArtifact).map(Artifact::getId)
                .map(id-> new MavenArtifact(id.getModuleRevisionId().getOrganisation(), id.getName(), id.getRevision()))
                .collect(Collectors.toList());
    }

    @NotNull
    private synchronized ArtifactDownloadReport[] callIvy(@NotNull final MavenArtifact project, @NotNull Set<MavenArtifact> dependencies)
            throws IOException, ParseException
    {
        if(dependencies.isEmpty())
            return new ArtifactDownloadReport[0];

        final Ivy ivy = Ivy.newInstance(settings);

        final File ivyFile = File.createTempFile("ivy", ".xml");
        ivyFile.deleteOnExit();

        final DefaultModuleDescriptor md =
                DefaultModuleDescriptor.newDefaultInstance(
                        ModuleRevisionId.newInstance(project.getGroup(), project.getArtifact(), project.getVersion())
                );

        dependencies.forEach(dependency ->
                md.addDependency(
                        new DefaultDependencyDescriptor(md,
                                ModuleRevisionId.newInstance(dependency.getGroup(), dependency.getArtifact(), dependency.getVersion()),
                                false, false,true
                        )
                )
        );

        XmlModuleDescriptorWriter.write(md, ivyFile);

        final ResolveOptions resolveOptions = new ResolveOptions().setConfs(new String[]{"default"});

        final ResolveReport report = ivy.resolve(ivyFile.toURI().toURL(), resolveOptions);

        return report.getAllArtifactsReports();
    }
}
