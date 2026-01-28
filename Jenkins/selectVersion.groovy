import groovy.json.JsonSlurper
import java.util.Base64
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials

def app      = "eai-camel-rgv"
def baseUrl  = binding.variables.get('EAI_ARTIFACTORY_URL') ?: ''
def groupPath = "fr/labanquepostale/marches/eai"

def CRED_ID = "usr_gitlab_eai"              // <= TON ID DE CREDENTIAL

if (!app) return ["(missing APP param)"]

if (!baseUrl) return ["(missing baseUrl param)"]

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
