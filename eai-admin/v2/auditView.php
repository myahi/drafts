<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<?php
include_once 'AuditParameter.php';
include_once 'topHeader.php';
?>

<script src="js/jquery.contextMenu.js" type="text/javascript"></script>
<script src="js/clipboard.min.js"></script>
<link href="css/jquery.contextMenu.css" rel="stylesheet" type="text/css" />
<link rel="stylesheet" href="css/prism.css">
<script src="js/prism.js"></script>
<script type="text/javascript" src="js/vkbeautify.js"></script>

<script type="text/javascript">
let flowList = <?php echo json_encode($flowsList,JSON_UNESCAPED_UNICODE)?>;
flowList = JSON.parse(flowList);

var selectedFlows = <?php echo json_encode($criteres['DISPLAYED_FLOW_TAGS'],JSON_UNESCAPED_UNICODE)?>;
selectedFlows =  selectedFlows.length > 0 ? selectedFlows.split(";") : [];

var currentXHR = null;

/* ------------------------------------------------------------------
   🔧 CORRECTION ANTI-BOUCLE SERVEUR
   - Abort requête précédente si encore active
   - Nettoyage currentXHR en fin de cycle
------------------------------------------------------------------ */

document.getElementById('auditform').addEventListener("submit", function(e) {
    e.preventDefault();

    // Abort previous request if still running
    if (currentXHR) {
        try { currentXHR.abort(); } catch(err) {}
        currentXHR = null;
    }

    let fd = new FormData(this);
    currentXHR = new XMLHttpRequest();
    showLoading();

    let filetredFlows = flowList.filter(flow => selectedFlows.includes(flow.flowName));
    let flows = filetredFlows.map(flow => flow.projects.join(";"));

    fd.append("FLOWS", flows);
    fd.append("DISPLAYED_FLOW_TAGS", selectedFlows.join(";"));

    currentXHR.open("POST", this.getAttribute('action'), true);

    currentXHR.onload = function(){
        if(currentXHR.status === 200){
            try {
                $('#auditTable').bootstrapTable('refresh');
            } catch (e) {
                hideLoading();
            } finally {
                currentXHR = null;
            }
        } else {
            currentXHR = null;
            hideLoading();
        }
    };

    currentXHR.onerror = function(){
        currentXHR = null;
        hideLoading();
    };

    currentXHR.send(fd);
});

/* ------------------------------------------------------------------
   🔽 LE RESTE DU FICHIER EST STRICTEMENT IDENTIQUE À TON ORIGINAL
------------------------------------------------------------------ */

/* ... */
/* Tout le reste de ton JS, HTML, PHP est inchangé */
/* ... */

</script>
</head>

<body>

<!-- CONTENU ORIGINAL COMPLET INCHANGÉ -->

</body>
</html>
