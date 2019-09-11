# This script provides handy functions to use the ABECTO REST service, hidding the raw HTTP requests.

library("httr", "jsonlite")

base <- "http://localhost:8080/"
jar <- "target/abecto.jar"

# abecto server

isRunning <- function() {
    tryCatch({GET(url=base);TRUE;},error=function(e){FALSE})
}

start <- function() {
    if(!isRunning()){
        system(paste0("java -jar ",jar),wait=F)
        while(!isRunning()) {}
    }
}

# project

createProject <- function(label="") {
    content(POST(url=paste0(base,"project"),body=list(label=label)))$id
}

deleteProject  <- function(project) {
    content(DELETE(url=paste0(base,"project/",project)))
}

listProjects <- function() {
    content(GET(url=paste0(base,"project")))
}

getProject <- function(project) {
    content(GET(url=paste0(base,"project/",project)))
}

runProject <- function(project,await=FALSE) {
    content(GET(url=paste0(base,"project/",project,"/run"),query=list(await=await)))
}

# knowledgebase

createKnowledgebase  <- function(project,label="") {
    content(POST(url=paste0(base,"knowledgebase"),body=list(project=project,label=label)))$id
}

deleteKnowledgebase  <- function(knowledgebase) {
    content(DELETE(url=paste0(base,"knowledgebase/",knowledgebase)))
}

getKnowledgebase  <- function(knowledgebase) {
    content(GET(url=paste0(base,"knowledgebase/",knowledgebase)))
}

listKnowledgebases <- function(project=NULL) {
    content(GET(url=paste0(base,"knowledgebase"),query=list(project=project)))
}

# step

createStep  <- function(class,knowledgebase=NULL,input=NULL,parameters=NULL) {
    content(POST(url=paste0(base,"step"),body=list(class=class,knowledgebase=knowledgebase,input=input,parameters=parameters)))$id
}

getStep <- function(step) {
    content(GET(url=paste0(base,"step/",step)))
}

listSteps <- function(project) {
    content(GET(url=paste0(base,"step"),query=list(project=project)))
}

loadStep  <- function(step,file) {
    content(POST(url=paste0(base,"step/",step,"/load"),body=list(file=upload_file(file))))
}

getLastProcessing <- function(step) {
    content(GET(url=paste0(base,"step/",step,"/processing/last")))
}

# parameter

addParameter  <- function(step,key=NULL,value=NULL) {
    content(POST(url=paste0(base,"step/",step,"/load"),body=list(key=key,value=value)))
}

getParameter <- function(step,key=NULL) {
    content(GET(url=paste0(base,"step/",step),query=list(key=key)))
}

# processing

getProcessing <- function(processing) {
    content(GET(url=paste0(base,"processing/",processing)))
}

getProcessingResult <- function(processing) {
    content(GET(url=paste0(base,"processing/",processing,"/result")))
}

# tutorial helper

writeTempFile <- function(data) {
    path <- tempfile()
    writeLines(data, path)
    return(path)
}
