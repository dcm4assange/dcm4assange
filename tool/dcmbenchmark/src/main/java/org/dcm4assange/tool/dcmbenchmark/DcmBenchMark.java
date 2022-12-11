/*
 * Copyright 2021 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dcm4assange.tool.dcmbenchmark;

import org.dcm4assange.DicomInputStream;
import org.dcm4assange.DicomObject;
import org.dcm4assange.Tag;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Dec 2022
 */
@CommandLine.Command(
        name = "dcmbenchmark",
        mixinStandardHelpOptions = true,
        versionProvider = DcmBenchMark.VersionProvider.class,
        descriptionHeading = "%n",
        description = "The dcmbenchmark utility parse a DICOM file repetitively, printing parsing time and used memory as tab-separated values (TSV) to standard output.",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        showDefaultValues = true,
        footerHeading = "%nExample:%n",
        footer = { "$ dcmbenchmark image.dcm", "Read DICOM file image.dcm without Pixel Data 100 times with one measurement per 10 parsings." }
)
public class DcmBenchMark implements Callable<Integer> {
    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[]{ DcmBenchMark.class.getModule().getDescriptor().rawVersion().orElse("7") };
        }
    }

    @CommandLine.Parameters(description = "DICOM file to parse.")
    Path file;

    @CommandLine.Option(names = "-n", paramLabel = "<no>",
            description = "Number of parsing per measurement.")
    int samples = 10;

    @CommandLine.Option(names = "-m", paramLabel = "<no>",
            description = "Number of measurements.")
    int measurements = 10;

    @CommandLine.Option(names = "-g",
            description = "Runs the garbage collector to free memory.")
    boolean gc;

    @CommandLine.Option(names = "-a",
            description = "Accumulate parsed datasets in memory.")
    boolean accumulate;

    @CommandLine.Option(names = "-p",
            description = "Read Pixel Data from file.")
    boolean pixelData;

    public static void main(String[] args) {
        new CommandLine(new DcmBenchMark()).execute(args);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("N\tTime (ns)\tMemory (bytes)");
        Runtime rt = Runtime.getRuntime();
        List<DicomObject> list = new LinkedList<>();
        for (int i = 1; i <= measurements; i++) {
            long start = System.nanoTime();
            for (int j = 0; j < samples; j++) {
                try (DicomInputStream dis = new DicomInputStream(file)) {
                    list.add((pixelData ? dis : dis.stopBefore(Tag.PixelData)).readDataSet());
                }
            }
            long end = System.nanoTime();
            if (gc) rt.gc();
            if (!accumulate) list.clear();
            System.out.printf("%d\t%d\t%d%n", i * samples, end - start, rt.totalMemory() - rt.freeMemory());
        }
        return 0;
    }
}
