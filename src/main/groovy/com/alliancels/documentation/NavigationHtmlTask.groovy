package com.alliancels.documentation

import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction


/**
 * Task for generating navigation page and section previous/next links.
 */
class NavigationHtmlTask extends SourceTask {

    @Input @OutputDirectory
    File linkOutputDir

    @Input @OutputDirectory
    File navigationOutputDir

    @Input
    List<String> documentSourceDirs

    List<Section> sections
    
    //Counter for number of node start html elements without node end html elements
    int numOfNodeStartsWithoutNodeEnds = 0

    private HtmlPresenter htmlPresenter = new HtmlPresenter()

    @TaskAction
    void exec() throws GradleException {

        sections = []

        source.each {
            sections.add((new LayoutParser()).createSection(it))
        }

        createNextPreviousLinkFiles()

        sections = []

        source.each {
            sections.add((new LayoutParser()).createSection(it))
        }
        createNavigationPage()
    }

    void createNextPreviousLinkFiles() {

        sections.each {

            String navigationLinkHtml = ''

            Section previousGroup = it.getPrevious(sections)
            Section nextGroup = it.getNext(sections)

            // Get link HTML
            // Do not show Previous link if the first section
            // Do not show Next link if the last section
            if (previousGroup != null && nextGroup != null) {
                String previousLink = getRelativePath(previousGroup.folder, it.folder)
                String nextLink = getRelativePath(nextGroup.folder, it.folder)
                
                File previousLinkFile = getLinkOutputFile(previousGroup.folder)
                File nextLinkFile = getLinkOutputFile(nextGroup.folder)
                
                previousLink += "\\" + previousLinkFile.getName()
                nextLink += "\\" + nextLinkFile.getName()
                
                navigationLinkHtml = HtmlPresenter.getPreviousNextLinks(previousLink, nextLink)
            } else if (previousGroup == null && nextGroup != null) {
                String nextLink = getRelativePath(nextGroup.folder, it.folder)
                
                File nextLinkFile = getLinkOutputFile(nextGroup.folder)
                
                nextLink += "\\" + nextLinkFile.getName()
                
                navigationLinkHtml = HtmlPresenter.getNextLink(nextLink)
            } else if (previousGroup != null && nextGroup == null) {
                String previousLink = getRelativePath(previousGroup.folder, it.folder)
                
                File previousLinkFile = getLinkOutputFile(previousGroup.folder)
                
                previousLink += "\\" + previousLinkFile.getName()
                
                navigationLinkHtml = HtmlPresenter.getPreviousLink(previousLink)
            } else {
                GradleException("No next or previous section was found for " + it.folder.toString())
            }

            // Create html file with links
            File outputFile = getLinkOutputFile(it.folder)
            outputFile.createNewFile()
            outputFile.write(navigationLinkHtml)
        }
    }

    void createNavigationPage() {

        def navigationPaneContents = ''
        
        numOfNodeStartsWithoutNodeEnds = 0

        documentSourceDirs.each {
            def rootSection = Section.findSection(sections,
                    new File(project.projectDir, it))
            navigationPaneContents += createNavigationNodes(rootSection)
            
            //Add node end html elements for each node start element without a corresponding end
            navigationPaneContents += getNavigationNodeEnds(numOfNodeStartsWithoutNodeEnds)
        }

        def extensions = (DocumentationPluginExtension) getProject()
                .getExtensions().findByName("documentation")

        def navigationPage = htmlPresenter.createNavigationPage(extensions.projectName, extensions.version, extensions.date, navigationPaneContents)
        navigationOutputDir.mkdirs()
        def outputFile = new File(navigationOutputDir, 'navigation.html')
        outputFile.createNewFile()
        outputFile.write(navigationPage)
    }

    String createNavigationNodes(Section section) {

        String navigationPaneContents = ''

        //Add node start html element
        navigationPaneContents += htmlPresenter.getNavigationNodeStart()
        //Increment node start html element counter
        numOfNodeStartsWithoutNodeEnds++
        
        //Add link to navigation (keep unique file name)
        String relativePath = getRelativePath(section.folder, project.projectDir)
        File getLinkFile = getLinkOutputFile(section.folder)
        String link = relativePath + "\\" + getLinkFile.getName()
        navigationPaneContents += htmlPresenter.getNavigationLink(link, section.name)

        Section nextGroup = section.getNext(sections)

        // If not the last group
        if (nextGroup != null) {
            // If the next group is a child of the current group, get the next group
            if (nextGroup.getParent(sections) == section) {
                navigationPaneContents += createNavigationNodes(nextGroup)
            // If the next group is not a child of the current group
            } else {
                int depthChange = getDepthChange(section.folder, nextGroup.folder)
                navigationPaneContents += getNavigationNodeEnds(depthChange + 1)
                navigationPaneContents += createNavigationNodes(nextGroup)
            }
        }
        // If the last group, end any nodes not ended yet
        else {
            int depthChange = getDepthChange(section.folder, project.projectDir)
            navigationPaneContents += getNavigationNodeEnds(depthChange - 1)
        }

        return navigationPaneContents
    }

    File getLinkOutputFile(File inputFolder) {

        String relativePath = getRelativePath(inputFolder, project.projectDir)
        File outputPath = new File (linkOutputDir, relativePath)
        outputPath.mkdirs()
        
        //Get name of link file to be created by copying file name in converted markdown folder
        String pathToFileName = project.buildDir.toString() + "\\documentation\\convertedMarkdown\\" + relativePath
        File fileToCopyNameFrom = new File (pathToFileName)
        File[] listFiles = fileToCopyNameFrom.listFiles()
        //Default file name to "section.html"
        String fileNameToCopy = "section.html"
        //Get name of first file in list (ignore directories)
        for(int i = 0; i < listFiles.length; i++)
        {
            if(listFiles[i].isFile())
            {
                fileNameToCopy = listFiles[i].getName()
            }
        }
        
        return new File(outputPath, fileNameToCopy)
    }

    String getRelativePath(File file, File root) {
        String relativePath = root.toPath().relativize(file.toPath()).toString()
        String relativePathCrossPlatform = relativePath.replace("\\", "/")
        return relativePathCrossPlatform
    }

    int getDepth(File file, File root) {
        return getRelativePath(file, root).split('/').size()
    }

    int getDepthChange(File current, File next) {
        int change = getDepth(current, project.projectDir) -
                getDepth(next, project.projectDir)
        return change
    }

    String getNavigationNodeEnds(int count) {

        String nodeEnds = ''

        while (count > 0) {
            count--
            nodeEnds += htmlPresenter.getNavigationNodeEnd()
            //Decrement node start html element counter
            numOfNodeStartsWithoutNodeEnds--
        }

        return nodeEnds
    }
}
