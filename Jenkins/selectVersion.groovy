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
  def err = conn.errorStream ? conn.errorStream.getText("UTF-8") : ""
  return [__code: code, __err: err]
}

def projects = apiGet("/api/v4/projects?membership=true&per_page=20") // <-- sans simple=true
if (projects instanceof Map && projects.__code) return ["(FAIL ${projects.__code}) ${projects.__err?.take(120)}"]
if (!(projects instanceof List) || projects.isEmpty()) return ["(no projects)"]

// Affiche des champs robustes
return projects.take(20).collect { p ->
  def id = p?.id
  def name = p?.name
  def path = p?.path
  def pwn = p?.path_with_namespace
  def web = p?.web_url
  "${id} | ${pwn ?: '(no pwn)'} | ${path ?: '(no path)'} | ${name ?: '(no name)'} | ${web ?: '(no web_url)'}"
}
