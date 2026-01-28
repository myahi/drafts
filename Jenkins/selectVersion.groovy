import groovy.json.JsonSlurper
import java.util.Base64
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials

def app = (binding.variables.get('APP') ?: 'eai-camel-rgv').trim()
def baseUrl = (binding.variables.get('EAI_ARTIFACTORY_URL') ?: 'https://dlu.pop.sf.intra.laposte.fr/artifactory').trim()
baseUrl = baseUrl.replaceAll(/\/+$/, '')  // retire / final si présent

def groupPath = "fr/labanquepostale/marches/eai"   // groupId Maven converti en path
def repo = (binding.variables.get('EAI_ARTIFACTORY_REPO') ?: '').trim()

def CRED_ID = "usr_artifactory_eai"   // <-- ton credential Jenkins (username/password)

if (!app) return ["(missing APP)"]
if (!baseUrl) return ["(missing EAI_ARTIFACTORY_URL)"]

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

def repoLine = repo ? "\"repo\": \"${repo}\"," : ""

def aql = """
items.find({
  ${repoLine}
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

int code = conn.responseCode
if (code < 200 || code >= 300) {
  def err = conn.errorStream ? conn.errorStream.getText("UTF-8") : ""
  return ["(Artifactory HTTP ${code}) ${err?.take(180)}"]
}

def json = new JsonSlurper().parse(conn.inputStream)
def prefix = "${groupPath}/${app}/"

def versions = (json?.results ?: []).collect { r ->
  def p = r.path as String
  (p && p.startsWith(prefix)) ? p.substring(prefix.length()).split("/")[0] : null
}.findAll { it }.unique()

if (versions.isEmpty()) return ["(no versions found)"]

// optionnel : enlever les snapshots
versions = versions.findAll { !it.endsWith("-SNAPSHOT") }

// tri simple
return versions.sort().reverse()
