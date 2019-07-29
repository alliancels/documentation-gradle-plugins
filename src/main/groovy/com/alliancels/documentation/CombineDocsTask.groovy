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
        createCombinedDocs()
    }

    String getRelativePath(File file, File root) {
        String relativePath = root.toPath().relativize(file.toPath()).toString()
        String relativePathCrossPlatform = relativePath.replace("\\", "/")
        return relativePathCrossPlatform
    }

    void createCombinedDocs() {

        //Create empty file for all in one combined doc
        File docBeingGenerated = new File("${project.buildDir}/documentation/All/AllDocsCombined.html")
        docBeingGenerated.createNewFile()
        docBeingGenerated.text = ''
        println("Generated combined doc/page for: AllDocsCombined")
        
        //Create a combined doc page for each folder in source directory
        documentSourceDirs.each {
            File individualDocBeingGenerated = new File("${project.buildDir}/documentation/All/${it}.html")
            individualDocBeingGenerated.createNewFile()
            individualDocBeingGenerated.text = ''
            println("Generated combined doc/page for: $it")
        
            //The order of how each page is added to the combined page matches the order followed in navigation
            //Add page individual document as well as all combined document
            def rootSection = Section.findSection(sections, new File(project.projectDir, it))
            navigateDocDirectoryToBuildCombinedPage(rootSection, docBeingGenerated)
			navigateDocDirectoryToBuildCombinedPage(rootSection, individualDocBeingGenerated)
			
            //Clean up (remove) unwanted html bits (hide next and previous links and move headers to above individual sections)
            cleanUpCombinedDocHTML(individualDocBeingGenerated)
            
            //Add combined link to navigation page
            String addDocToNav = "${it}"
            addCombinedLinksToNavigation(addDocToNav)
		}
		
        //Clean up (remove) unwanted html bits (hide next and previous links and move headers to above individual sections)
        cleanUpCombinedDocHTML(docBeingGenerated)
		
        //Add combined link to navigation page 
        String addCombinedDocToNav = "AllDocsCombined"
        addCombinedLinksToNavigation(addCombinedDocToNav)
    }
	
	void navigateDocDirectoryToBuildCombinedPage(Section section, File file) {
		
		Section sectionToBeAdd = section
		
		//Loop through all sections (all pages in document)
		while (sectionToBeAdd != null)
		{
            //Get relative path to section to be added
            String pathToAdd = getRelativePath(sectionToBeAdd.folder, project.projectDir)
		
            //Create link for section to be added
            String linkToAdd = pathToAdd + "/section.html"
		
            //Append section to combined page
            File htmlToAppend = new File("${project.buildDir}/documentation/All/$linkToAdd")
            file.append(htmlToAppend.text)
            
            //Insert breaks and page separator indicator so it is easy to distinguish between sections of combined page
            file.append("<br>")
            file.append("<p>===========================================================</p>")
            file.append("<br>")

            //Get next section to add
            sectionToBeAdd = sectionToBeAdd.getNext(sections)
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
    
    void cleanUpCombinedDocHTML(File fileToCleanUp) {
    
        //Get contents of file to clean up
        String contents = fileToCleanUp.getText( 'UTF-8' )
        
        //Remove header element tags (this moves the header text to above the individual section instead of stacked at the top of the combined doc)
        contents = contents.replaceAll("<header>", '')
        contents = contents.replaceAll("</header>", '')
        
        //Remove text of Next and Previous buttons (the html elements remain but without text they do not appear on the page)
        contents = contents.replaceAll(">Previous<", "><")
        contents = contents.replaceAll(">Next<", "><")
        
        //Set file to now cleaned up contents 
        fileToCleanUp.text = contents
    }
}
