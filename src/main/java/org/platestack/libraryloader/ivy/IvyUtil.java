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
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.resolver.URLResolver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class IvyUtil
{
    public static void maina(String[] args) throws IOException, ParseException
    {
        //compile group: 'org.jetbrains.kotlin', name: 'kotlin-stdlib', version: '1.1.2-4'
        //System.out.println(resolveArtifact( "org.jetbrains.kotlin", "kotlin-stdlib", "[1.1.2-4]" ));
        MavenArtifact project = new MavenArtifact("org.platestack.runtime.plugin", "a_cool_plugin", "0.1.0-SNAPSHOT");
        /*
        List<File> files = LibraryResolver.getInstance().resolve(project,
                new MavenArtifact("org.jetbrains.kotlin", "kotlin-stdlib-jre8", "1.1.2-4"),
                new MavenArtifact("org.jetbrains.kotlin", "kotlin-reflect", "1.1.2-4"),
                //new MavenArtifact("org.platestack", "immutable-collections", "0.1.0"),
                new MavenArtifact("com.github.salomonbrys.kotson", "kotson", "2.5.0"),
                new MavenArtifact("io.github.microutils", "kotlin-logging", "1.4.4"),
                new MavenArtifact("org.ow2.asm", "asm", "5.2")
        );
        */
        List<MavenArtifact> dependencies = LibraryResolver.readArtifacts(new File("D:\\_InteliJ\\org.platestack\\plate-bukkit\\plate-common\\plate-api\\src\\main\\required-libraries.list").toPath());
        List<File> files = LibraryResolver.getInstance().resolve(project, dependencies);
        files.forEach(System.out::println);
    }

    public static File resolveArtifact(String groupId, String artifactId, String version) throws IOException, ParseException
    {
        IvySettings ivySettings = new IvySettings();

        URLResolver resolver = new URLResolver();
        resolver.setM2compatible(true);
        resolver.setName("Maven Central");
        resolver.addArtifactPattern("http://repo1.maven.org/maven2/[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]");
        ivySettings.addResolver(resolver);

        resolver = new URLResolver();
        resolver.setM2compatible(true);
        resolver.setName("JCenter");
        resolver.addArtifactPattern("https://jcenter.bintray.com/[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]");
        ivySettings.addResolver(resolver);
        ivySettings.setDefaultResolver(resolver.getName());

        Ivy ivy = Ivy.newInstance(ivySettings);

        File ivyFile = File.createTempFile("ivy", ".xml");
        ivyFile.deleteOnExit();

        String[] dep = new String[]{groupId, artifactId, version};

        DefaultModuleDescriptor md =
                DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(
                        dep[0],dep[1] + "-caller", "working"));

        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md,
                ModuleRevisionId.newInstance(dep[0], dep[1], dep[2]), false, false, true);
        md.addDependency(dd);

        XmlModuleDescriptorWriter.write(md, ivyFile);

        String[] confs = new String[]{"default"};
        ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs);

        //init resolve report
        ResolveReport report = ivy.resolve(ivyFile.toURI().toURL(), resolveOptions);

        File jarArtifactFile = report.getAllArtifactsReports()[0].getLocalFile();

        return jarArtifactFile;
    }
}
