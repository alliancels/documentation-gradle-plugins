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

            if (outputDir.listFiles() != null) {
                project.delete(outputDir.listFiles())
            }
        }

        inputs.outOfDate {

			// Only act on files, not directories
            if (it.file.isFile()) {
                File outputFile = getOutputFile(it.file)

                def sourceStream = new File(it.file.canonicalPath).newDataInputStream()
                def destinationStream = new File(outputFile.canonicalPath).newDataOutputStream()
                destinationStream << sourceStream
                sourceStream.close()
                destinationStream.close()
                
                //Create new file path for all images
                File allImagesPath = new File("${project.buildDir}/documentation/All/Images/")
                //Generate directory for path if not already done
                if(!allImagesPath.exists())
                {
                    allImagesPath.mkdirs()
                }
                //Create new file for image to be copied
                File imageBeingCopied = new File(allImagesPath, it.file.name)
                //Define stream for moving file from
                def sourceStreamAll = new File(it.file.canonicalPath).newDataInputStream()
                //Define stream for moving file to
                def desinationStreamAll = new File(imageBeingCopied.canonicalPath).newDataOutputStream()
                //Move file
                desinationStreamAll << sourceStreamAll
                //Close streams 
                sourceStreamAll.close()
                desinationStreamAll.close()
                println("Moved all images")
            }
        }

        inputs.removed {

			// Only act on files, not directories
            if (it.file.isFile()) {
                File outputFile = getOutputFile(it.file)

                if (outputFile.exists()) {
                    outputFile.delete()
                }
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
