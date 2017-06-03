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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LibraryClassLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryClassLoader.class);

    @NotNull
    private static Optional<String> readSingleLine(@NotNull Path file)
    {
        try
        {
            return Files.lines(file).findFirst();
        }
        catch(IOException e)
        {
            LOGGER.warn("Failed to read the file "+file, e);
            return Optional.empty();
        }
    }

    @NotNull
    private static Optional<URL> checkFile (
            @NotNull final Path localJarPath,
            @NotNull final Path localJarMd5Path,
            @NotNull final Path localJarSha1Path
    ){
        if(Files.isRegularFile(localJarPath) && Files.isRegularFile(localJarMd5Path) && Files.isRegularFile(localJarSha1Path))
        {
            final Optional<BigInteger> md5Hash = readSingleLine(localJarMd5Path).map(line -> new BigInteger(line.split(" ")[0], 16));
            final Optional<BigInteger> sha1Hash = readSingleLine(localJarSha1Path).map(line -> new BigInteger(line.split(" ")[0], 16));
            if(md5Hash.isPresent() && sha1Hash.isPresent())
            {
                try
                {
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                    try (InputStream in = Files.newInputStream(localJarPath))
                    {
                        final byte[] bytes = new byte[40*1024];
                        int read;
                        while ((read = in.read(bytes)) != -1)
                        {
                            md5.update(bytes, 0, read);
                            sha1.update(bytes, 0, read);
                        }

                        final BigInteger resultMd5 = new BigInteger(1, md5.digest());
                        final BigInteger resultSha1 = new BigInteger(1, sha1.digest());

                        if(resultMd5.equals(md5Hash.get()) && resultSha1.equals(sha1Hash.get()))
                        {
                            return Optional.of(localJarPath.toUri().toURL());
                        }
                        else
                        {
                            LOGGER.warn(
                                    "The file {} seems to be corrupted! Expected MD5: {}, SHA-1: {} ; Calculated MD5: {}, Sha-1: {}",
                                    localJarPath, md5Hash.get(), sha1Hash.get(), resultMd5, resultSha1
                            );
                        }
                    }
                    catch(IOException e)
                    {
                        LOGGER.warn("Failed to read the file "+localJarPath, e);
                    }
                }
                catch(NoSuchAlgorithmException e)
                {
                    throw new UnsupportedOperationException(e);
                }
            }
        }

        return Optional.empty();
    }

    private static void download(final @NotNull URL url, final @NotNull Path toPath)
            throws IOException
    {
        LOGGER.info("Attempting to download {} to {}", url, toPath);
        try
        {
            Files.createDirectories(toPath.getParent());
            try (
                    InputStream inputStream = url.openStream();
                    ReadableByteChannel rbc = Channels.newChannel(inputStream);
                    FileChannel out = FileChannel.open(toPath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    )
            )
            {
                long total = out.transferFrom(rbc, 0, Long.MAX_VALUE);
                LOGGER.info("Downloaded {} bytes to {}", total, toPath);
            }
        }
        catch(Exception e)
        {
            LOGGER.warn("Failed to download {} - {}: {}", url, e.getClass().getSimpleName(), String.valueOf(e.getLocalizedMessage()));
            throw e;
        }
    }

    @NotNull
    public static URLClassLoader createClassLoader(
            final @NotNull Path localRepository,
            final @NotNull Dependency[] dependencies
    ) throws FileAlreadyExistsException, IOException, SecurityException
    {
        return createClassLoader(localRepository, dependencies, null);
    }

    @NotNull
    public static URLClassLoader createClassLoader(
            final @NotNull Path localRepository,
            final @NotNull Dependency[] dependencies,
            final @Nullable ClassLoader parent
    ) throws FileAlreadyExistsException, IOException, SecurityException
    {
        return createClassLoader(localRepository, dependencies, new URL[0], parent);
    }

    @NotNull
    public static URLClassLoader createClassLoader(
            final @NotNull Path localRepository,
            final @NotNull Dependency[] dependencies,
            final @NotNull URL[] classPath,
            final @Nullable ClassLoader parent
    ) throws FileAlreadyExistsException, IOException, SecurityException
    {
        return createClassLoader(localRepository, new URL[]{
                new URL("http://central.maven.org/maven2/"),
                new URL("https://jcenter.bintray.com/")
        }, dependencies, classPath, parent);
    }

    @NotNull
    public static URLClassLoader createClassLoader(
            final @NotNull Path localRepository,
            final @NotNull URL[] repositories,
            final @NotNull Dependency[] dependencies,
            final @NotNull URL[] classPath,
            final @Nullable ClassLoader parent
    ) throws FileAlreadyExistsException, IOException, SecurityException
    {
        final Path localDir = Files.createDirectories(localRepository);
        final List<URL> localJars = Arrays.stream(dependencies).parallel()
                .map(dependency -> {
                    final Path groupDir = localDir.resolve(dependency.getGroup());
                    final Path artifactDir = groupDir.resolve(dependency.getArtifact());
                    final Path versionDir = artifactDir.resolve(dependency.getVersion());

                    final Path localJarPath = versionDir.resolve(dependency.getJarName());
                    final Path localJarMd5Path = versionDir.resolve(dependency.getJarName()+".md5");
                    final Path localJarSha1Path = versionDir.resolve(dependency.getJarName()+".sha1");

                    Optional<URL> local = checkFile(localJarPath, localJarMd5Path, localJarSha1Path);
                    if(local.isPresent())
                        return local.get();

                    local = Arrays.stream(repositories).map(repository -> {
                        try
                        {
                            return new URL(repository, dependency.getJarPath());
                        } catch(MalformedURLException e)
                        {
                            LOGGER.error("Failed to generate URL for "+dependency+" at "+repository, e);
                            return null;
                        }
                    }).filter(Objects::nonNull).map(url -> {
                        try
                        {
                            for(int i = 0; i < 4; i ++)
                            {
                                if(i != 0)
                                    LOGGER.warn("Retrying {}/3 - {}", i, url);
                                download(url, localJarPath);
                                download(new URL(url, dependency.getJarName() + ".md5"), localJarMd5Path);
                                download(new URL(url, dependency.getJarName() + ".sha1"), localJarSha1Path);
                                final Optional<URL> ok = checkFile(localJarPath, localJarMd5Path, localJarSha1Path);
                                if(ok.isPresent())
                                    return ok.get();
                            }
                            LOGGER.warn("Giving up {}", url);
                            return null;
                        }
                        catch(Exception ignored)
                        {
                            return null;
                        }
                        /*
                        try
                        {
                            LOGGER.debug("Sending HEAD request to {}", url);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("HEAD");
                            int responseCode = connection.getResponseCode();
                            LOGGER.debug("The HEAD request to {} returned {}", url, responseCode);
                            if(responseCode != 200)
                                return false;

                            connection = (HttpURLConnection) new URL(url, dependency.getJarName()+".md5").openConnection();
                            connection.setRequestMethod("HEAD");
                            responseCode = connection.getResponseCode();
                            LOGGER.debug("The HEAD request to {} returned {}", url, responseCode);
                            if(responseCode != 200)
                                return false;

                            connection = (HttpURLConnection) new URL(url, dependency.getJarName()+".sha1").openConnection();
                            connection.setRequestMethod("HEAD");
                            responseCode = connection.getResponseCode();
                            LOGGER.debug("The HEAD request to {} returned {}", url, responseCode);
                            if(responseCode != 200)
                                return false;

                        } catch(IOException e)
                        {
                            LOGGER.warn("Failed to open "+url, e);
                            return false;
                        }
                        */
                    }).filter(Objects::nonNull).findFirst();

                    if(!local.isPresent())
                        throw new IllegalStateException("Failed to download the dependency "+dependency);

                    return local.get();
                }).collect(Collectors.toList());

        return new URLClassLoader(Stream.concat(localJars.stream(), Arrays.stream(classPath)).toArray(URL[]::new), parent);
    }
}
