<?php
include_once 'AuditController.php';
include_once 'topHeader.php';
?>

<!DOCTYPE html>
<html>
<head>

<link rel="stylesheet" href="css/application.css">
<link rel="stylesheet" href="css/auditView.css">

<link href="css/jquery.contextMenu.css" rel="stylesheet">
<link rel="stylesheet" href="css/prism.css">

</head>

<body>

<div class="audit-view">

<?php $category = "audit"; include 'menu-central.php'; ?>
<?php $page = 'AuditView'; include 'menu-audit.php'; ?>

<div id="main">
    <div id="bigsidebar">

        <!-- TON FORM EXISTANT = inchangé -->
        <form id="auditform" name="auditform" action="AuditController.php" method="post">
            <!-- TU NE TOUCHES PAS -->
        </form>

    </div>

    <div id="content">
        <table id="auditTable"></table>
    </div>
</div>

</div>

<!-- 🔥 CONFIG PHP → JS -->
<script>
window.AUDIT_CONFIG = {
    flows: <?= json_encode($flowsList, JSON_UNESCAPED_UNICODE) ?>,
    selectedFlows: <?= json_encode($criteres['DISPLAYED_FLOW_TAGS'], JSON_UNESCAPED_UNICODE) ?>,
    pagination: <?= json_encode($pagination) ?>,
    data: <?= json_encode($data) ?>,
    codifiers: <?= json_encode($codifiers) ?>
};
</script>

<!-- LIBS -->
<script src="js/jquery.contextMenu.js"></script>
<script src="js/clipboard.min.js"></script>
<script src="js/prism.js"></script>
<script src="js/vkbeautify.js"></script>

<!-- TON JS -->
<script src="js/auditView.js"></script>

</body>
</html>
