import groovy.json.JsonSlurper
import java.net.URLEncoder
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials

def app = "eai-camel-rgv"  // ou: (binding.variables.get('APP') ?: '').trim()
def baseUrl = "https://gitlab.pop.sf.intra.laposte.fr"   // <-- pas de / final
def projectPath = (binding.variables.get('PROJECT_PATH')
        ?: 'bfi-mar-tpma/eai-marches/eai-camel-rgv').trim()  // <-- chemin complet

def CRED_ID = "usr_gitlab_eai"

if (!app) return ["(missing APP)"]
if (!baseUrl) return ["(missing GitLab baseUrl)"]
if (!projectPath) return ["(missing PROJECT_PATH)"]

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

def apiGet = { String path ->
  def url = new URL("${baseUrl}${path}")
  def conn = url.openConnection()
  conn.setRequestProperty("PRIVATE-TOKEN", token)
  conn.setRequestProperty("Accept", "application/json")
  conn.connect()
  int code = conn.responseCode
  if (code >= 200 && code < 300) return slurper.parse(conn.inputStream)
  def err = conn.errorStream ? conn.errorStream.getText("UTF-8") : ""
  throw new RuntimeException("GitLab API error ${code} on ${path} :: ${err}")
}

// 1) ID projet
def encodedPath = URLEncoder.encode(projectPath, "UTF-8")
def project = apiGet("/api/v4/projects/${encodedPath}")
def projectId = project?.id
if (!projectId) return ["(project not found: ${projectPath})"]

// 2) Versions depuis Maven packages
def versions = [] as Set
int page = 1
int perPage = 100

while (true) {
  def pkgs = apiGet("/api/v4/projects/${projectId}/packages?package_type=maven&per_page=${perPage}&page=${page}")
  if (!(pkgs instanceof List) || pkgs.isEmpty()) break

  pkgs.each { p ->
    if ((p?.name as String) == app && p?.version) {
      versions << (p.version as String)
    }
  }

  if (pkgs.size() < perPage) break
  page++
}

if (versions.isEmpty()) return ["(no versions found for ${app})"]

return versions.toList().sort().reverse() 
