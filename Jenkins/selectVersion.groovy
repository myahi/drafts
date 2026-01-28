import groovy.json.JsonSlurper
import java.net.URLEncoder
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials

def baseUrl = "https://gitlab.pop.sf.intra.laposte.fr"
def CRED_ID = "usr_gitlab_eai"      // mets l’ID du credential correspondant à "l'autre user"
def searchTerm = "eai-camel-rgv"

def creds = CredentialsProvider.lookupCredentials(
  StandardUsernamePasswordCredentials,
  Jenkins.instance,
  null,
  null
).find { it.id == CRED_ID }

if (!creds) return ["(credential not found: ${CRED_ID})"]

def token = creds.password?.plainText
if (!token) return ["(empty token)"]

def slurper = new JsonSlurper()

def apiGetRaw = { String path ->
  def url = new URL("${baseUrl}${path}")
  def conn = url.openConnection()

  // On envoie les deux modes (PRIVATE-TOKEN et Bearer)
  conn.setRequestProperty("PRIVATE-TOKEN", token)
  conn.setRequestProperty("Authorization", "Bearer ${token}")

  conn.setRequestProperty("Accept", "application/json")
  conn.connect()

  int code = conn.responseCode
  def body = ""
  try {
    body = (code >= 200 && code < 300) ? conn.inputStream.getText("UTF-8")
                                      : (conn.errorStream ? conn.errorStream.getText("UTF-8") : "")
  } catch (ignored) { }
  return [code: code, body: body]
}

// 1) check auth
def u = apiGetRaw("/api/v4/user")
if (u.code != 200) return ["(USER ${u.code}) ${u.body?.take(180)}"]

// 2) search projects
def q = URLEncoder.encode(searchTerm, "UTF-8")
def r = apiGetRaw("/api/v4/projects?search=${q}&simple=true&per_page=10")
if (r.code != 200) return ["(SEARCH ${r.code}) ${r.body?.take(180)}"]

def results = new JsonSlurper().parseText(r.body)
if (!(results instanceof List) || results.isEmpty()) return ["(search ok, but 0 results)"]

return results.collect { it.path_with_namespace as String }.take(10)
