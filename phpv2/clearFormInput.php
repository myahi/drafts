function clearFormInput(){
  // 1) Nettoyage champs (au plus proche de ton existant)
  document.getElementById("details").value = "";

  const form = document.getElementById("auditform");
  form.reset();

  // Forcer à vide (sinon form.reset() peut remettre les valeurs initiales PHP)
  const codeEl = document.getElementById("code");
  if (codeEl) codeEl.value = "";

  const codifierEl = document.getElementById("codifier");
  if (codifierEl) codifierEl.value = "";

  const auditDataEl = document.getElementById("auditData");
  if (auditDataEl) auditDataEl.value = "";

  const tagInputEl = document.getElementById("tagInput");
  if (tagInputEl) tagInputEl.value = "";

  // 2) Vider la liste de tags Flux (UI + état)
  let fluxList = document.getElementById("flux-list");
  if (fluxList) {
    // plus robuste que remove span par span
    fluxList.innerHTML = "";
  }
  selectedFlows = [];

  // 3) Remise à zéro des selects
  const projectSel = document.getElementById("projectName");
  if (projectSel) projectSel.selectedIndex = 0;

  const statusSel = document.getElementById("STATUS");
  if (statusSel) statusSel.selectedIndex = 0;

  // 4) DateRangePicker : reset propre + mettre J 00:00 -> 23:59
  const $dr = $('input[name="daterange"]');

  // IMPORTANT: éviter empilement d'instances/handlers
  $dr.off('apply.daterangepicker');
  if ($dr.data('daterangepicker')) {
    $dr.data('daterangepicker').remove();
    $dr.removeData('daterangepicker');
  }

  // Ré-instancie avec tes options via ta fonction existante (qui doit maintenant être "clean")
  // Si tu as déjà appliqué le fix dans initDateRangePicker (off/remove), tu peux juste appeler initDateRangePicker(true)
  // Ici, on le fait explicitement: on remet le champ sur "Aujourd'hui 00:00 - 23:59" juste après.
  initDateRangePicker(true);

  // Forcer la valeur à aujourd'hui 00:00 - 23:59 (ce que tu veux exactement)
  const start = moment().startOf('day');
  const end = moment().endOf('day');
  $dr.val(start.format('DD-MM-YYYY HH:mm') + " - " + end.format('DD-MM-YYYY HH:mm'));
}
