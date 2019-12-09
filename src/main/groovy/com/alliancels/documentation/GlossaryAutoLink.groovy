package com.alliancels.documentation

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.jsoup.nodes.Element



/**
 * Automatically find and replace glossary terms with glossary links
 */
class GlossaryAutoLink {
    static List<String> listOfTerms = []
    static List<String> listOfLinks = []
    static List<String> listOfAnchors = []
    
    static void autoLinkGlossary(List<Section> sectionList, File buildDirectory, File sourceDirectory) {

        //Create list of glossary sections
        List<Section> glossarySectionList = findGlossarySections(sectionList)
        
        //If one or more glossaries exist in documentation
        if(glossarySectionList.size() > 0)
        {
            //Create list of terms, links, and anchors
            createTermsLists(sectionList, glossarySectionList, sourceDirectory, buildDirectory)
            
            //Replace each term with its corresponding link
            autoLinkTerms(sectionList, sourceDirectory, buildDirectory)
        }
        else
        {
            println("Glossary not found!")
        }
    }

    static List<Section> findGlossarySections(List<Section> sectionList) {
    
        List<Section> glossarySectionList = []
        
        //For each section of documentation
        sectionList.each {
            //If the section name contains "Glossary" or "glossary" add it to glossary list
            if(it.name.contains("Glossary") || it.name.contains("glossary"))
            {
                glossarySectionList.add(it)
            }
        }
        
        return glossarySectionList
    }
    
    static void createTermsLists(List<Section> sectionList, List<Section> glossarySectionList, File sourceDirectory, File buildDirectory) {
    
        listOfTerms = []
        listOfLinks = []
        listOfAnchors = []
        
        //For each glossary section
        glossarySectionList.each {
            //Get corresponding glossary html file
            File glossaryFile = getBuildFileFromSourceFile(it.folder, sourceDirectory, buildDirectory)
            String glossaryFileString = glossaryFile.toString()
            glossaryFileString = glossaryFileString + "/glossary.html"
            glossaryFile = new File(glossaryFileString)
            
            //Get text of html file
            String glossaryFileText = glossaryFile.getText()
            
            //Parse html text
            Document document = Jsoup.parse(glossaryFileText, "UTF-8")
            
            //Extract all first column text values from glossary table
            Elements terms = document.select("td:eq(0)").select("td")
            //Extract all second column text values from glossary table
            Elements links = document.select("td:eq(1)").select("td")
            //Extract all third column text values from glossary table
            Elements anchors = document.select("td:eq(2)").select("td")
            
            //If terms have not already been replaced by links add text values to corresponding string lists 
            // and remove link and anchor columns
            if(terms.first().text()[0] == "!")
            {
                listOfTerms += terms.eachText()
                listOfLinks += links.eachText()
                listOfAnchors += anchors.eachText()
                
                //Remove link and anchor columns (these do not need to be seen in the final documentation)
                Elements removeTableElements = document.select("td:eq(1)").select("td")
                removeTableElements += document.select("td:eq(2)").select("td")
                removeTableElements += document.select("th:eq(1)").select("th")
                removeTableElements += document.select("th:eq(2)").select("th")
                for(int i = 0; i < removeTableElements.size(); i++)
                {
                    glossaryFile.text = glossaryFile.text.replaceAll(removeTableElements[i].toString(), "")
                }
            }
        }
    }
    
    static void autoLinkTerms(List<Section> sectionList, File sourceDirectory, File buildDirectory) {
        
        //For each section of documentation
        sectionList.each {
            //Get corresponding html file
            File eachHypertextFile = getBuildFileFromSourceFile(it.folder, sourceDirectory, buildDirectory)
            //Use filter to grab only the .html files and not a subfolder (or image folder or other reference document)
            FilenameFilter htmlFileFilter = new FilenameFilter() {
                public boolean accept(File f, String name)
                {
                    return name.endsWith("html")
                }
            }
            File[] htmlFileList = eachHypertextFile.listFiles(htmlFileFilter)
            File htmlFile = htmlFileList[0]
            
            //Get text of html file
            String htmlFileText = htmlFile.text
            
            //Pieces of raw html link
            String htmlLinkPrefix = "<a href="
            String htmlLinkInfix = ">"
            String htmlLinkPostfix = "</a>"
            
            //Loop through each term
            for(int i = 0; i < listOfTerms.size; i++)
            {
                //Create direct refernce link to be added
                String linkToAdd = buildDirectory.toString().replace("\\", "/") + "/documentation/All/"
                
                //Do not add anchors that are not applicable
                String includeAnchor = ""
                if(listOfAnchors[i] != ".")
                {
                    includeAnchor = listOfAnchors[i] 
                }
                
                //Remove !'s from terms so link only shows term name and not "!term!"
                String includeTerm = listOfTerms[i].replaceAll("!", "")
                
                //<a href= + "direct reference file path" + link + anchor + > + term + </a> 
                linkToAdd = htmlLinkPrefix +  linkToAdd + listOfLinks[i] + includeAnchor + htmlLinkInfix + includeTerm + htmlLinkPostfix
                
                //Find and replace all/each instance of "!term!" in documentaiton with its corresponding link 
                htmlFileText = htmlFileText.replaceAll((listOfTerms[i]), linkToAdd)
            }
            
            //Clear lists
            List<String> pathList = []
            List<String> idList = []

            htmlFile.text = htmlFileText
        }
    }
    
    static File getBuildFileFromSourceFile(File sourceFile, File sourceDirectory, File buildDirectory) {
        
        String filString = sourceFile.toString()
        String srcString = sourceDirectory.toString()
        String bldString = buildDirectory.toString()
        
        String buildFileString = filString - srcString
        
        buildFileString = bldString + "/documentation/All" + buildFileString
        
        File buildFile = new File(buildFileString)
        
        return buildFile
    }
    
    static void cleanUpAndLinkCombinedDoc(File fileToCleanUpAndLink, File buildDirectory) {
    
        //Get html text contents
        String combinedHtmlContents = fileToCleanUpAndLink.getText( 'UTF-8' )
        
        //Parse html contents
        Document document = Jsoup.parse(combinedHtmlContents, "UTF-8")

        //Remove all next/previous div elements
        document.select("div:contains(Next)").remove()
        document.select("div:contains(Previous)").remove()

        //Get all ids
        Elements ids = document.select("header").next()
        List<String> idList = ids.eachAttr("id")

        //Get all paths
        Elements headersPath = document.select("header")
        List<String> pathList = headersPath.eachText()
        
        //Replace back slashes (for file system) with forward slashes (for html)
        for(int i = 0; i < pathList.size(); i++)
        {
            pathList[i] = pathList[i].split(" ").last()
            pathList[i] = pathList[i].replace("\\", "/")
        }
        
        //Find and replace all non-jump-to links (that are not referencing the root with corresponding updated jump-to link)
        Elements linksToUpdate = document.select("a[href]")
        List<String> listOfLinksToUpdate = linksToUpdate.eachAttr("href")
        List<String> compareLinks = []
        //For each link that needs to be updated
        for(int i = 0; i < listOfLinksToUpdate.size(); i++)
        {
            //Get root path for links
            String rootLink = buildDirectory.toString().replace("\\", "/") + "/documentation/All/"
            //Create link to compare to compare to (remove root from link)
            compareLinks[i] = listOfLinksToUpdate[i].replaceAll(rootLink, "")
            
            //If the link being updated includes both a path and a jump-to
            if(listOfLinksToUpdate[i].contains("#") && listOfLinksToUpdate[i].contains(".html"))
            {
                //Remove all but the jump-to reference
                combinedHtmlContents = combinedHtmlContents.replaceAll(listOfLinksToUpdate[i], listOfLinksToUpdate[i].split(".html").last())
            }
            //Else if link exists in combined document 
            else if(pathList.contains(compareLinks[i]))
            {
                //Replace with corresponding jump-to link
                combinedHtmlContents = combinedHtmlContents.replaceAll(listOfLinksToUpdate[i], "#" + idList[pathList.indexOf(compareLinks[i])])
            }
        }
        
        //Modify headers so that they appear at top of each individual page instead of overlapped at the top of the combined doc
        combinedHtmlContents = combinedHtmlContents.replaceAll("<header>", "") 
        combinedHtmlContents = combinedHtmlContents.replaceAll("</header>", "")
        
        //Hide all next/previous
        combinedHtmlContents = combinedHtmlContents.replaceAll(">Next<", "><")
        combinedHtmlContents = combinedHtmlContents.replaceAll(">Previous<", "><")
        
        fileToCleanUpAndLink.text = combinedHtmlContents
    }
    
    static void updateCombinedDocLinks(File fileToUpdateLinks) {
    
        //Parse html
        Document document = Jsoup.parse(fileToUpdateLinks, "UTF-8")
        
        //Extract link elements
        Elements elements = document.select("a[href]")
        
        //For each link element
        for(Element linkElement : elements)
        {
            //Get original link element (for replacing later)
            String oldLinkElement = linkElement.toString()
            if(HtmlLinkParser.isLinkExternal(linkElement.toString()))
            {
                //If the link references external web page do nothing
            }
            else if(linkElement.attr("href") == null)
            {
                //If (for some reason) the link reference is null do nothing
            }
            else
            {
                //Get string of link element path
                String oldLinkPath = HtmlLinkParser.getLinkPath(linkElement.attr("href"))
                //If string is not null and is not already referencing root
                if(oldLinkPath != null && !oldLinkPath.contains("C:"))
                {
                    //Create new link referencing root
                    File linkedFile = new File(fileToUpdateLinks.parentFile, oldLinkPath)
                    
                    //Replace back slashes (for file system) with forward slashes (for html)
                    String newLinkPath = linkedFile.toString().replace("\\", "/")
                    
                    //Separate each element of path
                    List<String> newLinkPathSplit = newLinkPath.split("/")
                    
                    //Get index of first ".." in link/path
                    int index = newLinkPathSplit.indexOf("..")
                    //While there is ".." in link/path
                    while(index > 0)
                    {
                        if(index - 1 > 0)
                        {
                            //Remove each instance of ".." and the corresponding related path element
                            newLinkPathSplit.remove(index - 1)
                            newLinkPathSplit.remove(index - 1)
                        }
                        //Get next index of ".."
                        index = newLinkPathSplit.indexOf("..")
                    }
                    
                    //Rebuild link/path (adding back "/" where necessary due to removed "..")
                    newLinkPath = ""
                    for(int i = 0; i < newLinkPathSplit.size(); i++)
                    {
                        newLinkPath += newLinkPathSplit[i]
                        //Do not add "/" to end of link/path
                        if(i < newLinkPathSplit.size() - 1)
                        {
                            newLinkPath += "/"
                        }
                    }
                    
                    //Update each link/path in html file with updated link/path
                    if(linkedFile.exists())
                    {
                        linkElement.attr("href", newLinkPath)
                    
                    }
                    
                    //Add escape sequences to original link element
                    oldLinkElement = oldLinkElement.replace("\\", "\\\\")
                    
                    //Replace each original link element with updated new link element
                    fileToUpdateLinks.text = fileToUpdateLinks.text.replaceAll(oldLinkElement, linkElement.toString())
                }
            }
        }
    }
}
