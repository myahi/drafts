import groovy.json.JsonSlurper
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials

def baseUrl = "https://gitlab.pop.sf.intra.laposte.fr"
def CRED_ID = "usr_gitlab_eai"

def creds = CredentialsProvider.lookupCredentials(
  StandardUsernamePasswordCredentials, Jenkins.instance, null, null
).find { it.id == CRED_ID }

if (!creds) return ["(credential not found)"]
def token = creds.password?.plainText
if (!token) return ["(empty token)"]

def slurper = new JsonSlurper()

def apiGet = { String path ->
  def url = new URL("${baseUrl}${path}")
  def conn = url.openConnection()
  conn.setRequestProperty("PRIVATE-TOKEN", token)
  conn.setRequestProperty("Authorization", "Bearer ${token}")
  conn.setRequestProperty("Accept", "application/json")
  conn.connect()
  int code = conn.responseCode
  if (code >= 200 && code < 300) return slurper.parse(conn.inputStream)
  return [__code: code]
}

// projets dont le user est membre
def projects = apiGet("/api/v4/projects?membership=true&simple=true&per_page=20")
if (projects.__code) return ["(FAIL ${projects.__code})"]

if (!(projects instanceof List) || projects.isEmpty()) return ["(no membership projects for this token)"]

return projects.collect { it.path_with_namespace as String }
