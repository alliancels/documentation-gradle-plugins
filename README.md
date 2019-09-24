## Overview

Gradle plugin for helping with writing project requirements, tests plans for those requirements, and status reports for those test plans.

## Usage

### Gradle build setup

In the project gradle.build file, the Documentation library will need to be available to the build script:

```
buildscript {
    repositories {
        maven {
            url 'https://github.com/alliancels/maven-repo/raw/releases'
        }
    }
    dependencies {
        //For Gradle 3.x:
        //classpath 'com.alliancels:documentation-gradle-plugins:0.0.14'
        //For Gradle 5.x:
        classpath 'com.alliancels:documentation-gradle-plugins:0.1.4'
    }
}
```

Then the plugin will need to be applied.

```
import com.alliancels.documentation.DocumentationPlugin
apply plugin: DocumentationPlugin
```

Finally, the plugin will need to be configured for the project:

```
// Define any number of documents to create, each built from any number of markdown source folders
Document all = new Document()
all.with {
	name = "All"
	sourceFolders = ['UserRequirements', 'DesignRequirements']
	previewEnabled = false
}

Document preview = new Document()
preview.with {
	name = "Preview"
	sourceFolders = ['UserRequirements', 'DesignRequirements']
	previewEnabled = true
}

// Setup plug-in
documentation {
	// Project name
    projectName = 'example'
    // Document version
    version = 'version'
    // Document date
    date = 'date'
    // List of documents to create
    documents = [all, preview]
}
```

### Requirements Source Folders

The following folders are expected to exist within the root of any Gradle project this plugin is applied to:

	UserRequirements
	DesignRequirements
	
### Requirements Section Folders

Under the top-level requirements folders, a hierarchy of subfolders for each requirements "section" can be created.

	UserRequirements\
		layout.yaml
		section.md
		Section1\
			layout.yaml
			section.md
			Images\
			Section1-1\
				layout.yaml
				section.md
				Images\
		Section2\
			section.md

#### requirements.md

Each folder must contain a markdown file containing the section contents.

Links to other sections should use relative paths, and should use the *.html suffix rather than
the *.md suffix.

UserRequirements\Section1\Section1-1\requirements.md example:

	[link to Section2](..\..\Section2\requirements.html)

#### layout.yaml

Each folder must contain a file describing the layout of the section.  This file must provide
a proper name for the section, otherwise the folder name will be used.

UserRequirements\Section1\layout.yaml content example:

	name: Section One
	
This file can optionally also specify the ordering of all of its immediate subfolders (subsections).
If this field is not defined, the sections will be ordered alphabetically based on their folder
name.  If only some of the subsections are listed, they will be ordered below the parent section first,
followed by all remaining subsections in alphabetical order based on their folder name.

UserRequirements\layout.yaml content example:

	orderedSubfolders:
	  - Section2
	  - Section1

#### Images\

This folder is optional.  All files in this folder will be copied into the output.  Links to images
from markdown should be relative.

requirements.md example:

	![example image](Images\example.png)
	
### Assemble Documentation

For each `Document` object defined in the build script, a task called `assemble<document name>' will be created.
The output of the task will be in a folder matching the document name.  The root file of the generated document
HTML site will be called `navigation.html`.

For example, the following configuration will create tasks called "assembleAll" and "assemblePreview".  The
output folders would be `build/documentation/All` and `build/documentation/Preview`.

```
Document all = new Document()
all.with {
	name = "All"
	sourceFolders = ['UserRequirements', 'DesignRequirements']
	previewEnabled = false
}

Document preview = new Document()
preview.with {
	name = "Preview"
	sourceFolders = ['UserRequirements', 'DesignRequirements']
	previewEnabled = true
}

// Setup plug-in
documentation {
    projectName = 'example'
    // Document version
    version = 'version'
    date = 'date'
    documents = [all, preview]
}
```

To get a list of all documentation tasks that have been created, invoke `gradlew tasks` and look at the tasks listed in the
*DocumentationPlugin tasks* group.

Note that task names can be camel-case abbreviated.  For example, a task called "assembleAll" can be invoked using only "aA".

#### Assemble documentation with live preview

Manually invoking a rebuild of the HTML site after making a change is tedious.  To avoid this, it can
be auto-rebuilt and redisplayed whenever a requirements source file is saved.

To allow this type of "live preview" of the generated HTML site, enable browser auto-refresh with setting the
`previewEnabled` parameter of a document to "true":

```
Document preview = new Document()
preview.with {
	name = "Preview"
	sourceFolders = ['UserRequirements', 'DesignRequirements']
	previewEnabled = true
}
```

Enable automatic continuous rebuild using the following command:

`gradlew <task> --continuous`

This can be abbreviated as `gradlew <task> -t`.

Example: ``gradlew assemblePreview --continuous`

Changes to existing markdown files will be visible shortly after saving.  Changes to the the folder
structure of layout.yaml files will require the browser to be manually refreshed.

### Combine Documentation

Combines each individual markdown-converted-to-html section/file/page into one html file/page for each top level document source folder
as well as one all-in-one combined document. The order in which each section/file/page is combined matches the order described above in 
[layout.yaml](#layout.yaml). Since this is mostly intended to make it easy to print, some html elements are modified
and section numbers are added.

This task can be invoked with the "combineAll" command; the task is not automatically ran when "assembleAll" is invoked 
because combined documentation doesn't always need to be generated and it may unnecessarily lengthen build times.

#### Glossary Auto Linking

Included in the "combineAll" command is automatic linking of glossary terms. Each file in the documentation that contains "Glossary" 
or "glossary" will be examined for a table of terms and corresponding links and anchors. Terms are in the form of "!Term!". Links are file paths to
a specific ".html" file in the build directory. Anchors are in the form of "#anchor-example"; if no anchor wanted/needed a "." should be placed 
in the anchor column (see example table below). 

| Term    | Link                              | Anchor        |
|:--------|:----------------------------------|:--------------|
| !Term1! | ExampleDocument/exampleDoc1.html  | .             |
| !Term2! | ExampleDocument/exampleDoc2.html  | #example-doc2 |

Each "!Term!" in the documentation is then automatically replaced by its corresponding link, and anchor if wanted/needed.

#### Combined Documentation Auto Linking

Links in the combined documentation are automatically updated to navigate/jump-to the appropriate place in the combined documentation.
This is done by replacing direct reference links with corresponding jump-to links. If the combined document contains a link external 
to itself it will attempt to link to the external document if it exists. 

### Check Documentation

For each `Document` object defined in the build script, a task called `check<document name>' will be created.
This task can be run to check for problems in the document.

Currently, this only checks for broken links, including:
   - Intrapage links (anchors within a page)
   - Interpage links (file and optionally an anchor in the file)
   - Image source paths

For example, the following configuration will create tasks called "checkAll".  It will check the
contents of `build/documentation/All`.

```
Document all = new Document()
all.with {
    name = "All"
    sourceFolders = ['UserRequirements', 'DesignRequirements']
    previewEnabled = false
}

// Setup plug-in
documentation {
    projectName = 'example'
    // Document version
    version = 'version'
    date = 'date'
    documents = [all]
}
```

### Build Documentation

For each `Document` object defined in the build script, a task called `build<document name>' will be created.
This simply chains the "assemble" and "check" tasks into a single command.

# Build

The project can be rebuilt by invoking the `gradlew build` command.

# Release new versions

Clone [the maven repo](https://github.com/alliancels/maven-repo) to the same root directory as this repo.

    Example:
        \repos\documentation-gradle-plugins
        \repos\maven-repo

Push the new version to this project's repo.

Publish library by invoking the `gradlew publish` command.  This will install the library, source, and doc jars into the
local maven repo.

Commit and push changes to the maven repo.

# Manual testing

In general, this project should be tested using unit and integegration tests, as well as end-to-end tests with the
[Gradle Test Kit](https://docs.gradle.org/current/userguide/test_kit.html).  In some cases, manual testing may also be
needed.

## Test using example project

Go to the *example* project directory (`cd example`), and run tasks as a composite build that includes the plugin build:
`gradlew --include-build .. <task>`

For example, to run a task called "assembleAll", invoke `gradlew --include-build .. aA`.

## Test using an external project

In order to manually test without releasing a new version, the plugin still needs to be published.
To do this, follow the steps to publish in the [Release new versions section](#release-new-versions), but push the changes to the
[maven repo](https://github.com/alliancels/maven-repo) on a temporary topic branch, rather than the 'releases' branch.

In the project that you want to perform the manual testing with, adjust the [Gradle Build Setup](#gradle-build-setup)
to use your topic branch of the maven repo instead of the 'releases' branch.  That is, replace
`url 'https://github.com/alliancels/maven-repo/raw/releases'` with
`url 'https://github.com/alliancels/maven-repo/raw/topicBranchName'`.

When you are done testing, the topic branch can be purged.
