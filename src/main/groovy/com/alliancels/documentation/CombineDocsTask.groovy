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

        File docBeingGenerated = new File("${project.buildDir}/documentation/All/AllDocsCombined.html")
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
			
            //Remove unwanted hmtl bits (hide next and previous links and move headers to above individual sections)
            String contents = new File("${project.buildDir}/documentation/All/${it}.html").getText( 'UTF-8' )
            contents = contents.replaceAll("<header>", '')
            contents = contents.replaceAll("</header>", '')
            contents = contents.replaceAll(">Previous<", "><")
            contents = contents.replaceAll(">Next<", "><")
            individualDocBeingGenerated.text = contents
            
            //Add combined link to navigation page
            String addDocToNav = "${it}"
            addCombinedLinksToNavigation(addDocToNav)
		}
		
		//Remove unwanted hmtl bits (hide next and previous links and move headers to above individual sections)
		String contents = new File("${project.buildDir}/documentation/All/AllDocsCombined.html").getText( 'UTF-8' )
		contents = contents.replaceAll("<header>", '')
		contents = contents.replaceAll("</header>", '')
		contents = contents.replaceAll(">Previous<", "><")
		contents = contents.replaceAll(">Next<", "><")
		docBeingGenerated.text = contents
        
        //Add combined link to navigation page 
        String addCombinedDocToNav = "AllDocsCombined"
        addCombinedLinksToNavigation(addCombinedDocToNav)
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
	
	void addCombinedLinksToNavigation(String docToLink) {
        //Get navigation file to modify
        File navFile = new File("${project.buildDir}/documentation/All/navigation.html")
        //Get text of file to modify
        String navContents = navFile.getText( 'UTF-8' )
        //Create link to be added
        String linkToAdd = docToLink + ".html"
        //Create hmtl to be added
        String htmlToAdd = "<li><a href=\"" + linkToAdd + "\" target=\"sectionFrame\">" + docToLink + "</a>" + "</li>" + "\r\n" + "</div>"
        //If combined link isn't already added
        if(!navContents.contains(linkToAdd))
        {
            //Replace the first instance of "</div"> with html generated above
            navContents = navContents.replaceFirst("</div>", htmlToAdd)
            //Replace old text contents with link added contents
            navFile.text = navContents
        }
    }
}
