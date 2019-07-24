package com.alliancels.documentation

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction


/**
 * Task for generating navigation page and section previous/next links.
 */
class CombineDocsTask extends SourceTask {

    @Input
    List<String> documentSourceDirs

    List<Section> sections

    @TaskAction
    void exec() throws GradleException {

        sections = []

        source.each {
            sections.add((new LayoutParser()).createSection(it))
        }
        createAllInOnePage()
    }

    String getRelativePath(File file, File root) {
        String relativePath = root.toPath().relativize(file.toPath()).toString()
        String relativePathCrossPlatform = relativePath.replace("\\", "/")
        return relativePathCrossPlatform
    }

    void createAllInOnePage() {

        File docBeingGenerated = new File("${project.buildDir}/documentation/All/AllDocsInOne.html")
        docBeingGenerated.createNewFile()
        docBeingGenerated.text = ''
        
        documentSourceDirs.each {
            println("Name of document: $it")
            File individualDocBeingGenerated = new File("${project.buildDir}/documentation/All/${it}.html")
            individualDocBeingGenerated.createNewFile()
            individualDocBeingGenerated.text = ''
        
            def rootSection = Section.findSection(sections, new File(project.projectDir, it))
            navigateDocDirectoryToBuildAllInOnePage(rootSection, docBeingGenerated)
			navigateDocDirectoryToBuildAllInOnePage(rootSection, individualDocBeingGenerated)
			println('\n')
		}

    }
	
	void navigateDocDirectoryToBuildAllInOnePage(Section section, File file) {
		
		Section sectionToBeAdd = section
		
		int testVar = 0
		
		while (sectionToBeAdd != null)
		{
            //Get relative path to sectionToBeAdd
            String pathToAdd = getRelativePath(sectionToBeAdd.folder, project.projectDir)
            println("$testVar: Adding path: " + pathToAdd)
		
            //Create link for sectionToBeAdd
            String link = pathToAdd + "/section.html"
            println("$testVar: Adding link: " + link)
		
            //Append link to file
            File htmlToAppend = new File("${project.buildDir}/documentation/All/$link")
            println("$testVar: Appending this html: " + htmlToAppend)
            file.append(htmlToAppend.text)
            file.append("<br>")
            file.append("<p>===========================================================</p>")
            file.append("<br>")

            //Get next section to add
            sectionToBeAdd = sectionToBeAdd.getNext(sections)
            testVar++
		}
	}
}
