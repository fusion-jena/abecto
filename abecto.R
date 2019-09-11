# This script provides handy functions to use the ABECTO REST service, hidding the raw HTTP requests.

library("httr")
library("jsonlite")

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
    http_status(DELETE(url=paste0(base,"project/",project)))$message
}

listProjects <- function() {
    fromJSON(content(GET(url=paste0(base,"project")),as="text"))
}

getProject <- function(project) {
    fromJSON(content(GET(url=paste0(base,"project/",project)),as="text"))
}

runProject <- function(project,await=FALSE) {
    secondLevelToJSON(fromJSON(content(GET(url=paste0(base,"project/",project,"/run"),query=list(await=await)),as="text")))
}

# knowledgebase

createKnowledgebase  <- function(project,label="") {
    content(POST(url=paste0(base,"knowledgebase"),body=list(project=project,label=label)))$id
}

deleteKnowledgebase  <- function(knowledgebase) {
    http_status(DELETE(url=paste0(base,"knowledgebase/",knowledgebase)))$message
}

getKnowledgebase  <- function(knowledgebase) {
    fromJSON(content(GET(url=paste0(base,"knowledgebase/",knowledgebase)),as="text"))
}

listKnowledgebases <- function(project=NULL) {
    fromJSON(content(GET(url=paste0(base,"knowledgebase"),query=list(project=project)),as="text"))
}

# step

createStep  <- function(class,knowledgebase=NULL,input=NULL,parameters=NULL) {
    body=list(class=class,knowledgebase=knowledgebase,parameters=toJSON(parameters,auto_unbox=TRUE))
    if (!is.null(input)){
        names(input) <- rep("input",times=length(input))
        body <- append (body,input)
    }
    content(POST(url=paste0(base,"step"),body=body))$id
}

getStep <- function(step) {
    prettify(content(GET(url=paste0(base,"step/",step)),as="text"))
}

listSteps <- function(project) {
    secondLevelToJSON(fromJSON(content(GET(url=paste0(base,"step"),query=list(project=project)),as="text")))
}

loadStep  <- function(step,file) {
    prettify(content(POST(url=paste0(base,"step/",step,"/load"),body=list(file=upload_file(file))),as="text"))
}

getLastProcessing <- function(step) {
    content(GET(url=paste0(base,"step/",step,"/processing/last")))$id
}

# parameter

addParameter  <- function(step,key=NULL,value=NULL) {
    prettify(content(POST(url=paste0(base,"step/",step,"/load"),body=list(key=key,value=value)),as="text"))
}

getParameter <- function(step,key=NULL) {
    prettify(content(GET(url=paste0(base,"step/",step),query=list(key=key)),as="text"))
}

# processing

getProcessing <- function(processing) {
    prettify(content(GET(url=paste0(base,"processing/",processing)),as="text"))
}

getProcessingResult <- function(processing) {
    prettify(content(GET(url=paste0(base,"processing/",processing,"/result")),as="text"))
}

# utils

writeTempFile <- function(data) {
    path <- tempfile()
    writeLines(data, path)
    return(path)
}

secondLevelToJSON <- function(df) {
    for (i in 1:ncol(df)) {
        if (is.data.frame(df[,i])) {
            newCol <- c()
            for (j in 1:nrow(df)) {
                newCol[j] <- toJSON(df[j,i],pretty=TRUE)
            }
            df[[i]] <- newCol
        }
    }
    return(df)
}