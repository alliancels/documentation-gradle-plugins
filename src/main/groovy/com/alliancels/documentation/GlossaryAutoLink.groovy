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

        List<Section> glossarySectionList = findGlossarySections(sectionList)
        if(glossarySectionList.size() > 0)
        {
            println("Glossary found!")
            
            //
            createTermsLists(sectionList, glossarySectionList, sourceDirectory, buildDirectory)
            autoLinkTerms(sectionList, sourceDirectory, buildDirectory)
        }
        else
        {
            println("Glossary not found!")
        }
    }

    static List<Section> findGlossarySections(List<Section> sectionList) {
    
        List<Section> glossarySectionList = []
        
        sectionList.each {
            if(it.name == "Glossary")
            {
                glossarySectionList.add(it)
            }
        }
        
        if(glossarySectionList.size() > 0)
        {
            println("Number of glossaries: " + glossarySectionList.size())
        }
        
        return glossarySectionList
    }
    
    static void createTermsLists(List<Section> sectionList, List<Section> glossarySectionList, File sourceDirectory, File buildDirectory) {
    
        println("Creating glossary term, link, and anchor lists...")
        
        listOfTerms = []
        listOfLinks = []
        listOfAnchors = []
        
        glossarySectionList.each {
            File glossaryFile = getBuildFileFromSourceFile(it.folder, sourceDirectory, buildDirectory)
            
            String glossaryFileString = glossaryFile.toString()
            
            glossaryFileString = glossaryFileString + "/Glossary.html"
            
            glossaryFile = new File(glossaryFileString)
        
            String glossaryFileText = glossaryFile.getText()
            
            Document document = Jsoup.parse(glossaryFileText, "UTF-8")
            
            Elements terms = document.select("td:eq(0)").select("td")
            Elements links = document.select("td:eq(1)").select("td")
            Elements anchors = document.select("td:eq(2)").select("td")
            println(terms)
            println(links)
            println(anchors)
            
            listOfTerms += terms.eachText()
            listOfLinks += links.eachText()
            listOfAnchors += anchors.eachText()
            println(listOfTerms)
            println(listOfLinks)
            println(listOfAnchors)
        }
        
        println("++++++++++++++++++++++++++++++++++++++")
        println("++++++++++++++++++++++++++++++++++++++")
        println("++++++++++++++++++++++++++++++++++++++")
        println("======================================")
        println("======================================")
        println("======================================")
    }
    
    static void autoLinkTerms(List<Section> sectionList, File sourceDirectory, File buildDirectory) {
        
        println("Auto linking terms...")
        
        sectionList.each {
            File eachHypertextFile = getBuildFileFromSourceFile(it.folder, sourceDirectory, buildDirectory)
                        
            FilenameFilter htmlFileFilter = new FilenameFilter() {
                public boolean accept(File f, String name)
                {
                    return name.endsWith("html")
                }
            }
            
            File[] htmlFileList = eachHypertextFile.listFiles(htmlFileFilter)
            
            File htmlFile = htmlFileList[0]
            
            println("this is the html file: " + htmlFile)
            
            String htmlFileText = htmlFile.text
            
            println("replaced!")
            
            //String linkToAdd = buildDirectory.toString().replace("\\", "/") + "/documentation/All/"
            
            String htmlLinkPrefix = "<a href="
            String htmlLinkInfix = ">"
            String htmlLinkPostfix = "</a>"
            
            for(int i = 0; i < listOfTerms.size; i++)
            {
                String linkToAdd = buildDirectory.toString().replace("\\", "/") + "/documentation/All/"
                
                linkToAdd = htmlLinkPrefix +  linkToAdd + listOfLinks[i] + htmlLinkInfix + listOfTerms[i] + htmlLinkPostfix
                
                htmlFileText = htmlFileText.replaceAll(("!" + listOfTerms[i]), linkToAdd)
            }
            
            htmlFile.text = htmlFileText
        }
    }
    
    static File getBuildFileFromSourceFile(File sourceFile, File sourceDirectory, File buildDirectory) {
        
        println("Source File: " + sourceFile)
        println("Source Directory: " + sourceDirectory)
        println("Build Directory: " + buildDirectory)
        
        String filString = sourceFile.toString()
        String srcString = sourceDirectory.toString()
        String bldString = buildDirectory.toString()
        
        println("Source File String: " + filString)
        println("Source Directory String: " + srcString)
        println("Build Directroy String: " + bldString)
        
        String buildFileString = filString - srcString
        println("Build file postfix: " + buildFileString) 
        
        buildFileString = bldString + "/documentation/All" + buildFileString
        println("Build file: " + buildFileString)
        
        File buildFile = new File(buildFileString)
        
        return buildFile
    }
}
