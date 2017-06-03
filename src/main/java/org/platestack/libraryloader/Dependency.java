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

package org.platestack.libraryloader;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Dependency
{
    @NotNull private final String group;
    @NotNull private final String artifact;
    @NotNull private final String version;

    public Dependency(final @NotNull String group, final @NotNull String artifact, final @NotNull String version)
    {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
    }

    @NotNull
    public String getGroup()
    {
        return group;
    }

    @NotNull
    public String getArtifact()
    {
        return artifact;
    }

    @NotNull
    public String getVersion()
    {
        return version;
    }

    @NotNull
    public String getPath()
    {
        return group.replace('.','/')+'/'+artifact+'/'+version+'/';
    }

    @NotNull
    public String getJarPath()
    {
        return getPath()+getJarName();
    }

    @NotNull
    public String getJarName()
    {
        return artifact+'-'+version+".jar";
    }

    @Override
    public String toString()
    {
        return "Dependency{" +
                "group='" + group + '\'' +
                ", artifact='" + artifact + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(group, that.group) &&
                Objects.equals(artifact, that.artifact) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(group, artifact, version);
    }
}
