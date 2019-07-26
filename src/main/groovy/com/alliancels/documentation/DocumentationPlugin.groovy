package com.alliancels.documentation

import org.gradle.api.Project
import org.gradle.api.Plugin

class DocumentationPlugin implements Plugin<Project> {

    File convertedMarkdown
    File links
    File documentationOutput

    void apply(Project project) {

        // Apply stanndard Gradle 'base' plug-in, to add the 'clean' task
        project.apply(plugin: 'base')

        documentationOutput = new File(project.buildDir, 'documentation/')
        convertedMarkdown = new File(documentationOutput, 'convertedMarkdown')
        links = new File(documentationOutput, 'links')

        def extension = project.extensions.create('documentation', DocumentationPluginExtension.class)

        // Dynamically create tasks based on documents list
        project.afterEvaluate {

            // Create a task to convert all markdown files
            project.task('markdownToHtml', type: MarkdownToHtmlTask) {
                description = "Convert all markdown files to html files"
                include "**/*.md"
                outputDir = convertedMarkdown
                source getAllSourceFolders(extension.documents)
            }

            // Create a set of tasks for each document defined by the user
            extension.documents.each {
                createDocumentTasks(project, it)
            }

            // Group all tasks under the DocumentationPlugin task category
            project.tasks.each {
                it.group = "DocumentationPlugin"
            }
        }
    }

    void createDocumentTasks(Project project, Document document) {

        File outputFolder = new File(documentationOutput, document.name + "/")

        project.task("copyImages${document.name}", type: FileCopyTask) {
            description = "Copy images into the output for the ${document.name} document."
            include "**/Images/**"
            outputDir = outputFolder
            source document.sourceFolders
        }

        project.task("navigation${document.name}", type: NavigationHtmlTask) {
            description = "Create navigation page and navigation links for the ${document.name} document."
            include "**/layout.yaml"
            linkOutputDir = links
            navigationOutputDir = outputFolder
            documentSourceDirs = document.sourceFolders
            source document.sourceFolders
        }

        project.task("assemble${document.name}", type: AssembleDocumentTask,
                dependsOn: ['markdownToHtml', "copyImages${document.name}", "navigation${document.name}"]) {
            description = "Assemble the ${document.name} document."
            outputDir = outputFolder
            linkDir = links
            convertedMarkdownDir = convertedMarkdown
            source links, convertedMarkdown
            previewEnabled = document.previewEnabled
            document.sourceFolders.each {
                include "${it}/**/section.html"
            }
        }
        
        project.task("combine${document.name}", type: CombineDocsTask) {
            description = "Create combined pages for the ${document.name} document."
            include "**/layout.yaml"
            documentSourceDirs = document.sourceFolders
            source document.sourceFolders
        }

        project.task("check${document.name}", type: CheckDocumentTask) {
            description = "Check the ${document.name} document for problems."
            source outputFolder
            document.sourceFolders.each {
                include "${it}/**/*.html"
            }
        }

        project.task("build${document.name}", dependsOn:["assemble${document.name}", "check${document.name}"]) {
            description = "Build the ${document.name} document, by assembling it and checking it for problems."
        }
    }

    List<String> getAllSourceFolders(List<Document> documents) {

        List<String> allSourceFolders = []

        documents.each {
            allSourceFolders += it.sourceFolders
        }

        return allSourceFolders.unique()
    }
}