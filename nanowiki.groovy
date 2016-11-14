sparqlEP = "http://127.0.0.1:9999/blazegraph/sparql" // using a local Blazegraph instance

outputFilename = "/D5.6 - Completeness/foo.html"
completenessReport = report.createReport();

findDatasets = ui.readFile("/D5.6 - Completeness/findDatasets.rq")

datasets = rdf.sparqlRemote(sparqlEP, findDatasets)

toCheck = [
  "physchem" : [
    props : [ "NPO_1967", "NPO_274", "NPO_1694", "NPO_1697", "NPO_1235", "NPO_1812", "NPO_1302" ],
  ]
]
propertiesToTest = [
  "NPO_1967" : [
    label      : "Aggregation",
  ],
  "NPO_274" : [
    label      : "Shape",
  ],
  "NPO_1694" : [
    label      : "Particle size",
  ],
  "NPO_1697" : [
    label      : "Size distribution",
  ],
  "NPO_1235" : [
    label      : "Surface area",
  ],
  "NPO_1812" : [
    label      : "Surface charge",
  ],
  "NPO_1302" : [
    label      : "Zeta potential",
  ],
]

scores = new ArrayList();
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

	maxScore = 0
	score = 0
    missingProperties = new ArrayList()
    for (String group : toCheck.keySet()) {
      for (String prop : toCheck[group].props) {
        // completenessReport.addText("Property: ${prop}")
        maxScore++
        propertyCheck = ui.readFile("/D5.6 - Completeness/propertyCheck.rq")
        propertyCheck = propertyCheck.replace("\${materialURI}", materialURI)
        propertyCheck = propertyCheck.replace("\${prop}", prop)
        propertyData = rdf.sparqlRemote(sparqlEP, propertyCheck)
        if (propertyData.rowCount > 0) {
          // OK, data found!
          completenessReport.addTable(propertyData, propertiesToTest[prop].label);
          score++
        } else {
          missingProperties.add(prop)
        }
      }
    }
    scorePercentage = Math.round((score/maxScore)*100)
    completenessReport.forceNewLine().addText("Score: " + scorePercentage + " %").forceNewLine()
    scores.add(scorePercentage)
    if (missingProperties.size() > 0) {
      errorMessage = "Missing data for: "
      for (String prop : missingProperties) {
        errorMessage += propertiesToTest[prop].label + " "
      }
      completenessReport.addText(errorMessage).forceNewLine()
    }
  }
}

if (ui.fileExists(outputFilename)) ui.remove(outputFilename)
output = ui.newFile(outputFilename, report.asHTML(completenessReport) )

