sparqlEP = "http://127.0.0.1:9999/blazegraph/sparql" // using a local Blazegraph instance

outputFilename = "/D5.6 - Completeness/foo.html"
scoresOutputFilename = "/D5.6 - Completeness/foo.csv"
completenessReport = report.createReport();

findDatasets = ui.readFile("/D5.6 - Completeness/findDatasets.rq")

datasets = rdf.sparqlRemote(sparqlEP, findDatasets)

toCheck = [
  "physchem" : [
    props : [ "NPO_1967", "NPO_274", "NPO_1694", "NPO_1697", "NPO_1235",
              "NPO_1812", "NPO_1302", "PATO_0001536", "C53414" ],
  ]
]
propertiesToTest = [
  "C53414" : [
    label      : "Composition",
    score      : 1.0,
    iri        : "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C53414",
    query      : "compositionCheck"
  ],
  "NPO_1967" : [
    label      : "Aggregation",
    score      : 1.0,
    iri        : "http://purl.bioontology.org/ontology/npo#NPO_1967",
    query      : "propertyCheck"
  ],
  "NPO_274" : [
    label      : "Shape",
    score      : 0.5,              // not at all reported, half bonus
    iri        : "http://purl.bioontology.org/ontology/npo#NPO_274",
    query      : "propertyCheck"
  ],
  "NPO_1694" : [
    label      : "Particle size",
    score      : 0.5,              // either particle size or distribution
    iri        : "http://purl.bioontology.org/ontology/npo#NPO_1694",
    query      : "propertyCheck"
  ],
  "NPO_1697" : [
    label      : "Size distribution",
    score      : 0.5,              // either particle size or distribution
    iri        : "http://purl.bioontology.org/ontology/npo#NPO_1697",
    query      : "propertyCheck"
  ],
  "NPO_1235" : [
    label      : "Surface area",
    score      : 0.0,              // not at all reported
    iri        : "http://purl.bioontology.org/ontology/npo#NPO_1235",
    query      : "propertyCheck"
  ],
  "NPO_1812" : [
    label      : "Surface charge",
    score      : 1.0,
    iri        : "http://purl.bioontology.org/ontology/npo#NPO_1812",
    query      : "propertyCheck"
  ],
  "NPO_1302" : [
    label      : "Zeta potential",
    score      : 1.0,
    iri        : "http://purl.bioontology.org/ontology/npo#NPO_1302",
    query      : "propertyCheck"
  ],
  "PATO_0001536" : [
    label      : "Solubility",
    score      : 1.0,
    iri        : "http://purl.obolibrary.org/obo/PATO_0001536",
    query      : "propertyCheck"
  ],
]

scoresList = "score\n"
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
      completenessReport.addLink(
        materialURI.replace("localhost/ambit2","data.enanomapper.net")+"/study",
        "Open in eNanoMapper"
      ).forceNewLine()
    } else {
      completenessReport.startSubSection(materialURI);
    }

	maxScore = 0
	score = 0
    missingProperties = new ArrayList()
    for (String group : toCheck.keySet()) {
      for (String prop : toCheck[group].props) {
        // completenessReport.addText("Property: ${prop}")
        maxScore += propertiesToTest[prop].score
        propertyCheck = ui.readFile("/D5.6 - Completeness/${propertiesToTest[prop].query}.rq")
        propertyCheck = propertyCheck.replace("\${materialURI}", materialURI)
        propertyCheck = propertyCheck.replace("\${prop}", propertiesToTest[prop].iri)
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
    scoresList += "" + scorePercentage + "\n"
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

if (ui.fileExists(scoresOutputFilename)) ui.remove(scoresOutputFilename)
scoresOutput = ui.newFile(scoresOutputFilename, scoresList )
