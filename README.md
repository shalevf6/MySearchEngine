# My Search Engine - A Search Engine Project
This is the first part of the Search Engine Project which is part of the Information Retrieval Course.
Through this application, you can index and create a posting lists file and dictionaries for a given corpus
that answers the rules given by the course's stuff.

## Setup
Simply click on the My_Search_Engine.jar for running the application.

## List of the buttons and other elements in the main window and their functionalities:
### There are 2 "Browse" buttons that open up a File Chooser window:
1. The upper "Browse" button is for getting the path of the folder of the corpus(the
   data set) and within it the stop words file. It's important that the corpuse's
   folder will be named "corpus" (without the apostrophes), and that the stop words
   file will be named: "stop_words.text", or the application will not run.
2. The lower "Browse" button is for getting the folder in which all the data files,
   meaning all the posting files and dictionaries, will be saved in.
You can write the path of the files in the text box next to each "Browse" button as well.
### The "Stemming" check box:
While checked, it will do one of 2 things:
1. If the "Activate" button will be pressed, it will alert the system the the indexing
   process will be using stemming.
2. If the "Load Dictionary" button will be pressed, it will alert the system that the
   posting files and dictionaries that are loaded went through stemming (and save them
   accordingly).
   
While unchecked, it will do one of 2 things:
1. If the "Activate" button will be pressed, it will alert the system the the indexing
   process will be using stemming.
2. If the "Load Dictionary" button will be pressed, it will alert the system that the
   posting files and dictionaries that are loaded gone through stemming.
### The "Activate" button:
The "Activate" button starts the indexing process, with / without stemming (depends if
the "Stemming" checkbox is checked / unchecked), using the corpus given as input, and
writes the posting files and dictionaries to the posting path given as input.
Without using the 2 "Browse" buttons (or text boxes) to choose the necessary paths, the
"Activate" button will not work.
This process will usually take 10 to 15 minutes for a corpus with about half a million
documents, depends on whether you check "Stemming" or not. It will pop up an alert when
it's done with the time the index process took, the number of documents that were indexed,
and the amount of unique terms that were indexed.
If one of the paths doesn't exist, or the path for the corpus doesn't include the corpus
directory or the stop words file inside it, an appropriate error alert will be shown.
### The "Load Dictionary" button:
The "Load Dictionary" button opens up a file chooser window. You should choose the parent
directory of the directory which all the posting and dictionary files are saved in. After 
selecting a right directory, it starts the loading process, with / without stemming (depends
if the "Stemming" checkbox was checked / unchecked).
This process will take less than 10 to 40 seconds for a corpus with about half a million
documents, depends on whether you checked "Stemming" or not. It will pop up an alert when
it's done.
If the path doesn't exist, or the directory in the path doesn't include all the necessary
files, an appropriate error alert will be shown.
### The "Reset" button:
The "Reset" button will erase all of the data saved (if any) in the disk under the posting
path, and all of the data saved in the main memory as well (all the dictionaries).
### The "Show Dictionary" button:
The "Show Dictionary" button will open a new window with all of the unique terms in the
current dictionary (after indexing or loading), sorted by lexicographic order, with the
amount of times each of the terms were found in total in the data set.
Keep in mind, the "Show dictionary" button will work only after loading / indexing a
dataset beforehand. Pressing the "Reset" button will of course mean that only after 
another loading / indexing, the "Show Dictionary" button will work again.
### The "Language Chooser" button:
The "Language Chooser" button will open a small window with a choice box full of all the
languages found in the data set.
Keep in mind, the "Show dictionary" button will work only after loading / indexing a
dataset beforehand. Pressing the "Reset" button will of course mean that only after 
another loading / indexing, the "Show Dictionary" button will work again.


## Link to the project's repository in github:
https://github.com/shalevf6/MySearchEngine

## Built With
JavaFX

## Authors
- Shalev Fainstein
- Idan Shani
