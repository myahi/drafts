<?php
// AuditController.php
include_once 'AuditDAO.php';
include_once 'AuditKeywordDAO.php';
include_once 'connexion.php';

[span_0](start_span)// Sécurité héritée de AuditLoader[span_0](end_span)
$_GET['screenId'] = $GLOBALS['SCREEN_AUDIT_PREFIX'];
include 'verification.php';

if(!ini_get('date.timezone')) { date_default_timezone_set('Europe/Paris'); [span_1](start_span)}
if (session_status() == PHP_SESSION_NONE) { session_start(); }[span_1](end_span)

// --- PARTIE A : Capture (Ancien AuditLoaderForSearch) ---
if ($_SERVER['REQUEST_METHOD'] === 'POST' && !isset($_GET['ajax'])) {
    if (isset($_POST['init'])) {
        [span_2](start_span)$_SESSION['recherche_code'] = '';[span_2](end_span)
        [span_3](start_span)$_SESSION['recherche_projet'] = '';[span_3](end_span)
        $_SESSION['recherche_displayed_flow_tags'] = '';
        [span_4](start_span)$_SESSION['recherche_timestamp'] = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d"), date("Y")));[span_4](end_span)
        [span_5](start_span)$_SESSION['recherche_timestamp2'] = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d")+1, date("Y")));[span_5](end_span)
        $_SESSION['recherche_useArchiveAudit'] = false;
    } else {
        $_SESSION['recherche_code'] = $_POST['codeAudit'] ?? [span_6](start_span)'';[span_6](end_span)
        $_SESSION['recherche_projet'] = $_POST['projet'] ?? [span_7](start_span)'';[span_7](end_span)
        $_SESSION['recherche_displayed_flow_tags'] = $_POST['displayed_flow_tags'] ?? '';
        $_SESSION['recherche_timestamp'] = $_POST['dateDebut'] ?? [span_8](start_span)'';[span_8](end_span)
        $_SESSION['recherche_timestamp2'] = $_POST['dateFin'] ?? [span_9](start_span)'';[span_9](end_span)
        [span_10](start_span)$_SESSION['recherche_useArchiveAudit'] = isset($_POST['useArchiveAudit']);[span_10](end_span)
    }
    [span_11](start_span)// Crucial : On force l'écriture avant de passer à la suite[span_11](end_span)
    [span_12](start_span)session_write_close();[span_12](end_span)
    [span_13](start_span)session_start();[span_13](end_span)
}

// --- PARTIE B : Données (Ancien AuditLoader) ---
[span_14](start_span)$auditDAO = new AuditDAO();[span_14](end_span)
$auditKeywordDAO = new AuditKeywordDAO();

$criteres = array (
    [span_15](start_span)'CODE' => $_SESSION['recherche_code'] ?? '',[span_15](end_span)
    'PROJECT' => $_SESSION['recherche_projet'] ?? [span_16](start_span)'',[span_16](end_span)
    // Correction ici : Nom de clé avec underscores pour correspondre à ton JS
    'DISPLAYED_FLOW_TAGS' => $_SESSION['recherche_displayed_flow_tags'] ?? '',
    'TIMESTAMP' => $_SESSION['recherche_timestamp'] ?? [span_17](start_span)'',[span_17](end_span)
    'TIMESTAMP2' => $_SESSION['recherche_timestamp2'] ?? [span_18](start_span)'',[span_18](end_span)
    [span_19](start_span)'useArchiveAudit' => $_SESSION['recherche_useArchiveAudit'] ?? false[span_19](end_span)
);

$pagination = isset($_REQUEST['offset']) ? (int)[span_20](start_span)$_REQUEST['offset'] : 0;[span_20](end_span)
$intervalle = isset($_REQUEST['limit']) ? (int)[span_21](start_span)$_REQUEST['limit'] : 50;[span_21](end_span)

[span_22](start_span)$auditInformations = $auditDAO->getAuditData($pagination, $intervalle, "", $criteres, "desc", $criteres['useArchiveAudit']);[span_22](end_span)
[span_23](start_span)$rows = $auditInformations->auditLines;[span_23](end_span)

// Données pour les selects de l'AuditView
$resultProject = $auditDAO->getProjetNames();
$flowsList = $auditKeywordDAO->getAuditFlowsList();
$data = $auditKeywordDAO->getAuditDescriptionsList();
$codifiers = $auditKeywordDAO->getAuditCodifiersList();

// --- PARTIE C : Réponse JSON pour le tableau AJAX ---
if (isset($_GET['ajax'])) {
    header('Content-Type: application/json');
    echo json_encode([
        "total" => $auditInformations->totalCount,
        "rows"  => $rows
    ]);
    exit;
}
