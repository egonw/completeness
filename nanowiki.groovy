sparqlEP = "http://127.0.0.1:9999/blazegraph/sparql" // using a local Blazegraph instance

outputFilename = "/D5.6 - Completeness/foo.html"
completenessReport = report.createReport();

findDatasets = """
PREFIX void:  <http://rdfs.org/ns/void#>
PREFIX dcterms: <http://purl.org/dc/terms/>

SELECT DISTINCT ?thing ?name WHERE {
  ?thing a void:Dataset ;
    dcterms:title ?name
}
"""

datasets = rdf.sparqlRemote(sparqlEP, findDatasets)

toCheck = [
  "physchem" : [
    props : [ "NPO_274", "NPO_1694" ],
    multiplier : 1.0
  ]
]
propertiesToTest = [
  "NPO_274" : [
    label  : "NPO_274",
    weight : 1.0
  ],
  "NPO_1694" : [
    label  : "NPO_1694",
    weight : 1.0
  ]
]

// iterate over all data sets
for (int i=1; i<=datasets.rowCount; i++) {
  uri = datasets.get(i, "thing")
  title = datasets.get(i, "name")
  println "Dataset: $title ($uri)"
  completenessReport.startSection(title)
  completenessReport.addLink(uri, uri)
  
  findMaterials = """
    PREFIX void:    <http://rdfs.org/ns/void#>
    PREFIX dcterms: <http://purl.org/dc/terms/>
    PREFIX obo:     <http://purl.obolibrary.org/obo/>

    SELECT DISTINCT ?substance WHERE {
      BIND (<${uri}> as ?dataset)
      ?dataset a void:Dataset .
      ?substance a obo:CHEBI_59999 ;
        dcterms:source ?dataset .
    } LIMIT 50
  """
  materials = rdf.sparqlRemote(sparqlEP, findMaterials)
  println " #materials: " + materials.rowCount
  
  for (int j=1; j<=materials.rowCount; j++) {
    materialURI = materials.get(j, "substance")
    nameLookup = """
      prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>

      select distinct ?label where {
        BIND (<${materialURI}> as ?substance)
        ?substance rdfs:label ?label .
      }
    """
    nameData = rdf.sparqlRemote(sparqlEP, nameLookup)
    if (nameData.rowCount > 0) {
      completenessReport.startSubSection(nameData.get(1,"label"));
    } else {
      completenessReport.startSubSection(materialURI);
    }
    
    for (String group : groups) {
      missingProperties = new ArrayList()
      for (String prop : propertiesToTest) {
        // completenessReport.addText("Property: ${prop}")
        propertyCheck = """
          prefix obo: <http://purl.obolibrary.org/obo/>
          prefix bao: <http://www.bioassayontology.org/bao#>
          prefix sso: <http://semanticscience.org/resource/>
          prefix npo: <http://purl.bioontology.org/ontology/npo#>
          prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>
          prefix dc:    <http://purl.org/dc/elements/1.1/>
        
          select distinct ?type ?title ?value ?unit where {
            BIND (<${materialURI}> as ?substance)
            BIND (npo:${prop} as ?propertyType)
            { ?assay a ?propertyType . }
            UNION
            { ?assay a [ rdfs:subClassOf+ ?propertyType ] }
            ?substance a obo:CHEBI_59999 ;
              obo:BFO_0000056 ?mgroup .
            ?mgroup obo:OBI_0000299 ?endpoint .
            ?endpoint sso:has-value ?value ;
                      sso:has-unit ?unit .
            ?assay a bao:BAO_0000015, ?type ;
              bao:BAO_0000209 ?mgroup ;
              dc:title ?title .
            FILTER (?type != bao:BAO_0000015)
          } ORDER BY ASC(?substance)
        """
        propertyData = rdf.sparqlRemote(sparqlEP, propertyCheck)
        if (propertyData.rowCount > 0) {
          // completenessReport.addText("data for $prop").forceNewLine()
          // OK, data found!
          // completenessReport.addTable(propertyData, prop);
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
