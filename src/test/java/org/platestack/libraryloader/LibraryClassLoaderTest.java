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

import org.junit.Test;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

public class LibraryClassLoaderTest
{
    @Test
    public void createClassLoader() throws Exception
    {
        Path tempDirectory = Files.createTempDirectory("downloads_");
        try
        {
            URLClassLoader loader = LibraryClassLoader.createClassLoader(tempDirectory, new Dependency[]{
                    new Dependency("junit", "junit", "4.12")
            });
            System.out.println(Arrays.toString(loader.getURLs()));
        }
        finally
        {
            Files.walk(tempDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(System.out::println)
                    .forEach(File::delete);
        }
    }

}