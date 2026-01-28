import groovy.json.JsonSlurper
import java.net.URLEncoder
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials

def baseUrl = "https://gitlab.pop.sf.intra.laposte.fr"
def projectPath = "bfi-mar-tpma/eai-marches/eai-camel-rgv"
def app = "eai-camel-rgv"
def CRED_ID = "usr_gitlab_eai"

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
  if (code >= 200 && code < 300) {
    return slurper.parse(conn.inputStream)
  }
  def err = conn.errorStream ? conn.errorStream.getText("UTF-8") : ""
  return [__code: code, __err: err]
}

// 1) Test auth simple
def ver = apiGet("/api/v4/version")
if (ver.__code) {
  return ["(AUTH FAIL ${ver.__code})"]
}

// 2) Récupération projet
def encodedPath = URLEncoder.encode(projectPath, "UTF-8")
def project = apiGet("/api/v4/projects/${encodedPath}")
if (project.__code) {
  return ["(PROJECT FAIL ${project.__code})"]
}
def projectId = project.id

// 3) Liste packages Maven
def pkgs = apiGet("/api/v4/projects/${projectId}/packages?package_type=maven&per_page=5")
if (pkgs.__code) {
  return ["(PACKAGES FAIL ${pkgs.__code})"]
}

if (!(pkgs instanceof List) || pkgs.isEmpty()) {
  return ["(OK auth/project — no Maven packages)"]
}

// 4) Versions
def versions = pkgs.findAll { it.name == app && it.version }
                  .collect { it.version }
                  .unique()
                  .sort()
                  .reverse()

return versions ?: ["(OK auth/project — no versions for ${app})"]
