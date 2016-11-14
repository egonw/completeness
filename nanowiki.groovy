sparqlEP = "http://127.0.0.1:9999/blazegraph/sparql" // using a local Blazegraph instance

outputFilename = "/D5.6 - Completeness/foo.html"
completenessReport = report.createReport();

findDatasets = ui.readFile("/D5.6 - Completeness/findDatasets.rq")

datasets = rdf.sparqlRemote(sparqlEP, findDatasets)

toCheck = [
  "physchem" : [
    props : [ "NPO_274", "NPO_1694" ],
  ]
]
propertiesToTest = [
  "NPO_274" : [
    label  : "NPO_274",
  ],
  "NPO_1694" : [
    label  : "NPO_1694",
  ]
]

// iterate over all data sets
for (int i=1; i<=datasets.rowCount; i++) {
  uri = datasets.get(i, "thing")
  title = datasets.get(i, "name")
  println "Dataset: $title ($uri)"
  completenessReport.startSection(title)
  completenessReport.addLink(uri, uri)
  
  findMaterials = ui.readFile("/D5.6 - Completeness/findMaterials.rq")
  findMaterials = findMaterials.replace("\${uri}", uri)
  materials = rdf.sparqlRemote(sparqlEP, findMaterials)
  println " #materials: " + materials.rowCount
  
  for (int j=1; j<=materials.rowCount; j++) {
    materialURI = materials.get(j, "substance")
    nameLookup = ui.readFile("/D5.6 - Completeness/nameLookup.rq")
    nameLookup = nameLookup.replace("\${materialURI}", materialURI)
    nameData = rdf.sparqlRemote(sparqlEP, nameLookup)
    if (nameData.rowCount > 0) {
      completenessReport.startSubSection(nameData.get(1,"label"));
    } else {
      completenessReport.startSubSection(materialURI);
    }

    for (String group : toCheck.keySet()) {
      missingProperties = new ArrayList()
      for (String prop : toCheck[group].props) {
        // completenessReport.addText("Property: ${prop}")
        propertyCheck = ui.readFile("/D5.6 - Completeness/propertyCheck.rq")
        propertyCheck = propertyCheck.replace("\${materialURI}", materialURI)
        propertyCheck = propertyCheck.replace("\${prop}", prop)
        propertyData = rdf.sparqlRemote(sparqlEP, propertyCheck)
        if (propertyData.rowCount > 0) {
          // completenessReport.addText("data for $prop").forceNewLine()
          // OK, data found!
          completenessReport.addTable(propertyData, prop);
        } else {
          missingProperties.add(prop)
        }
      }
      if (missingProperties.size() > 0) {
        errorMessage = "Missing data for: "
        for (String prop : missingProperties) {
          errorMessage += prop + " "
        }
        completenessReport.addText(errorMessage).forceNewLine()
      } else {
        completenessReport.addText("All expected data found!").forceNewLine()
      }
    }
  }
}

if (ui.fileExists(outputFilename)) ui.remove(outputFilename)
output = ui.newFile(outputFilename, report.asHTML(completenessReport) )
