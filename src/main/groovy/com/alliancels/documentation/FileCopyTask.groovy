package com.alliancels.documentation

import org.gradle.api.GradleException
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

/**
 * Task for copying new/updated files from input to output, and removing deleted input files from the output
 */
class FileCopyTask extends SourceTask {

    @Input @OutputDirectory @Optional
    File outputDir

    @TaskAction
    void execute(IncrementalTaskInputs inputs) throws GradleException {

        if (!inputs.incremental) {
            project.delete(outputDir.listFiles())
        }

        inputs.outOfDate {
            File outputFile = getOutputFile(it.file)

            def sourceStream = new File(it.file.canonicalPath).newDataInputStream()
            def destinationStream = new File(outputFile.canonicalPath).newDataOutputStream()
            destinationStream << sourceStream
            sourceStream.close()
            destinationStream.close()
        }

        inputs.removed {
            File outputFile = getOutputFile(it.file)

            if (outputFile.exists()) {
                outputFile.delete()
            }
        }
    }

    File getOutputFile(File inputFile) {
        String relativePath = getRelativePath(inputFile.parentFile, project.projectDir)
        File outputPath = new File (outputDir, relativePath)
        outputPath.mkdirs()
        return new File(outputPath, inputFile.name)
    }

    String getRelativePath(File file, File root) {
        return root.toPath().relativize(file.toPath()).toString()
    }
}
