import groovy.json.JsonSlurper
import java.net.URLEncoder
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials

def app = "eai-camel-rgv"
def baseUrl = "https://gitlab.pop.sf.intra.laposte.fr"   // sans / final

// IMPORTANT : c’est le path_with_namespace complet (group/subgroup/project)
def projectPath = (binding.variables.get('PROJECT_PATH')
        ?: 'bfi-mar-tpma/eai-marches/eai-camel-rgv').trim()

def EXPECTED_WEB_URL = "https://gitlab.pop.sf.intra.laposte.fr/bfi-mar-tpma/eai-marches/eai-camel-rgv"

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
  return [__error:true, __code:code, __body:err, __path:path]
}

// --- 1) lookup direct
def encodedPath = URLEncoder.encode(projectPath, "UTF-8")
def project = apiGet("/api/v4/projects/${encodedPath}")

def projectId = null
def resolvedPwn = null

if (!(project instanceof Map) || !project.__error) {
  projectId = project?.id
  resolvedPwn = project?.path_with_namespace
} else {
  // 404 => fallback search
  if ((project.__code as int) != 404) {
    return ["(GitLab API error ${project.__code} on ${project.__path})"]
  }
}

// --- 2) fallback search si 404
if (!projectId) {
  def projectName = projectPath.tokenize('/').last()
  def q = URLEncoder.encode(projectName, "UTF-8")
  def results = apiGet("/api/v4/projects?search=${q}&per_page=100&simple=true")

  if (results instanceof Map && results.__error) {
    return ["(GitLab API error ${results.__code} on ${results.__path})"]
  }

  def hit = (results instanceof List) ? results.find { r ->
    def web = (r?.web_url ?: "") as String
    def pwn = (r?.path_with_namespace ?: "") as String
    web == EXPECTED_WEB_URL || pwn.equalsIgnoreCase(projectPath)
  } : null

  if (!hit && results instanceof List) {
    // fallback permissif: match sur la fin de l’URL
    hit = results.find { r ->
      def web = ((r?.web_url ?: "") as String).toLowerCase()
      web.endsWith("/" + projectName.toLowerCase())
    }
  }

  projectId = hit?.id
  resolvedPwn = hit?.path_with_namespace

  if (!projectId) {
    return ["(404: projet introuvable ou token sans accès au projet)"]
  }
}

// --- 3) Liste packages Maven
def versions = [] as Set
int page = 1
int perPage = 100

while (true) {
  def pkgs = apiGet("/api/v4/projects/${projectId}/packages?package_type=maven&per_page=${perPage}&page=${page}")
  if (pkgs instanceof Map && pkgs.__error) {
    return ["(GitLab API error ${pkgs.__code} on ${pkgs.__path})"]
  }
  if (!(pkgs instanceof List) || pkgs.isEmpty()) break

  pkgs.each { p ->
    if ((p?.name as String) == app && p?.version) {
      versions << (p.version as String)
    }
  }

  if (pkgs.size() < perPage) break
  page++
}

if (versions.isEmpty()) {
  return ["(projet OK: ${resolvedPwn ?: projectPath} — aucun package Maven trouvé pour name=${app})"]
}

return versions.toList().sort().reverse()
