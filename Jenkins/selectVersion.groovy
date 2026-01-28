import groovy.json.JsonSlurper
import java.net.URLEncoder
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials

def baseUrl = "https://gitlab.pop.sf.intra.laposte.fr"
def CRED_ID = "usr_gitlab_eai"
def searchTerm = "eai-camel-rgv"   // tu peux aussi tester "eai-marches"

def creds = CredentialsProvider.lookupCredentials(
  StandardUsernamePasswordCredentials,
  Jenkins.instance,
  null,
  null
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
  def err = conn.errorStream ? conn.errorStream.getText("UTF-8") : ""
  return [__code: code, __err: err]
}

def q = URLEncoder.encode(searchTerm, "UTF-8")
def results = apiGet("/api/v4/projects?search=${q}&simple=true&per_page=50")

if (results instanceof Map && results.__code) {
  return ["(SEARCH FAIL ${results.__code})"]
}

if (!(results instanceof List) || results.isEmpty()) {
  return ["(no visible projects matching '${searchTerm}' for this token)"]
}

// Affiche ce que le token “voit”
return results.take(20).collect { r ->
  "${r.path_with_namespace}  (id=${r.id})"
} 
