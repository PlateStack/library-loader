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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final public class MavenArtifact
{
    private final @NotNull String group;
    private final @NotNull String artifact;
    private final @NotNull String version;

    public MavenArtifact(final @NotNull String group, final @NotNull String artifact, final @NotNull String version)
    {
        this.group = Objects.requireNonNull(group, "Group can't be null").trim();
        this.artifact = Objects.requireNonNull(artifact, "Artifact can't be null").trim();
        this.version = Objects.requireNonNull(version, "Version can't be null").trim();

        if(group.isEmpty()) throw new IllegalArgumentException("Group can't be empty");
        if(artifact.isEmpty()) throw new IllegalArgumentException("Artifact can't be empty");
        if(version.isEmpty()) throw new IllegalArgumentException("Version can't be empty");
    }

    @NotNull
    public final String getGroup()
    {
        return group;
    }

    @NotNull
    public final String getArtifact()
    {
        return artifact;
    }

    @NotNull
    public final String getVersion()
    {
        return version;
    }

    @NotNull
    @Override
    public final String toString()
    {
        return "MavenArtifact{" +
                "group='" + group + '\'' +
                ", artifact='" + artifact + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    @Override
    public final boolean equals(@Nullable Object o)
    {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        MavenArtifact that = (MavenArtifact) o;
        return Objects.equals(group, that.group) &&
                Objects.equals(artifact, that.artifact) &&
                Objects.equals(version, that.version);
    }

    @Override
    public final int hashCode()
    {
        return Objects.hash(group, artifact, version);
    }
}
