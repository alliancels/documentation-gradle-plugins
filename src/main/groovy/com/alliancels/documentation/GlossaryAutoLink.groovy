package com.alliancels.documentation

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements


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
            //If the section name is "Glossary" add it to glossary list
            if(it.name == "Glossary")
            {
                glossarySectionList.add(it)
            }
        }
        
        if(glossarySectionList.size() > 0)
        {
            println("Number of glossaries found: " + glossarySectionList.size())
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
            glossaryFileString = glossaryFileString + "/Glossary.html"
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
            
            //Add text values to corresponding string lists
            listOfTerms += terms.eachText()
            listOfLinks += links.eachText()
            listOfAnchors += anchors.eachText()
        }
    }
    
    static void autoLinkTerms(List<Section> sectionList, File sourceDirectory, File buildDirectory) {
        
        println("List of terms: " + listOfTerms)
        println("List of links: " + listOfLinks)
        println("List of Anchors: " + listOfAnchors)

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
                //<a href= + "direct reference file path" + link + anchor + > + term + </a> 
                linkToAdd = htmlLinkPrefix +  linkToAdd + listOfLinks[i] + includeAnchor + htmlLinkInfix + listOfTerms[i] + htmlLinkPostfix
                
                //Find and replace all/each instance of "!term!" in documentaiton with its corresponding link 
                htmlFileText = htmlFileText.replaceAll(("!" + listOfTerms[i] + "!"), linkToAdd)
            }
            
            htmlFile.text = htmlFileText
        }
        
        println("Terms linked!")
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
}
