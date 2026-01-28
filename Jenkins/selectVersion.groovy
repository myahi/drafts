import groovy.json.JsonSlurper
import java.util.Base64

def app = APP

def baseUrl   = System.getenv("EAI_ARTIFACTORY_URL")
def groupPath = "fr.labanquepostale.marches.eai"
def user      = System.getenv("ARTIFACTORY_USER")
def token     = System.getenv("ARTIFACTORY_TOKEN")

// Sécurité basique : si pas de variables, on renvoie vide
if (!baseUrl || !groupPath || !user || !token) {
  return []
}

// Maven layout : <groupPath>/<artifactId>/<version>/...jar
def aql = """
items.find({
  "repo": "${repo}",
  "path": {"\$match":"${groupPath}/${app}/*"},
  "name": {"\$match":"${app}-*.jar"}
}).include("path")
"""

def url = new URL("${baseUrl}/api/search/aql")
def conn = url.openConnection()
conn.setRequestMethod("POST")
conn.setDoOutput(true)

def auth = Base64.encoder.encodeToString("${user}:${token}".getBytes("UTF-8"))
conn.setRequestProperty("Authorization", "Basic ${auth}")
conn.setRequestProperty("Content-Type", "text/plain")

conn.outputStream.withWriter("UTF-8") { it << aql }

def json = new JsonSlurper().parse(conn.inputStream)
def prefix = "${groupPath}/${app}/"

// Extrait la version depuis le path: groupPath/app/<version>
def versions = json.results.collect { r ->
  def p = r.path as String
  if (p != null && p.startsWith(prefix)) {
    return p.substring(prefix.length()).split("/")[0]
  }
  return null
}.findAll { it != null }.unique()

return versions.sort().reverse()
