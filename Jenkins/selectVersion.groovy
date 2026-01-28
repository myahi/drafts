import groovy.json.JsonSlurper
import java.util.Base64
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials

def app      = binding.variables.get('APP') ?: ''
def baseUrl  = binding.variables.get('EAI_ARTIFACTORY_URL') ?: ''
def repo     = binding.variables.get('EAI_ARTIFACTORY_REPO') ?: ''
def groupPath = "fr/labanquepostale/marches/eai"   // <= en / (pas en .)

def CRED_ID = "artifactory-creds-id"              // <= TON ID DE CREDENTIAL

if (!app || !baseUrl || !repo) return ["(missing params)"]

def creds = CredentialsProvider.lookupCredentials(
  StandardUsernamePasswordCredentials,
  Jenkins.instance,
  null,
  null
).find { it.id == CRED_ID }

if (!creds) return ["(credential not found: ${CRED_ID})"]

def user = creds.username
def token = creds.password?.plainText
if (!user || !token) return ["(empty credential)"]

def aql = """
items.find({
  "repo": "${repo}",
  "path": {"\\\$match":"${groupPath}/${app}/*"},
  "name": {"\\\$match":"${app}-*.jar"}
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

def versions = (json?.results ?: []).collect { r ->
  def p = r.path as String
  if (p && p.startsWith(prefix)) p.substring(prefix.length()).split("/")[0] else null
}.findAll { it }.unique()

// tri simple (alphabétique). Si tu veux semver, je te le fais.
return versions.sort().reverse()
